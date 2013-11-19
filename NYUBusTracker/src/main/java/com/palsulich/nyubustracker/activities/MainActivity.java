package com.palsulich.nyubustracker.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.palsulich.nyubustracker.R;
import com.palsulich.nyubustracker.helpers.BusManager;
import com.palsulich.nyubustracker.helpers.FileGrabber;
import com.palsulich.nyubustracker.models.Bus;
import com.palsulich.nyubustracker.models.Route;
import com.palsulich.nyubustracker.models.Stop;
import com.palsulich.nyubustracker.models.Time;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {


    Stop startStop;     // Stop object to keep track of the start location of the desired route.
    Stop endStop;       // Keep track of the desired end location.
    ArrayList<Route> routesBetweenToAndFrom;        // List of all routes between start and end.
    ArrayList<Time> timesBetweenStartAndEnd;        // List of all times between start and end.

    Timer myTimer;  // Timer used to refresh the "time until next bus" every minute, on the minute.

    private GoogleMap mMap;     // Map to display all stops, segments, and buses.

    private static final LatLng BROADWAY = new LatLng(40.7291465, -73.9937559);


    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                // The Map is verified. It is now safe to manipulate the map.

            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        renewTimer();       // Creates and starts the timer to refresh time until next bus.

        setUpMapIfNeeded(); // Instantiates mMap, if it needs to be.

        // Default location of map is centered at 715 Broadway with zoom 15.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BROADWAY, 15));

        // List just for development use to display all
        //final ListView listView = (ListView) findViewById(R.id.mainActivityList);

        // Singleton BusManager to keep track of all stops, routes, etc.
        final BusManager sharedManager = BusManager.getBusManager(getApplicationContext());

        // mFileGrabber helps to manage cached files/pull new files from the network.
        FileGrabber mFileGrabber = new FileGrabber(getCacheDir());

        // Only parse stops, routes, buses, times, and segments if we don't have them. Could be more robust.
        if (!sharedManager.hasStops() && !sharedManager.hasRoutes()) {
            try {
                // The Class being created from the parsing *does* the parsing.
                // mFileGrabber.get*JSON() returns a JSONObject.
                Stop.parseJSON(mFileGrabber.getStopJSON());
                Route.parseJSON(mFileGrabber.getRouteJSON());
                BusManager.parseTimes(mFileGrabber.getVersionJSON(), mFileGrabber);
                Bus.parseJSON(mFileGrabber.getVehicleJSON());
                BusManager.parseSegments(mFileGrabber.getSegmentsJSON());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        setStartStop(mFileGrabber.getStartStopFile());
        setEndStop(mFileGrabber.getEndStopFile());

        for (Stop s : sharedManager.getStops()){
            mMap.addMarker(new MarkerOptions()      // Adds a balloon for every stop to the map.
                    .position(s.getLocation())
                    .title(s.getName()));
        }

        for (Route r : sharedManager.getRoutes()){
            PolylineOptions p = r.getSegment();     // Adds the segments of every Route to the map.
            if (p != null) mMap.addPolyline(p);
            else Log.v("MapDebugging", "Segment was null for " + r.getID());
        }
    }

    /*
    renewTimer() creates a new timer that calls setNextBusTime() every minute on the minute.
     */
    private void renewTimer() {
        Calendar rightNow = Calendar.getInstance();

        if (myTimer != null) myTimer.cancel();

        myTimer = new Timer();
        myTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setNextBusTime();
                    }
                });
            }
        }, (60 - rightNow.get(Calendar.SECOND)) * 1000, 60000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cacheToAndStartStop();      // Remember user's preferences across lifetimes.
        myTimer.cancel();           // Don't need a timer anymore -- must be recreated onResume.
    }

    @Override
    public void onPause() {
        super.onPause();
        cacheToAndStartStop();
        myTimer.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();
        renewTimer();
        setUpMapIfNeeded();
    }

    public void cacheToAndStartStop() {
        FileGrabber mFileGrabber = new FileGrabber(getCacheDir());
        mFileGrabber.setEndStop(endStop.getName());         // Creates or updates cache file.
        mFileGrabber.setStartStop(startStop.getName());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void setEndStop(String stopName) {
        if (stopName.equals("")) stopName = "80 Lafayette Street";      // Default end stop.
        Stop tempStop = BusManager.getBusManager().getStopByName(stopName);
        if (tempStop != null) {     // Make sure we actually have a stop!
            endStop = tempStop;
            ((Button) findViewById(R.id.to_button)).setText("End: " + stopName);
            if (startStop != null) setNextBusTime();    // Don't set the next bus if we don't have a valid route.
        }
    }

    private void setStartStop(String stopName) {
        if (stopName.equals("")) stopName = "715 Broadway at Washington Square";    // Default start stop.
        if (endStop != null && endStop.getName().equals(stopName)) {    // We have an end stop and its name is the same as stopName.
            // Swap the start and end stops.
            Stop temp = startStop;
            startStop = endStop;
            ((Button) findViewById(R.id.from_button)).setText("Start: " + startStop.getName());
            endStop = temp;
            ((Button) findViewById(R.id.to_button)).setText("End: " + endStop.getName());
            setNextBusTime();
        } else {
            // We have a new start. So, we must ensure the end is actually connected.
            Stop tempStop = BusManager.getBusManager().getStopByName(stopName);
            if (tempStop != null){      // Don't set Start to an invalid stop. Should never happen.
                startStop = tempStop;
                ((Button) findViewById(R.id.from_button)).setText("Start: " + stopName);
                if (endStop != null){
                    // Loop through all connected Routes.
                    for (Route r : startStop.getRoutes()){
                        // If the current endStop is connected, we don't have to change endStop.
                        if (r.hasStop(endStop.getName())){
                            setNextBusTime();
                            return;
                        }
                    }
                    // If we did not return above, the current endStop is not connected to the new
                    // startStop. So, by default pick the first connected stop.
                    endStop = startStop.getRoutes().get(0).getStops().get(0);
                }

            }
        }
    }

    private String getTimeOfWeek() {
        Calendar rightNow = Calendar.getInstance();
        String dayOfWeek = rightNow.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
        String timeOfWeek = "Weekday";
        if (dayOfWeek.equals("Saturday") || dayOfWeek.equals("Sunday")) timeOfWeek = "Weekend";
        else if (dayOfWeek.equals("Friday")) timeOfWeek = "Friday";

        return timeOfWeek;
    }

    private void setNextBusTime() {
        Calendar rightNow = Calendar.getInstance();
        ArrayList<Route> fromRoutes = startStop.getRoutes();
        ArrayList<Route> routes = new ArrayList<Route>();
        for (Route r : fromRoutes) {
            if (r.hasStop(endStop.getName())) {
                Log.v("Route Debugging", "Adding a route!");
                routes.add(r);
            }
        }
        if (routes.size() != 0) {
            routesBetweenToAndFrom = routes;
            timesBetweenStartAndEnd = new ArrayList<Time>();
            for (int j = 0; j < routes.size(); j++) {
                String timeOfWeek = getTimeOfWeek();
                // Get the Times at this stop for this route.
                List<Time> times = Arrays.asList(startStop.getTimes()
                        .get(timeOfWeek)
                        .get(routes.get(j).getLongName()));
                for (Time t : times){
                    t.setRoute(routes.get(j).getLongName());
                }
                timesBetweenStartAndEnd.addAll(times);
            }
            Time currentTime = new Time(rightNow.get(rightNow.HOUR_OF_DAY), rightNow.get(rightNow.MINUTE));
            Time nextBusTime = timesBetweenStartAndEnd.get(0);
            for (Time tempTime : timesBetweenStartAndEnd) {
                if (tempTime.isAfter(currentTime)) {
                    if (nextBusTime.isAfter(currentTime) && tempTime.isBefore(nextBusTime)) {
                        nextBusTime = tempTime;
                    } else if (nextBusTime.isBefore(currentTime) && tempTime.isAfter(nextBusTime)) {
                        nextBusTime = tempTime;
                    }
                }
            }
            String timeOfNextBus = nextBusTime.toString();
            String timeUntilNextBus = currentTime.getTimeAsStringUntil(nextBusTime);
            ((TextView) findViewById(R.id.times_button)).setText(timeOfNextBus + nextBusTime.getViaRoute());
            ((TextView) findViewById(R.id.next_bus)).setText(timeUntilNextBus);
        } else
        {
            Context context = getApplicationContext();
            CharSequence text = "That stop is unavailable!";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }

    }

    public void createToDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        final String[] connectedStops = BusManager.getBusManager().getConnectedStops(startStop);
        builder.setItems(connectedStops, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                setEndStop(connectedStops[which]);
            }
        });
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void createFromDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        final String[] stops = BusManager.getBusManager().getStopsAsArray();
        builder.setItems(stops, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                setStartStop(stops[which]);
            }
        });
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void createTimesDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        final Time[] times = timesBetweenStartAndEnd.toArray(new Time[1]);
        final String[] timesAsString = new String[times.length];
        for (int i = 0; i < times.length; i++) {
            timesAsString[i] = times[i].toString() + times[i].getViaRoute();
        }
        builder.setItems(timesAsString, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Nothing to do, ish.
            }
        });
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

}
