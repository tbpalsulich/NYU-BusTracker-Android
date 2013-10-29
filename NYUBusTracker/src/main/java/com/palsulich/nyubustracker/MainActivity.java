package com.palsulich.nyubustracker;

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

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    String charset = "UTF-8";
    String agencies = "72";
    String query = makeQuery("agencies", agencies, charset);

    Stop fromStop;
    Stop toStop;
    Route routeBetweenToAndFrom;

    Timer myTimer;

    private String stopsURL = "http://api.transloc.com/1.2/stops.json?" + query;
    private String routesURL = "http://api.transloc.com/1.2/routes.json?" + query;
    private String segmentsURL = "http://api.transloc.com/1.2/segments.json?" + query;
    private String vehiclesURL = "http://api.transloc.com/1.2/vehicles.json?" + query;
    private String versionURL = "https://s3.amazonaws.com/nyubustimes/1.0/version.json";

    private String makeQuery(String param, String value, String charset) {
        try {
            return String.format(param + "=" + URLEncoder.encode(agencies, charset));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    static final String TAG_DATA = "data";
    static final String TAG_LONG_NAME = "long_name";
    static final String TAG_LOCATION = "location";
    static final String TAG_LAT = "lat";
    static final String TAG_LNG = "lng";
    static final String TAG_HEADING = "heading";
    static final String TAG_STOP_NAME = "name";
    static final String TAG_STOP_ID = "stop_id";
    static final String TAG_ROUTES = "routes";
    static final String TAG_ROUTE = "route";
    static final String TAG_ROUTE_ID = "route_id";
    static final String TAG_WEEKDAY = "Weekday";
    static final String TAG_FRIDAY = "Friday";
    static final String TAG_WEEKEND = "Weekend";
    static final String TAG_VEHICLE_ID = "vehicle_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        final ListView listView = (ListView) findViewById(R.id.mainActivityList);

        final BusManager sharedManager = BusManager.getBusManager(getApplicationContext());
        if (!sharedManager.hasStops() && !sharedManager.hasRoutes()) {
            FileGrabber mFileGrabber = new FileGrabber(getCacheDir());
            try {
                Stop.parseJSON(mFileGrabber.getJSON(stopsURL, "stopsJSON"));
                Route.parseJSON(mFileGrabber.getJSON(routesURL, "routesJSON"));
                BusManager.parseTimes(mFileGrabber.getJSON(versionURL, "versionJSON"), mFileGrabber);
                Bus.parseJSON(mFileGrabber.getJSON(vehiclesURL, "vehiclesJSON"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        setFromStop("715 Broadway at Washington Square");
        setToStop("80 Lafayette Street");

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        myTimer.cancel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void setToStop(String stopName) {
        toStop = BusManager.getBusManager().getStopByName(stopName);
        ((Button) findViewById(R.id.to_button)).setText("To: " + stopName);
        if (fromStop != null) setNextBusTime();
    }

    private void setFromStop(String stopName) {
        if (toStop != null && toStop.name.equals(stopName)) {
            Stop temp = fromStop;
            fromStop = toStop;
            ((Button) findViewById(R.id.from_button)).setText("From: " + fromStop.name);
            toStop = temp;
            ((Button) findViewById(R.id.to_button)).setText("To: " + toStop.name);
            setNextBusTime();
        } else {
            fromStop = BusManager.getBusManager().getStopByName(stopName);
            ((Button) findViewById(R.id.from_button)).setText("From: " + stopName);
            if (toStop != null) setNextBusTime();
        }
    }

    private String getTimeOfWeek() {
        Calendar rightNow = Calendar.getInstance();
        String dayOfWeek = rightNow.getDisplayName(rightNow.DAY_OF_WEEK, rightNow.LONG, Locale.getDefault());
        String timeOfWeek = "Weekday";
        if (dayOfWeek.equals("Saturday") || dayOfWeek.equals("Sunday")) timeOfWeek = "Weekend";
        else if (dayOfWeek.equals("Friday")) timeOfWeek = "Friday";

        return timeOfWeek;
    }

    private void setNextBusTime() {
        Calendar rightNow = Calendar.getInstance();
        ArrayList<Route> fromRoutes = fromStop.getRoutes();
        Route route = null;
        for (int i = 0; i < fromRoutes.size(); i++) {
            if (fromRoutes.get(i).hasStop(toStop.name)) {
                route = fromRoutes.get(i);
            }
        }
        if (route != null) {
            routeBetweenToAndFrom = route;
            String timeOfWeek = getTimeOfWeek();
            String[] times = fromStop.times.get(timeOfWeek).get(route.longName);
            int hour = 24;
            int min = 60;
            int closestHourToBus = 24;
            int closestMinToBus = 60;
            String time = "";
            int currentHour = rightNow.get(rightNow.HOUR_OF_DAY);
            int currentMin = rightNow.get(rightNow.MINUTE);
            for (int i = 0; i < times.length; i++) {
                int am = (times[i].contains("AM")) ? 0 : 12;
                int tempHour = Integer.parseInt(times[i].substring(0, times[i].indexOf(":")).trim());
                if (am == 0 || tempHour != 12) tempHour += am;
                int tempMin = Integer.parseInt(times[i].substring(times[i].indexOf(":") + 1, times[i].indexOf(" ")).trim());
/*                Log.v("Times", "Route: " + route.longName + " | am: " + am +
                        " | hour: " + tempHour + " | min: " + tempMin + " | currentHour: " + currentHour +
                        " | currentMin: " + currentMin);*/
                int hoursUntilBus = tempHour - currentHour;
                int minutesUntilBus = tempMin - currentMin;
                if (minutesUntilBus < 0) {
                    hoursUntilBus--;
                    minutesUntilBus += 60;
                }
                if (hoursUntilBus >= 0 && hoursUntilBus <= closestHourToBus && minutesUntilBus < closestMinToBus) {
                    hour = tempHour;
                    min = tempMin;
                    closestHourToBus = hoursUntilBus;
                    closestMinToBus = minutesUntilBus;
                    time = times[i];
                }
            }
            String hours = "";
            String minutes = "";
            if (closestHourToBus > 0) {
                if (closestHourToBus > 1)
                    hours = "Next bus is in " + closestHourToBus + " hours and ";
                else if (closestHourToBus == 1)
                    hours = "Next bus is in " + closestHourToBus + " hour and ";
                if (closestMinToBus > 1) minutes = closestMinToBus + " minutes.";
                else if (closestMinToBus == 0) minutes = "";
            } else if (closestMinToBus > 0 && closestHourToBus == 0) {
                hours = "";
                if (closestMinToBus > 1)
                    minutes = "Next bus is in " + closestMinToBus + " minutes.";
                else if (closestMinToBus == 0)
                    minutes = "Next bus is in " + closestMinToBus + " minute.";
            } else {
                hours = "";
                minutes = "Next bus is right now!";
            }
            ((TextView) findViewById(R.id.times_button)).setText(time);
            if (time.length() > 0)
                ((TextView) findViewById(R.id.next_bus)).setText(hours + minutes);
            else
                ((TextView) findViewById(R.id.next_bus)).setText("I don't know when the next bus is!");

        } else {
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
        String timeOfWeek = getTimeOfWeek();
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        final String[] times = fromStop.times.get(timeOfWeek).get(routeBetweenToAndFrom.longName);
        builder.setItems(times, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Nothing to do, ish.
            }
        });
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

}
