package com.palsulich.nyubustracker.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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


    Stop fromStop;
    Stop toStop;
    ArrayList<Route> routesBetweenToAndFrom;
    ArrayList<Time> timesBetweenStartAndEnd;

    Timer myTimer;

    public static final String TAG_DATA = "data";
    public static final String TAG_LONG_NAME = "long_name";
    public static final String TAG_LOCATION = "location";
    public static final String TAG_LAT = "lat";
    public static final String TAG_LNG = "lng";
    public static final String TAG_HEADING = "heading";
    public static final String TAG_STOP_NAME = "name";
    public static final String TAG_STOP_ID = "stop_id";
    public static final String TAG_ROUTES = "routes";
    public static final String TAG_ROUTE = "route";
    public static final String TAG_ROUTE_ID = "route_id";
    public static final String TAG_WEEKDAY = "Weekday";
    public static final String TAG_FRIDAY = "Friday";
    public static final String TAG_WEEKEND = "Weekend";
    public static final String TAG_VEHICLE_ID = "vehicle_id";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        renewTimer();

        final ListView listView = (ListView) findViewById(R.id.mainActivityList);

        final BusManager sharedManager = BusManager.getBusManager(getApplicationContext());
        FileGrabber mFileGrabber = new FileGrabber(getCacheDir());
        if (!sharedManager.hasStops() && !sharedManager.hasRoutes()) {
            try {
                Stop.parseJSON(mFileGrabber.getStopJSON());
                Route.parseJSON(mFileGrabber.getRouteJSON());
                BusManager.parseTimes(mFileGrabber.getVersionJSON(), mFileGrabber);
                Bus.parseJSON(mFileGrabber.getVehicleJSON());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        setFromStop(mFileGrabber.getFromStopFile());
        setToStop(mFileGrabber.getToStopFile());

        ArrayAdapter<String> mAdapter =
                new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_1,
                        sharedManager.getRoutesAsArray());
        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String routeName = listView.getItemAtPosition(position).toString();
                Log.v("Debugging", "Clicked on route:" + routeName);
                Intent myIntent = new Intent(MainActivity.this, RouteActivity.class);
                myIntent.putExtra("route_name", routeName);
                startActivity(myIntent);
            }
        });
        listView.setAdapter(mAdapter);
    }

    private void renewTimer() {
        Calendar rightNow = Calendar.getInstance();

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
        cacheToAndFromStop();
        myTimer.cancel();
    }

    @Override
    public void onPause() {
        super.onPause();
        cacheToAndFromStop();
        myTimer.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();
        renewTimer();
    }

    public void cacheToAndFromStop() {
        FileGrabber mFileGrabber = new FileGrabber(getCacheDir());
        mFileGrabber.setToStop(toStop.getName());
        mFileGrabber.setFromStop(fromStop.getName());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void setToStop(String stopName) {
        if (stopName.equals("")) stopName = "80 Lafayette Street";
        toStop = BusManager.getBusManager().getStopByName(stopName);
        ((Button) findViewById(R.id.to_button)).setText("To: " + stopName);
        if (fromStop != null) setNextBusTime();
    }

    private void setFromStop(String stopName) {
        if (stopName.equals("")) stopName = "715 Broadway at Washington Square";
        if (toStop != null && toStop.getName().equals(stopName)) {
            Stop temp = fromStop;
            fromStop = toStop;
            ((Button) findViewById(R.id.from_button)).setText("From: " + fromStop.getName());
            toStop = temp;
            ((Button) findViewById(R.id.to_button)).setText("To: " + toStop.getName());
            setNextBusTime();
        } else {
            fromStop = BusManager.getBusManager().getStopByName(stopName);
            ((Button) findViewById(R.id.from_button)).setText("From: " + stopName);
            if (toStop != null) setNextBusTime();
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
        ArrayList<Route> fromRoutes = fromStop.getRoutes();
        ArrayList<Route> routes = new ArrayList<Route>();
        for (int i = 0; i < fromRoutes.size(); i++) {
            if (fromRoutes.get(i).hasStop(toStop.getName())) {
                Log.v("Route Debugging", "Adding a route!");
                routes.add(fromRoutes.get(i));
            }
        }
        if (routes.size() != 0) {
            routesBetweenToAndFrom = routes;
            timesBetweenStartAndEnd = new ArrayList<Time>();
            for (int j = 0; j < routes.size(); j++) {
                String timeOfWeek = getTimeOfWeek();
                // Get the Times at this stop for this route.
                List<Time> times = Arrays.asList(fromStop.getTimes()
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
        final String[] connectedStops = BusManager.getBusManager().getConnectedStops(fromStop);
        builder.setItems(connectedStops, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                setToStop(connectedStops[which]);
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
                setFromStop(stops[which]);
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
