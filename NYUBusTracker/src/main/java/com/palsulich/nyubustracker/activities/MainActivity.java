package com.palsulich.nyubustracker.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {


    Stop startStop;     // Stop object to keep track of the start location of the desired route.
    Stop endStop;       // Keep track of the desired end location.
    ArrayList<Route> routesBetweenToAndFrom;        // List of all routes between start and end.
    ArrayList<Time> timesBetweenStartAndEnd;        // List of all times between start and end.
    HashMap<String, Boolean> clickableMapMarkers;   // Hash of all markers which are clickable (so we don't zoom in on buses).
    ArrayList<Marker> busesOnMap = new ArrayList<Marker>();

    // mFileGrabber helps to manage cached files/pull new files from the network.
    FileGrabber mFileGrabber;

    Timer timeUntilTimer;  // Timer used to refresh the "time until next bus" every minute, on the minute.
    Timer busRefreshTimer; // Timer used to refresh the bus locations every few seconds.

    private GoogleMap mMap;     // Map to display all stops, segments, and buses.

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            MapFragment mFrag = ((MapFragment) getFragmentManager().findFragmentById(R.id.map));
            if (mFrag != null) mMap = mFrag.getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                // The Map is verified. It is now safe to manipulate the map.
                mMap.getUiSettings().setRotateGesturesEnabled(false);

            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFileGrabber = new FileGrabber(getCacheDir());

        renewTimeUntilTimer();       // Creates and starts the timer to refresh time until next bus.

        setUpMapIfNeeded(); // Instantiates mMap, if it needs to be.

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return !clickableMapMarkers.get(marker.getId());    // Return true to consume the event.
            }
        });

        // List just for development use to display all
        //final ListView listView = (ListView) findViewById(R.id.mainActivityList);

        // Singleton BusManager to keep track of all stops, routes, etc.
        final BusManager sharedManager = BusManager.getBusManager();

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

        updateMapWithNewStartOrEnd();

        renewBusRefreshTimer();
    }

    /*
    renewTimeUntilTimer() creates a new timer that calls setNextBusTime() every minute on the minute.
     */
    private void renewTimeUntilTimer() {
        Calendar rightNow = Calendar.getInstance();

        if (timeUntilTimer != null) timeUntilTimer.cancel();

        timeUntilTimer = new Timer();
        timeUntilTimer.scheduleAtFixedRate(new TimerTask() {
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

    private void renewBusRefreshTimer(){
        if (busRefreshTimer != null) busRefreshTimer.cancel();

        updateMapWithNewBusLocations();

        busRefreshTimer = new Timer();
        busRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            Bus.parseJSON(mFileGrabber.getVehicleJSON());
                            updateMapWithNewBusLocations();
                        } catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                });
            }
        }, 10000L, 10000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cacheToAndStartStop();      // Remember user's preferences across lifetimes.
        timeUntilTimer.cancel();           // Don't need a timer anymore -- must be recreated onResume.
        busRefreshTimer.cancel();
    }

    @Override
    public void onPause() {
        super.onPause();
        cacheToAndStartStop();
        timeUntilTimer.cancel();
        busRefreshTimer.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();
        renewTimeUntilTimer();
        renewBusRefreshTimer();
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

    public static Bitmap rotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void updateMapWithNewBusLocations(){
        BusManager sharedManager = BusManager.getBusManager();
        for (Marker m : busesOnMap){
            m.remove();
        }
        busesOnMap = new ArrayList<Marker>();
        for (Route r: routesBetweenToAndFrom){
            for (Bus b : sharedManager.getBuses()){
                Log.v("BusLocations", "bus id: " + b.getID() + ", bus route: " + b.getRoute() + " vs route: " + r.getID());
                if (b.getRoute().equals(r.getID())){
                    Marker mMarker = mMap.addMarker(new MarkerOptions()
                            .position(b.getLocation())
                            .icon(BitmapDescriptorFactory
                                    .fromBitmap(rotateBitmap(
                                            BitmapFactory.decodeResource(
                                                    this.getResources(),
                                                    R.drawable.ic_bus_arrow),
                                            b.getHeading())))
                            .anchor(0.5f, 0.5f));
                    clickableMapMarkers.put(mMarker.getId(), false);
                    busesOnMap.add(mMarker);
                }
            }
        }
    }

    private void updateMapWithNewStartOrEnd(){
        BusManager sharedManager = BusManager.getBusManager();
        setUpMapIfNeeded();
        mMap.clear();
        clickableMapMarkers = new HashMap<String, Boolean>();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Route r : routesBetweenToAndFrom){
            for (Stop s : r.getStops()){
                Marker mMarker = mMap.addMarker(new MarkerOptions()      // Adds a balloon for every stop to the map.
                        .position(s.getLocation())
                        .title(s.getName())
                        .anchor(0.5f, 0.5f)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_stop)));
                clickableMapMarkers.put(mMarker.getId(), true);
            }
            updateMapWithNewBusLocations();
            // Adds the segments of every Route to the map.
            for (PolylineOptions p : r.getSegments()){
                if (p != null){
                    for (LatLng loc : p.getPoints()){
                        builder.include(loc);
                    }
                    mMap.addPolyline(p);
                }
                else Log.v("MapDebugging", "Segment was null for " + r.getID());
            }
        }

        LatLngBounds bounds = builder.build();
        try{
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 30));
        } catch(IllegalStateException e) {
            e.printStackTrace();
            mMap.moveCamera(
                    CameraUpdateFactory
                            .newLatLngBounds(
                                    bounds,
                                    this.getResources().getDisplayMetrics().widthPixels,
                                    this.getResources().getDisplayMetrics().heightPixels,
                                    200));
        }

    }

    private void setEndStop(String stopName) {
        if (stopName.equals("")) stopName = "80 Lafayette Street";      // Default end stop.
        Stop tempStop = BusManager.getBusManager().getStopByName(stopName);
        if (tempStop != null) {     // Make sure we actually have a stop!
            endStop = tempStop;
            ((Button) findViewById(R.id.to_button)).setText("End: " + stopName);
            if (startStop != null){
                setNextBusTime();    // Don't set the next bus if we don't have a valid route.
                updateMapWithNewStartOrEnd();
            }
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
            updateMapWithNewStartOrEnd();
        } else {
            Log.v("Debugging", "setStartStop not swapping");
            // We have a new start. So, we must ensure the end is actually connected.
            Stop tempStop = BusManager.getBusManager().getStopByName(stopName);
            if (tempStop != null){      // Don't set Start to an invalid stop. Should never happen.
                startStop = tempStop;
                Log.v("Debugging", "New start stop: " + startStop.getName());
                ((Button) findViewById(R.id.from_button)).setText("Start: " + stopName);
                if (endStop != null){
                    // Loop through all connected Routes.
                    for (Route r : startStop.getRoutes()){
                        // If the current endStop is connected, we don't have to change endStop.
                        if (r.hasStop(endStop.getName())){
                            Log.v("Debugging", "Found a connected end stop: " + endStop.getName() + " through " + r.getLongName());
                            setNextBusTime();
                            updateMapWithNewStartOrEnd();
                            return;
                        }
                    }
                    BusManager sharedManager = BusManager.getBusManager();
                    ArrayList<Stop> connectedStops = startStop.getRoutes().get(0).getStops();
                    Log.v("Debugging", "setStartStop picking default endStop: " + connectedStops.get(connectedStops.indexOf(startStop) + 1).getName());
                    // If we did not return above, the current endStop is not connected to the new
                    // startStop. So, by default pick the first connected stop.
                    setEndStop(connectedStops.get(connectedStops.indexOf(startStop) + 1).getName());
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
        if (timeUntilTimer != null) timeUntilTimer.cancel();
        if (busRefreshTimer != null) busRefreshTimer.cancel();
        Calendar rightNow = Calendar.getInstance();
        ArrayList<Route> fromRoutes = startStop.getRoutes();
        ArrayList<Route> routes = new ArrayList<Route>();
        for (Route r : fromRoutes) {
            if (r.hasStop(endStop.getName()) && endStop.getTimes().get(getTimeOfWeek()).get(r.getLongName()) != null) {
                Log.v("Route Debugging", "Adding a route between " + startStop.getName() + " and " + endStop.getName() + ": " + r.getLongName());
                routes.add(r);
            }
        }
        if (routes.size() > 0) {
            ArrayList<Time> tempTimesBetweenStartAndEnd = new ArrayList<Time>();
            for (Route r : routes) {
                String timeOfWeek = getTimeOfWeek();
                // Get the Times at this stop for this route.
                ArrayList<Time> times = new ArrayList<Time>();
                if (startStop.getTimes().get(timeOfWeek).get(r.getLongName()) == null) Log.v("Debugging", "Bingo: " + r.getLongName());
                for (Time t : startStop.getTimes().get(timeOfWeek).get(r.getLongName())){
                    times.add(t);
                }
                for (Time t : times){
                    t.setRoute(r.getLongName());
                }
                tempTimesBetweenStartAndEnd.addAll(times);
            }
            if (tempTimesBetweenStartAndEnd != null && tempTimesBetweenStartAndEnd.size() > 0){
                timesBetweenStartAndEnd = tempTimesBetweenStartAndEnd;
                routesBetweenToAndFrom = routes;
                Time currentTime = new Time(rightNow.get(Calendar.HOUR_OF_DAY), rightNow.get(Calendar.MINUTE));
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
            }
            else{
                Context context = getApplicationContext();
                CharSequence text = "That stop is unavailable!!";
                int duration = Toast.LENGTH_SHORT;

                if (context != null){
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
            }
        } else
        {
            busRefreshTimer.cancel();
            timeUntilTimer.cancel();

            Context context = getApplicationContext();
            CharSequence text = "That stop is unavailable today!";
            int duration = Toast.LENGTH_SHORT;

            if (context != null){
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        }

        renewBusRefreshTimer();
        renewTimeUntilTimer();
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
