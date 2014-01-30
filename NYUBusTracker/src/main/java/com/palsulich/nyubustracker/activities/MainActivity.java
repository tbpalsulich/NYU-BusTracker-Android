package com.palsulich.nyubustracker.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
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
import com.palsulich.nyubustracker.adapters.StopAdapter;
import com.palsulich.nyubustracker.adapters.TimeAdapter;
import com.palsulich.nyubustracker.helpers.BusManager;
import com.palsulich.nyubustracker.models.Bus;
import com.palsulich.nyubustracker.models.Route;
import com.palsulich.nyubustracker.models.Stop;
import com.palsulich.nyubustracker.models.Time;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class MainActivity extends Activity {
    Stop startStop;     // Stop object to keep track of the start location of the desired route.
    Stop endStop;       // Keep track of the desired end location.
    ArrayList<Route> routesBetweenStartAndEnd;        // List of all routes between start and end.
    ArrayList<Time> timesBetweenStartAndEnd;        // List of all times between start and end.
    HashMap<String, Boolean> clickableMapMarkers;   // Hash of all markers which are clickable (so we don't zoom in on buses).
    ArrayList<Marker> busesOnMap = new ArrayList<Marker>();

    private static final String query = makeQuery("agencies", "72", "UTF-8");
    private static final String stopsURL = "http://api.transloc.com/1.2/stops.json?" + query;
    private static final String routesURL = "http://api.transloc.com/1.2/routes.json?" + query;
    private static final String segmentsURL = "http://api.transloc.com/1.2/segments.json?" + query;
    private static final String vehiclesURL = "http://api.transloc.com/1.2/vehicles.json?" + query;
    private static final String versionURL = "https://s3.amazonaws.com/nyubustimes/1.0/version.json";
    private static final String RUN_ONCE_PREF = "runOnce";
    private static final String STOP_PREF = "stops";
    private static final String START_STOP_PREF = "startStop";
    private static final String END_STOP_PREF = "endStop";
    private static final String TIME_VERSION_PREF = "stopVersions";
    private static final String FIRST_TIME = "firstTime";
    private static final String STOP_JSON_FILE = "stopJson";
    private static final String ROUTE_JSON_FILE = "routeJson";
    private static final String SEGMENT_JSON_FILE = "segmentJson";
    private static final String VERSION_JSON_FILE = "versionJson";

    private static String makeQuery(String param, String value, String charset) {
        try {
            return String.format(param + "=" + URLEncoder.encode(value, charset));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    private Downloader stopDownloader = new Downloader(new DownloaderHelper() {
        @Override
        public void parse(JSONObject jsonObject) {
            try {
                Stop.parseJSON(jsonObject);
                FileOutputStream fos = openFileOutput(STOP_JSON_FILE, MODE_PRIVATE);
                fos.write(jsonObject.toString().getBytes());
                fos.close();
            } catch (JSONException e) {
                Log.e("JSON", "Error parsing Stop JSON.");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e("JSON", "Error with Stop JSON IO.");
                e.printStackTrace();
            }
        }
    });

    private Downloader routeDownloader = new Downloader(new DownloaderHelper() {
        @Override
        public void parse(JSONObject jsonObject) {
            try {
                Route.parseJSON(jsonObject);
                FileOutputStream fos = openFileOutput(ROUTE_JSON_FILE, MODE_PRIVATE);
                fos.write(jsonObject.toString().getBytes());
                fos.close();
            } catch (JSONException e) {
                Log.e("JSON", "Error parsing Route JSON.");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e("JSON", "Error with Route JSON IO.");
                e.printStackTrace();
            }
        }
    });

    private Downloader segmentDownloader = new Downloader(new DownloaderHelper() {
        @Override
        public void parse(JSONObject jsonObject) {
            try {
                BusManager.parseSegments(jsonObject);
                FileOutputStream fos = openFileOutput(SEGMENT_JSON_FILE, MODE_PRIVATE);
                fos.write(jsonObject.toString().getBytes());
                fos.close();
            } catch (JSONException e) {
                Log.e("JSON", "Error parsing Segment JSON.");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e("JSON", "Error with Segment JSON IO.");
                e.printStackTrace();
            }
        }
    });

    private Downloader versionDownloader = new Downloader(new DownloaderHelper() {
        @Override
        public void parse(JSONObject jsonObject) {
            try {
                BusManager.parseVersion(jsonObject);
                for (String timeURL : BusManager.getTimesToDownload()){
                    SharedPreferences preferences = getSharedPreferences(TIME_VERSION_PREF, MODE_PRIVATE);
                    String stopID = timeURL.substring(timeURL.lastIndexOf("/") + 1, timeURL.indexOf(".json"));
                    Log.v("Refactor", "Time to download: " + stopID);
                    int newestStopTimeVersion = BusManager.getTimesVersions().get(stopID);
                    if (preferences.getInt(stopID, 0) != newestStopTimeVersion){
                        new Downloader(timeDownloaderHelper).execute(timeURL);
                        preferences.edit().putInt(stopID, newestStopTimeVersion);
                    }
                }
                FileOutputStream fos = openFileOutput(VERSION_JSON_FILE, MODE_PRIVATE);
                fos.write(jsonObject.toString().getBytes());
                fos.close();
            } catch (JSONException e) {
                Log.e("JSON", "Error parsing Version JSON.");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e("JSON", "Error with Version JSON IO.");
                e.printStackTrace();
            }
        }
    });

    private DownloaderHelper busDownloaderHelper = new DownloaderHelper() {
        @Override
        public void parse(JSONObject jsonObject) {
            try {
                Bus.parseJSON(jsonObject);
                updateMapWithNewBusLocations();
            } catch (JSONException e) {
                Log.e("JSON", "Error parsing Vehicle JSON.");
                e.printStackTrace();
            }
        }
    };

    private DownloaderHelper timeDownloaderHelper = new DownloaderHelper() {
        @Override
        public void parse(JSONObject jsonObject) {
            try {
                BusManager.parseTime(jsonObject);
                Log.v("Refactor", "Creating time cache file: " + jsonObject.getString("stop_id"));
                FileOutputStream fos = openFileOutput(jsonObject.getString("stop_id"), MODE_PRIVATE);
                fos.write(jsonObject.toString().getBytes());
                fos.close();
            } catch (JSONException e) {
                Log.e("JSON", "Error parsing Time JSON.");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e("JSON", "Error with Time JSON IO.");
                e.printStackTrace();
            }
        }
    };

    Time nextBusTime;

    Timer timeUntilTimer;  // Timer used to refresh the "time until next bus" every minute, on the minute.
    Timer busRefreshTimer; // Timer used to refresh the bus locations every few seconds.

    private GoogleMap mMap;     // Map to display all stops, segments, and buses.
    private boolean haveAMap = false;   // Flag to see if the device can display a map.

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            MapFragment mFrag = ((MapFragment) getFragmentManager().findFragmentById(R.id.map));
            if (mFrag != null) mMap = mFrag.getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                // The Map is verified. It is now safe to manipulate the map.
                mMap.getUiSettings().setRotateGesturesEnabled(false);
                mMap.getUiSettings().setZoomControlsEnabled(false);
                haveAMap = true;
            } else haveAMap = false;
        }
    }

    public String readSavedData (String fileName) {
        Log.v("Refactor", "Reading saved data from " + fileName);
        StringBuilder buffer = new StringBuilder("");
        try {
            FileInputStream inputStream = openFileInput(fileName);
            InputStreamReader streamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(streamReader);

            String readString = bufferedReader.readLine();
            while (readString != null) {
                buffer.append(readString);
                readString = bufferedReader.readLine();
            }

            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer.toString() ;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Log.v("General Debugging", "onCreate!");
        setContentView(R.layout.activity_main);

        final ProgressDialog progressDialog;

        int retCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
        if (retCode != ConnectionResult.SUCCESS) {
            GooglePlayServicesUtil.getErrorDialog(retCode, this, 1).show();
        }

        setUpMapIfNeeded(); // Instantiates mMap, if it needs to be.

        if (haveAMap) mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return !clickableMapMarkers.get(marker.getId());    // Return true to consume the event.
            }
        });

        // Singleton BusManager to keep track of all stops, routes, etc.
        final BusManager sharedManager = BusManager.getBusManager();

        SharedPreferences preferences = getSharedPreferences(RUN_ONCE_PREF, MODE_PRIVATE);
        if (preferences.getBoolean(FIRST_TIME, true)) {
            preferences.edit().putBoolean(FIRST_TIME, false).commit();
            ConnectivityManager connMgr = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                // Download and parse everything, put it all in persistent memory, continue.
                progressDialog = ProgressDialog.show(this, "Downloading Data", "Please wait...", true, false);

                stopDownloader.execute(stopsURL);
                routeDownloader.execute(routesURL);
                segmentDownloader.execute(segmentsURL);
                versionDownloader.execute(versionURL);
                final AsyncTask busTask = new Downloader(busDownloaderHelper).execute(vehiclesURL);

                new Timer().scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (stopDownloader.getStatus() == AsyncTask.Status.FINISHED &&
                            routeDownloader.getStatus() == AsyncTask.Status.FINISHED &&
                            segmentDownloader.getStatus() == AsyncTask.Status.FINISHED &&
                            versionDownloader.getStatus() == AsyncTask.Status.FINISHED &&
                            busTask.getStatus() == AsyncTask.Status.FINISHED){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Stop broadway = sharedManager.getStopByName("715 Broadway @ Washington Square");
                                    Stop lafayette = sharedManager.getStopByName("80 Lafayette St");
                                    getSharedPreferences(Stop.FAVORITES_PREF, MODE_PRIVATE)
                                            .edit()
                                            .putBoolean(broadway.getID(), true).commit();
                                    setStartStop(broadway);
                                    setEndStop(lafayette);
                                    broadway.setFavorite(true);
                                    Log.v("Refactor", "End: " + endStop.getName());
                                    // Update the map to show the corresponding stops, buses, and segments.
                                    if (routesBetweenStartAndEnd != null) updateMapWithNewStartOrEnd();
                                    renewBusRefreshTimer();
                                    renewTimeUntilTimer();
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    setNextBusTime();
                                                    progressDialog.dismiss();
                                                }
                                            });
                                        }
                                    }, 750L);
                                }

                            });
                            this.cancel();
                        }
                    }
                }, 0L, 500L);
            } else {
                Context context = getApplicationContext();
                CharSequence text = "Unable to connect to the network.";
                int duration = Toast.LENGTH_SHORT;

                if (context != null){
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
            }
        } else {
            if (!sharedManager.hasRoutes() || !sharedManager.hasStops()){
                Log.v("Refactor", "Parsing cached files...");
                try{
                    Stop.parseJSON(new JSONObject(readSavedData(STOP_JSON_FILE)));
                    Route.parseJSON(new JSONObject(readSavedData(ROUTE_JSON_FILE)));
                    BusManager.parseSegments(new JSONObject(readSavedData(SEGMENT_JSON_FILE)));
                    BusManager.parseVersion(new JSONObject(readSavedData(VERSION_JSON_FILE)));
                    for (String timeURL : BusManager.getTimesToDownload()){
                        String timeFileName = timeURL.substring(timeURL.lastIndexOf("/") + 1, timeURL.indexOf(".json"));
                        Log.v("Refactor", "Trying to parse " + timeFileName);
                        BusManager.parseTime(new JSONObject(readSavedData(timeFileName)));
                    }
                    preferences = getSharedPreferences(Stop.FAVORITES_PREF, MODE_PRIVATE);
                    Log.v("Refactor", "Done parsing...");
                    for (Stop s : sharedManager.getStops()){
                        boolean result = preferences.getBoolean(s.getID(), false);
                        Log.v("Refactor", s.getName() + " is " + result);
                        s.setFavorite(result);
                    }
                    //TODO: Make network call to check version. But, should ask the user how often they want to check for updates.
                } catch (JSONException e){
                    Log.e("RefactorJSON", "Error with JSON parsing cached file.");
                    e.printStackTrace();
                }
            }
            preferences = getSharedPreferences(STOP_PREF, MODE_PRIVATE);
            setStartStop(sharedManager.getStopByName(preferences.getString(START_STOP_PREF, "715 Broadway @ Washington Square")));
            setEndStop(sharedManager.getStopByName(preferences.getString(END_STOP_PREF, "80 Lafayette St")));

            // Update the map to show the corresponding stops, buses, and segments.
            if (routesBetweenStartAndEnd != null) updateMapWithNewStartOrEnd();

            // Get the location of the buses every 10 sec.
            renewBusRefreshTimer();
            renewTimeUntilTimer();
        }
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
                        if (startStop != null && endStop != null) setNextBusTime();
                    }
                });
            }
        }, (60 - rightNow.get(Calendar.SECOND)) * 1000, 60000);
    }

    private void renewBusRefreshTimer() {
        if (haveAMap) {
            if (busRefreshTimer != null) busRefreshTimer.cancel();

            busRefreshTimer = new Timer();
            busRefreshTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ConnectivityManager connMgr = (ConnectivityManager)
                                    getSystemService(Context.CONNECTIVITY_SERVICE);
                            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                            if (networkInfo != null && networkInfo.isConnected()) {
                                new Downloader(busDownloaderHelper).execute(vehiclesURL);
                            }
                            else{
                                Context context = getApplicationContext();
                                CharSequence text = "Unable to connect to the network.";
                                int duration = Toast.LENGTH_SHORT;

                                if (context != null){
                                    Toast toast = Toast.makeText(context, text, duration);
                                    toast.show();
                                }
                            }
                        }
                    });
                }
            }, 0, 1500L);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Log.v("General Debugging", "onDestroy!");
        cacheToAndStartStop();      // Remember user's preferences across lifetimes.
        timeUntilTimer.cancel();           // Don't need a timer anymore -- must be recreated onResume.
        busRefreshTimer.cancel();
    }

    @Override
    public void onPause() {
        super.onPause();
        //Log.v("General Debugging", "onPause!");
        cacheToAndStartStop();
        if (timeUntilTimer != null) timeUntilTimer.cancel();
        if (busRefreshTimer != null) busRefreshTimer.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Log.v("General Debugging", "onResume!");
        if (endStop != null && startStop != null) {
            setNextBusTime();
            renewTimeUntilTimer();
            renewBusRefreshTimer();
            setUpMapIfNeeded();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }


    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }

    public void cacheToAndStartStop() {
        if (endStop != null)
            getSharedPreferences(STOP_PREF, MODE_PRIVATE).edit().putString(END_STOP_PREF, endStop.getName()).commit();         // Creates or updates cache file.
        if (startStop != null)
            getSharedPreferences(STOP_PREF, MODE_PRIVATE).edit().putString(START_STOP_PREF, startStop.getName()).commit();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }


    // Clear the map of all buses and put them all back on in their new locations.
    private void updateMapWithNewBusLocations() {
        if (haveAMap && routesBetweenStartAndEnd != null) {
            BusManager sharedManager = BusManager.getBusManager();
            for (Marker m : busesOnMap) {
                m.remove();
            }
            busesOnMap = new ArrayList<Marker>();
            if (clickableMapMarkers == null)
                clickableMapMarkers = new HashMap<String, Boolean>();  // New set of buses means new set of clickable markers!
            for (Route r : routesBetweenStartAndEnd) {
                if (r.isActive()) {
                    for (Bus b : sharedManager.getBuses()) {
                        //Log.v("BusLocations", "bus id: " + b.getID() + ", bus route: " + b.getRoute() + " vs route: " + r.getID());
                        if (b.getRoute().equals(r.getID())) {
                            Marker mMarker = mMap.addMarker(new MarkerOptions()
                                    .position(b.getLocation())
                                    .icon(BitmapDescriptorFactory
                                            .fromBitmap(
                                                    rotateBitmap(
                                                            BitmapFactory.decodeResource(
                                                                    this.getResources(),
                                                                    R.drawable.ic_bus_arrow),
                                                            b.getHeading())
                                            ))
                                    .anchor(0.5f, 0.5f));
                            clickableMapMarkers.put(mMarker.getId(), false);    // Unable to click on buses.
                            busesOnMap.add(mMarker);
                        }
                    }
                }
            }
        }
    }


    // Clear the map, because we may have just changed what route we wish to display. Then, add everything back onto the map.
    private void updateMapWithNewStartOrEnd() {
        if (haveAMap) {
            BusManager sharedManager = BusManager.getBusManager();
            setUpMapIfNeeded();
            mMap.clear();
            clickableMapMarkers = new HashMap<String, Boolean>();
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            boolean validBuilder = false;
            for (Route r : routesBetweenStartAndEnd) {
                if (r.isActive()) {
                    //Log.v("MapDebugging", "Updating map with route: " + r.getLongName());
                    for (Stop s : r.getStops()) {
                        for (Stop f : s.getFamily()) {
                            if ((!f.isHidden() && !f.isRelatedTo(startStop) && !f.isRelatedTo(endStop))
                                    || (f == startStop || f == endStop)) {
                                // Only put one representative from a family of stops on the p
                                //Log.v("MapDebugging", "Not hiding " + f);
                                Marker mMarker = mMap.addMarker(new MarkerOptions()      // Adds a balloon for every stop to the map.
                                        .position(f.getLocation())
                                        .title(f.getName())
                                        .anchor(0.5f, 0.5f)
                                        .icon(BitmapDescriptorFactory
                                                .fromBitmap(
                                                        BitmapFactory.decodeResource(
                                                                this.getResources(),
                                                                R.drawable.ic_map_stop))));
                                clickableMapMarkers.put(mMarker.getId(), true);
                            } else {
                                //Log.v("MapDebugging", "** Hiding " + f);
                                //Log.v("MapDebugging", "      " + f.isHidden());// && !s.isRelatedTo(startStop) && !s.isRelatedTo(endStop)));
                            }
                        }
                    }
                    updateMapWithNewBusLocations();
                    // Adds the segments of every Route to the map.
                    for (String id : r.getSegmentIDs()) {
                        //Log.v("MapDebugging", "Trying to add a segment to the map: " + id);
                        PolylineOptions p = BusManager.getSegment(id);
                        if (p != null) {
                            for (LatLng loc : p.getPoints()) {
                                validBuilder = true;
                                builder.include(loc);
                            }
                            p.color(getResources().getColor(R.color.purple));
                            mMap.addPolyline(p);
                            //Log.v("MapDebugging", "Success!");
                        }
                        //else Log.v("MapDebugging", "Segment was null for " + r.getID());
                    }
                }
            }
            if (validBuilder) {
                LatLngBounds bounds = builder.build();
                try {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 30));
                } catch (IllegalStateException e) {      // In case the view is not done being created.
                    e.printStackTrace();
                    mMap.moveCamera(
                            CameraUpdateFactory
                                    .newLatLngBounds(
                                            bounds,
                                            this.getResources().getDisplayMetrics().widthPixels,
                                            this.getResources().getDisplayMetrics().heightPixels,
                                            100));
                }
            }
        }
    }

    private void setEndStop(Stop stop) {
        if (stop != null) {     // Make sure we actually have a stop!
            // Check there is a route between these stops.
            ArrayList<Route> routes = new ArrayList<Route>();               // All the routes connecting the two.
            for (Route r : startStop.getRoutes()) {
                if (r.hasStop(stop)) {
                    routes.add(r);
                }
            }
            if (routes.size() > 0) {
                endStop = stop;
                ((Button) findViewById(R.id.to_button)).setText(stop.getUltimateName());
                if (startStop != null) {
                    setNextBusTime();    // Don't set the next bus if we don't have a valid route.
                    if (routesBetweenStartAndEnd != null && haveAMap) updateMapWithNewStartOrEnd();
                }
            }
        }
    }

    private void setStartStop(Stop stop) {
        if (stop != null) {
            if (endStop == stop) {    // We have an end stop and its name is the same as stopName.
                // Swap the start and end stops.
                Stop temp = startStop;
                startStop = endStop;
                ((Button) findViewById(R.id.from_button)).setText(startStop.getUltimateName());
                endStop = temp;
                ((Button) findViewById(R.id.to_button)).setText(endStop.getUltimateName());
                setNextBusTime();
                updateMapWithNewStartOrEnd();
            } else { // We have a new start. So, we must ensure the end is actually connected. If not, pick a random connected stop.
                startStop = stop;
                ((Button) findViewById(R.id.from_button)).setText(stop.getUltimateName());
                if (endStop != null) {
                    // Loop through all connected Routes.
                    for (Route r : startStop.getRoutes()) {
                        if (r.hasStop(endStop) && endStop.getTimesOfRoute(r.getLongName()).size() > 0) {  // If the current endStop is connected, we don't have to change endStop.
                            setNextBusTime();
                            updateMapWithNewStartOrEnd();
                            return;
                        }
                    }
                    // If we did not return above, the current endStop is not connected to the new
                    // startStop. So, by default, pick the first connected stop.
                    BusManager sharedManager = BusManager.getBusManager();
                    setEndStop(sharedManager.getStopByName("715 Broadway @ Washington Square"));
                }
            }
        }
    }

    private void setNextBusTime() {
        /*
        Have a start stop that may have children, and an end stop that may have children.
        Find all routes that have both stops (or their children).
        Figure out which direction the user is going.
        Get all times in that direction, on all available routes, from that start stop (or its children).
        Insert the current time into that list and sort the list.
        Next bus time is the first element after the current time.
        Set the view values.
         */

        if (timeUntilTimer != null)
            timeUntilTimer.cancel();        // Don't want to be interrupted in the middle of this.
        if (busRefreshTimer != null) busRefreshTimer.cancel();
        Calendar rightNow = Calendar.getInstance();
        ArrayList<Route> startRoutes = startStop.getUltimateParent().getRoutes();        // All the routes leaving the start stop.
        ArrayList<Route> endRoutes = endStop.getUltimateParent().getRoutes();
        ArrayList<Route> availableRoutes = new ArrayList<Route>();               // All the routes connecting the two.
        for (Route r : startRoutes) {
            if (endRoutes.contains(r)) {
                availableRoutes.add(r);
            }
        }
        int bestDistance = BusManager.distanceBetween(startStop, endStop);

        int testDistance = BusManager.distanceBetween(startStop.getOppositeStop(), endStop.getOppositeStop());
        if (testDistance < bestDistance) {
            startStop = startStop.getOppositeStop();
            endStop = endStop.getOppositeStop();
        }

        testDistance = BusManager.distanceBetween(startStop, endStop.getOppositeStop());
        if (testDistance < bestDistance) {
            endStop = endStop.getOppositeStop();
        }

        testDistance = BusManager.distanceBetween(startStop.getOppositeStop(), endStop);
        if (testDistance < bestDistance) {
            startStop = startStop.getOppositeStop();
        }

        if (availableRoutes.size() > 0) {
            ArrayList<Time> tempTimesBetweenStartAndEnd = new ArrayList<Time>();
            for (Route r : availableRoutes) {
                // Get the Times at this stop for this route.
                ArrayList<Time> times = startStop.getTimesOfRoute(r.getLongName());
                if (times.size() > 0) {
                    tempTimesBetweenStartAndEnd.addAll(times);
                }
            }
            if (tempTimesBetweenStartAndEnd.size() > 0) {    // We actually found times.
                // Here, we grab the list of all times of all routes between the start and end, add in the current
                // time, then sort that list of times. That way, we know the first bus Time after the current time
                // is the Time of the soonest next Bus.
                timesBetweenStartAndEnd = new ArrayList<Time>(tempTimesBetweenStartAndEnd);
                routesBetweenStartAndEnd = availableRoutes;
                Time currentTime = new Time(rightNow.get(Calendar.HOUR_OF_DAY), rightNow.get(Calendar.MINUTE));
                tempTimesBetweenStartAndEnd.add(currentTime);
                Collections.sort(tempTimesBetweenStartAndEnd, Time.compare);
                Collections.sort(timesBetweenStartAndEnd, Time.compare);
                int index = tempTimesBetweenStartAndEnd.indexOf(currentTime);
                nextBusTime = tempTimesBetweenStartAndEnd.get((index + 1) % tempTimesBetweenStartAndEnd.size());
                ((TextView) findViewById(R.id.next_time)).setText(currentTime.getTimeAsStringUntil(nextBusTime));
                if (BusManager.getBusManager().isOnline()) {
                    ((TextView) findViewById(R.id.next_route)).setText("via Route " + nextBusTime.getRoute());
                    ((TextView) findViewById(R.id.next_bus)).setText("Next Bus In:");
                } else {
                    ((TextView) findViewById(R.id.next_route)).setText("");
                    ((TextView) findViewById(R.id.next_bus)).setText("");
                }
                updateMapWithNewStartOrEnd();
            }
        }
        renewBusRefreshTimer();
        renewTimeUntilTimer();
    }

    CompoundButton.OnCheckedChangeListener cbListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Stop s = (Stop) buttonView.getTag();
            s.setFavorite(isChecked);
            getSharedPreferences(Stop.FAVORITES_PREF, MODE_PRIVATE)
                    .edit().putBoolean(s.getID(), isChecked)
                    .commit();
        }
    };

    public void createEndDialog(View view) {
        // Get all stops connected to the start stop.
        final ArrayList<Stop> connectedStops = BusManager.getBusManager().getConnectedStops(startStop);
        ListView listView = new ListView(this);     // ListView to populate the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);    // Used to build the dialog with the list of connected Stops.
        builder.setView(listView);
        final Dialog dialog = builder.create();
        // An adapter takes some data, then adapts it to fit into a view. The adapter supplies the individual view elements of
        // the list view. So, in this case, we supply the StopAdapter with a list of stops, and it gives us back the nice
        // views with a heart button to signify favorites and a TextView with the name of the stop.
        // We provide the onClickListeners to the adapter, which then attaches them to the respective views.
        StopAdapter adapter = new StopAdapter(getApplicationContext(), connectedStops,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Clicked on a Stop. So, make it the end and dismiss the dialog.
                        Stop s = (Stop) view.getTag();
                        setEndStop(s);  // Actually set the end stop.
                        dialog.dismiss();
                    }
                }, cbListener);
        listView.setAdapter(adapter);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();  // Dismissed when a stop is clicked.
    }

    public void createStartDialog(View view) {
        final ArrayList<Stop> stops = BusManager.getBusManager().getStops();    // Show every stop as an option to start.
        ListView listView = new ListView(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(listView);
        final Dialog dialog = builder.create();
        StopAdapter adapter = new StopAdapter(getApplicationContext(), stops,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Stop s = (Stop) view.getTag();
                        setStartStop(s);    // Actually set the start stop.
                        dialog.dismiss();
                    }
                }, cbListener);
        listView.setAdapter(adapter);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void createTimesDialog(View view) {
        if (timesBetweenStartAndEnd != null) {
            // Library provided ListView with headers that (gasp) stick to the top.
            StickyListHeadersListView listView = new StickyListHeadersListView(this);
            listView.setDivider(new ColorDrawable(0xffffff));
            listView.setDividerHeight(1);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            TimeAdapter adapter = new TimeAdapter(getApplicationContext(), timesBetweenStartAndEnd);
            listView.setAdapter(adapter);
            int index = timesBetweenStartAndEnd.indexOf(nextBusTime);
            listView.setSelection(index);
            builder.setView(listView);
            Dialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();
        }
    }

    private class Downloader extends AsyncTask<String, Integer, JSONObject> {
        DownloaderHelper helper;

        public Downloader(DownloaderHelper helper) {
            this.helper = helper;
        }

        @Override
        public JSONObject doInBackground(String... urls) {
            try {
                return new JSONObject(downloadUrl(urls[0]));
            } catch (IOException e) {
                Log.e("JSON", "DownloadURL IO error.");
                e.printStackTrace();
            } catch (JSONException e) {
                Log.e("JSON", "DownloadURL JSON error.");
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            helper.parse(result);
        }
    }

    public abstract class DownloaderHelper {
        public abstract void parse(JSONObject jsonObject);
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
// the web page content as a InputStream, which it returns as
// a string.
    public static String downloadUrl(String myUrl) throws IOException {
        InputStream is = null;

        try {
            URL url = new URL(myUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            //Log.d("JSON", "The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string
            return readIt(is);

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    // Reads an InputStream and converts it to a String.
    public static String readIt(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                stream, "iso-8859-1"), 128);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
