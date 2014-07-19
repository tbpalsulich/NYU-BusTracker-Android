package com.palsulich.nyubustracker.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.android.gms.analytics.GoogleAnalytics;
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
import com.palsulich.nyubustracker.NYUBusTrackerApplication;
import com.palsulich.nyubustracker.R;
import com.palsulich.nyubustracker.adapters.StopAdapter;
import com.palsulich.nyubustracker.adapters.TimeAdapter;
import com.palsulich.nyubustracker.helpers.BusManager;
import com.palsulich.nyubustracker.helpers.MultipleOrientationSlidingDrawer;
import com.palsulich.nyubustracker.models.Bus;
import com.palsulich.nyubustracker.models.Route;
import com.palsulich.nyubustracker.models.Stop;
import com.palsulich.nyubustracker.models.Time;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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
    public static final boolean LOCAL_LOGV = true;
    private static final String QUERY = makeQuery("agencies", "72", "UTF-8");
    private static final String TRANSLOC_URL = "https://transloc-api-1-2.p.mashape.com";
    private static final String STOPS_URL = TRANSLOC_URL + "/stops.json?" + QUERY;
    private static final String ROUTES_URL = TRANSLOC_URL + "/routes.json?" + QUERY;
    private static final String SEGMENTS_URL = TRANSLOC_URL + "/segments.json?" + QUERY;
    private static final String VEHICLES_URL = TRANSLOC_URL + "/vehicles.json?" + QUERY;
    private static final String VERSION_URL = "https://s3.amazonaws.com/nyubustimes/1.0/version.json";
    private static final String RUN_ONCE_PREF = "runOnce";
    private static final String STOP_PREF = "stops";
    private static final String START_STOP_PREF = "startStop";
    private static final String END_STOP_PREF = "endStop";
    private static final String TIME_VERSION_PREF = "stopVersions";
    private static final String FIRST_TIME = "firstTime";
    private static final String STOP_JSON_FILE = "stopJson";
    private static final String CREATED_FILES_DIR = "NYUCachedFiles";
    private final DownloaderHelper stopDownloaderHelper = new DownloaderHelper() {
        @Override
        public void parse(JSONObject jsonObject) throws JSONException, IOException {
            Stop.parseJSON(jsonObject);
            File path = new File(getFilesDir(), CREATED_FILES_DIR);
            if (path.mkdir()) {
                File file = new File(path, STOP_JSON_FILE);
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                bufferedWriter.write(jsonObject.toString());
                bufferedWriter.close();
            }
        }
    };
    private static final String ROUTE_JSON_FILE = "routeJson";
    private final DownloaderHelper routeDownloaderHelper = new DownloaderHelper() {
        @Override
        public void parse(JSONObject jsonObject) throws JSONException, IOException {
            Route.parseJSON(jsonObject);
            File path = new File(getFilesDir(), CREATED_FILES_DIR);
            if (path.mkdir()) {
                File file = new File(path, ROUTE_JSON_FILE);
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                bufferedWriter.write(jsonObject.toString());
                bufferedWriter.close();
            }
        }
    };
    private static final String SEGMENT_JSON_FILE = "segmentJson";
    private final DownloaderHelper segmentDownloaderHelper = new DownloaderHelper() {
        @Override
        public void parse(JSONObject jsonObject) throws JSONException, IOException {
            BusManager.parseSegments(jsonObject);
            File path = new File(getFilesDir(), CREATED_FILES_DIR);
            if (path.mkdir()) {
                File file = new File(path, SEGMENT_JSON_FILE);
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                bufferedWriter.write(jsonObject.toString());
                bufferedWriter.close();
            }
        }
    };
    private static final String VERSION_JSON_FILE = "versionJson";
    public static final String REFACTOR_LOG_TAG = "refactor";
    private final DownloaderHelper versionDownloaderHelper = new DownloaderHelper() {
        @Override
        public void parse(JSONObject jsonObject) throws JSONException, IOException {
            BusManager sharedManager = BusManager.getBusManager();
            BusManager.parseVersion(jsonObject);
            for (String timeURL : sharedManager.getTimesToDownload()) {
                SharedPreferences preferences = getSharedPreferences(TIME_VERSION_PREF, MODE_PRIVATE);
                String stopID = timeURL.substring(timeURL.lastIndexOf("/") + 1, timeURL.indexOf(".json"));
                if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "Time to download: " + stopID);
                int newestStopTimeVersion = sharedManager.getTimesVersions().get(stopID);
                if (preferences.getInt(stopID, 0) != newestStopTimeVersion) {
                    if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "*   Actually downloading it!");
                    new Downloader(timeDownloaderHelper).execute(timeURL);
                    preferences.edit().putInt(stopID, newestStopTimeVersion).commit();
                }
                else if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "*   Not actually downloading it, because we already have the current version.");
            }
        }
    };
    private final DownloaderHelper timeDownloaderHelper = new DownloaderHelper() {
        @Override
        public void parse(JSONObject jsonObject) throws JSONException, IOException {
            BusManager.parseTime(jsonObject);
            if (jsonObject != null && jsonObject.length() > 0) {
                if (LOCAL_LOGV)
                    Log.v(REFACTOR_LOG_TAG, "Creating time cache file: " + jsonObject.getString("stop_id"));

                File path = new File(getFilesDir(), CREATED_FILES_DIR);
                if (path.mkdir()) {
                    File file = new File(path, jsonObject.getString("stop_id"));
                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                    bufferedWriter.write(jsonObject.toString());
                    bufferedWriter.close();
                }
            }
        }
    };
    private final CompoundButton.OnCheckedChangeListener cbListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Stop s = (Stop) buttonView.getTag();
            s.setFavorite(isChecked);
            getSharedPreferences(Stop.FAVORITES_PREF, MODE_PRIVATE).edit().putBoolean(s.getID(), isChecked).commit();
        }
    };
    private final DownloaderHelper busDownloaderHelper = new DownloaderHelper() {
        @Override
        public void parse(JSONObject jsonObject) throws JSONException, IOException {
            Bus.parseJSON(jsonObject);
            updateMapWithNewBusLocations();
        }
    };
    ArrayList<Time> timesBetweenStartAndEnd;        // List of all times between start and end.
    Time nextBusTime;
    ProgressDialog progressDialog;
    SharedPreferences oncePreferences;
    LocationManager mLocationManager;
    double onStartTime;
    private Stop startStop;     // Stop object to keep track of the start location of the desired route.
    private Stop endStop;       // Keep track of the desired end location.
    private ArrayList<Route> routesBetweenStartAndEnd;        // List of all routes between start and end.
    private HashMap<String, Boolean> clickableMapMarkers;   // Hash of all markers which are clickable (so we don't zoom in on buses).
    private ArrayList<Marker> busesOnMap = new ArrayList<Marker>();
    private TextSwitcher mSwitcher;
    private String mSwitcherCurrentText;
    private TimeAdapter timesAdapter;
    private StickyListHeadersListView timesList;
    private Timer timeUntilTimer;  // Timer used to refresh the "time until next bus" every minute, on the minute.
    private Timer busRefreshTimer; // Timer used to refresh the bus locations every few seconds.
    private GoogleMap mMap;     // Map to display all stops, segments, and buses.
    private boolean haveAMap = false;   // Flag to see if the device can display a map.
    private AsyncTask stopDownloader;
    private AsyncTask routeDownloader;
    private AsyncTask segmentDownloader;
    private AsyncTask versionDownloader;
    private boolean offline = true;
    private int downloadsOnTheWire = 0;

    private static String makeQuery(String param, String value, String charset) {
        try {
            return String.format(param + "=" + URLEncoder.encode(value, charset));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

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
                mMap.setMyLocationEnabled(true);
                haveAMap = true;
            }
            else haveAMap = false;
        }
    }

    String readSavedData(String fileName) {
        if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "Reading saved data from " + fileName);
        StringBuilder buffer = new StringBuilder("");
        try {
            File path = new File(getFilesDir(), CREATED_FILES_DIR);
            if (path.mkdir()) {
                File file = new File(path, fileName);
                FileInputStream inputStream = new FileInputStream(file);
                InputStreamReader streamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(streamReader);

                String readString = bufferedReader.readLine();
                while (readString != null) {
                    buffer.append(readString);
                    readString = bufferedReader.readLine();
                }

                inputStream.close();
            }
        } catch (IOException e) {
            if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "Failed to read " + fileName + "...");
            e.printStackTrace();
        }
        return buffer.toString();
    }

    private void downloadEverything() {
        deleteEverythingInMemory();
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            offline = false;
            // Download and parse everything, put it all in persistent memory, continue.
            progressDialog = ProgressDialog.show(this, getString(R.string.downloading), getString(R.string.wait), true, false);

            stopDownloader = new Downloader(stopDownloaderHelper).execute(STOPS_URL);
            routeDownloader = new Downloader(routeDownloaderHelper).execute(ROUTES_URL);
            segmentDownloader = new Downloader(segmentDownloaderHelper).execute(SEGMENTS_URL);
            versionDownloader = new Downloader(versionDownloaderHelper).execute(VERSION_URL);
        }
        else if (!offline) {    // Only show the offline dialog once.
            offline = true;
            Context context = getApplicationContext();
            CharSequence text = getString(R.string.unable_to_connect);
            int duration = Toast.LENGTH_SHORT;

            if (context != null) {
                Toast.makeText(context, text, duration).show();
            }
        }
    }

    private void pieceDownloadsTogether() {
        if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "Downloads on the wire: " + downloadsOnTheWire);
        if (downloadsOnTheWire == 0) {
            if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "Downloading finished!");
            oncePreferences.edit().putBoolean(FIRST_TIME, false).commit();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "Number of stops: " + BusManager.getBusManager().numStops());
                    Stop broadway = BusManager.getBusManager().getStopByName("715 Broadway @ Washington Square");
                    if (LOCAL_LOGV)
                        Log.v(REFACTOR_LOG_TAG, "has stops? " + (BusManager.getBusManager().getConnectedStops(broadway).size() > 0));
                    ArrayList<Stop> connectedStops = BusManager.getBusManager().getConnectedStops(broadway);
                    if (connectedStops.size() > 0) {
                        Stop nextStop = BusManager.getBusManager().getConnectedStops(broadway).get(0);
                        getSharedPreferences(Stop.FAVORITES_PREF, MODE_PRIVATE).edit().putBoolean(broadway.getID(), true).commit();
                        setStartStop(broadway);
                        setEndStop(nextStop);
                        broadway.setFavorite(true);
                        if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "End: " + endStop.getName());
                        // Update the map to show the corresponding stops, buses, and segments.
                        if (routesBetweenStartAndEnd != null) updateMapWithNewStartOrEnd();
                        renewBusRefreshTimer();
                        renewTimeUntilTimer();
                        setNextBusTime();
                    }
                    progressDialog.dismiss();
                }
            });
        }
        // Else, we have nothing to do, since not all downloads are finished.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "onCreate!");
        setContentView(R.layout.activity_main);

        ((NYUBusTrackerApplication) getApplication()).getTracker();

        oncePreferences = getSharedPreferences(RUN_ONCE_PREF, MODE_PRIVATE);

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

        mSwitcher = (TextSwitcher) findViewById(R.id.next_time);
        mSwitcherCurrentText = "";

        // Set the ViewFactory of the TextSwitcher that will create TextView object when asked
        mSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            public View makeView() {
                TextView myText = new TextView(MainActivity.this);
                myText.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.time_until_text_size));
                myText.setTextColor(getResources().getColor(R.color.main_text));
                myText.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                myText.setEllipsize(TextUtils.TruncateAt.END);
                myText.setSingleLine(true);
                return myText;
            }
        });

        // Declare the in and out animations and initialize them
        Animation out = AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.slide_in_left);
        Animation in = AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.slide_out_right);

        // set the animation type of textSwitcher
        mSwitcher.setInAnimation(in);
        mSwitcher.setOutAnimation(out);

        timesList = (StickyListHeadersListView) findViewById(R.id.times_list);
        timesAdapter = new TimeAdapter(this, new ArrayList<Time>());
        timesList.setAdapter(timesAdapter);

        if (oncePreferences.getBoolean(FIRST_TIME, true)) {
            if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "Downloading because of first time");
            downloadEverything();
        }
        else {
            if (!sharedManager.hasRoutes() || !sharedManager.hasStops()) {
                if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "Parsing cached files...");
                try {
                    JSONObject stopJson = new JSONObject(readSavedData(STOP_JSON_FILE));
                    JSONObject routeJson = new JSONObject(readSavedData(ROUTE_JSON_FILE));
                    JSONObject segJson = new JSONObject(readSavedData(SEGMENT_JSON_FILE));
                    JSONObject verJson = new JSONObject(readSavedData(VERSION_JSON_FILE));
                    Stop.parseJSON(stopJson);
                    Route.parseJSON(routeJson);
                    BusManager.parseSegments(segJson);
                    BusManager.parseVersion(verJson);
                    for (String timeURL : sharedManager.getTimesToDownload()) {
                        String timeFileName = timeURL.substring(timeURL.lastIndexOf("/") + 1, timeURL.indexOf(".json"));
                        if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "Trying to parse " + timeFileName);
                        try {
                            BusManager.parseTime(new JSONObject(readSavedData(timeFileName)));
                        } catch (JSONException e) {
                            if (LOCAL_LOGV)
                                Log.v(REFACTOR_LOG_TAG, "Didn't find time file, so downloading it: " + timeURL);
                            new Downloader(timeDownloaderHelper).execute(timeURL);
                        }
                    }
                    SharedPreferences favoritePreferences = getSharedPreferences(Stop.FAVORITES_PREF, MODE_PRIVATE);
                    if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "Done parsing...");
                    for (Stop s : sharedManager.getStops()) {
                        boolean result = favoritePreferences.getBoolean(s.getID(), false);
                        if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, s.getName() + " is " + result);
                        s.setFavorite(result);
                    }
                    new Downloader(versionDownloaderHelper).execute(VERSION_URL);
                    setStartAndEndStops();

                    // Update the map to show the corresponding stops, buses, and segments.
                    if (routesBetweenStartAndEnd != null) updateMapWithNewStartOrEnd();

                    // Get the location of the buses every 10 sec.
                    renewBusRefreshTimer();
                    renewTimeUntilTimer();
                    setNextBusTime();
                } catch (JSONException e) {
                    if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "Re-downloading because of an error.");
                    e.printStackTrace();
                    downloadEverything();
                }
            }
            else {
                setStartAndEndStops();
                updateMapWithNewStartOrEnd();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        //        if (LOCAL_LOGV) Log.v("General Debugging", "onStart!");
        onStartTime = System.currentTimeMillis();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "onResume!");
        if (endStop != null && startStop != null) {
            renewTimeUntilTimer();
            renewBusRefreshTimer();
            setUpMapIfNeeded();
            setStartAndEndStops();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "onPause!");
        cacheStartAndEndStops();
        if (timeUntilTimer != null) timeUntilTimer.cancel();
        if (busRefreshTimer != null) busRefreshTimer.cancel();
    }

    @Override
    public void onStop() {
        super.onStop();
        //        if (LOCAL_LOGV) Log.v("General Debugging", "onStop!");
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "onDestroy!");
        cacheStartAndEndStops();      // Remember user's preferences across lifetimes.
        if (timeUntilTimer != null)
            timeUntilTimer.cancel();           // Don't need a timer anymore -- must be recreated onResume.
        if (busRefreshTimer != null) busRefreshTimer.cancel();
    }

    @Override
    public void onBackPressed() {
        MultipleOrientationSlidingDrawer drawer = (MultipleOrientationSlidingDrawer) findViewById(R.id.sliding_drawer);
        if (drawer.isOpened()) drawer.animateClose();
        else super.onBackPressed();
    }

    void cacheStartAndEndStops() {
        if (endStop != null)
            getSharedPreferences(STOP_PREF, MODE_PRIVATE).edit().putString(END_STOP_PREF, endStop.getName()).commit();         // Creates or updates cache file.
        if (startStop != null)
            getSharedPreferences(STOP_PREF, MODE_PRIVATE).edit().putString(START_STOP_PREF, startStop.getName()).commit();
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
                            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                            if (networkInfo != null && networkInfo.isConnected()) {
                                offline = false;
                                new Downloader(busDownloaderHelper).execute(VEHICLES_URL);
                            }
                            else if (!offline) {
                                offline = true;
                                Context context = getApplicationContext();
                                CharSequence text = getString(R.string.unable_to_connect);
                                int duration = Toast.LENGTH_SHORT;

                                if (context != null) {
                                    Toast.makeText(context, text, duration).show();
                                }
                            }
                        }
                    });
                }
            }, 0, 1500L);
        }
    }

    /*
    Returns the best location we can, checking every available location provider.
    If no provider is available (e.g. all location services turned off), this will return null.
     */
    public Location getLocation() {
        Location bestLocation = null;
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        for (String provider : mLocationManager.getProviders(true)) {
            Location l = mLocationManager.getLastKnownLocation(provider);
            //            Log.d(REFACTOR_LOG_TAG, "time of location " + l.getAccuracy() + " is " + (System.currentTimeMillis() - l.getTime()));
            if (l != null && (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) && (System.currentTimeMillis() - l.getTime()) < 120000) {
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    void setStartAndEndStops() {
        String end = getSharedPreferences(STOP_PREF, MODE_PRIVATE).getString(END_STOP_PREF, "80 Lafayette St");         // Creates or updates cache file.
        String start = getSharedPreferences(STOP_PREF, MODE_PRIVATE).getString(START_STOP_PREF, "715 Broadway @ Washington Square");
        if (startStop == null) setStartStop(BusManager.getBusManager().getStopByName(start));
        if (endStop == null) setEndStop(BusManager.getBusManager().getStopByName(end));
        Location l = getLocation();
        if (l != null && System.currentTimeMillis() - onStartTime < 1000) {
            Location startLoc = new Location(""), endLoc = new Location("");
            startLoc.setLatitude(startStop.getLocation().latitude);
            startLoc.setLongitude(startStop.getLocation().longitude);
            endLoc.setLatitude(endStop.getLocation().latitude);
            endLoc.setLongitude(endStop.getLocation().longitude);
            //            Log.d(REFACTOR_LOG_TAG, "start: " + startStop);
            //            Log.d(REFACTOR_LOG_TAG, "end: " + endStop);
            //            Log.d(REFACTOR_LOG_TAG, "s dist: " + l.distanceTo(startLoc));
            //            Log.d(REFACTOR_LOG_TAG, "e dist: " + l.distanceTo(endLoc));
            if (l.distanceTo(startLoc) > l.distanceTo(endLoc)) {
                setStartStop(endStop);
            }
            //            Log.d(REFACTOR_LOG_TAG, "Location: " + l.getLatitude() + ", " + l.getLongitude());
        }
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
            boolean somethingActive = false;    // Used to make sure we put at least one set of segments on the map.
            for (Route r : routesBetweenStartAndEnd) {
                somethingActive = somethingActive || r.isActive(startStop);
            }
            for (Route r : routesBetweenStartAndEnd) {
                if (r.isActive(startStop) || !somethingActive) {
                    somethingActive = true;
                    for (Bus b : sharedManager.getBuses()) {
                        //if (LOCAL_LOGV) Log.v("BusLocations", "bus id: " + b.getID() + ", bus route: " + b.getRoute() + " vs route: " + r.getID());
                        if (b.getRoute().equals(r.getID())) {
                            Marker mMarker = mMap.addMarker(new MarkerOptions().position(b.getLocation()).icon(BitmapDescriptorFactory.fromBitmap(rotateBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_bus_arrow), b.getHeading()))).anchor(0.5f, 0.5f));
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
            setUpMapIfNeeded();
            mMap.clear();
            clickableMapMarkers = new HashMap<String, Boolean>();
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            boolean validBuilder = false;
            boolean somethingActive = false;    // Used to make sure we put at least one set of segments on the map.
            for (Route r : routesBetweenStartAndEnd) {
                somethingActive = somethingActive || r.isActive(startStop);
            }
            for (Route r : routesBetweenStartAndEnd) {
                if (r.isActive(startStop) || !somethingActive) {
                    somethingActive = true;
                    //if (LOCAL_LOGV) Log.v("MapDebugging", "Updating map with route: " + r.getLongName());
                    for (Stop s : r.getStops()) {
                        for (Stop f : s.getFamily()) {
                            if ((!f.isHidden() && !f.isRelatedTo(startStop) && !f.isRelatedTo(endStop)) || (f == startStop || f == endStop)) {
                                // Only put one representative from a family of stops on the p
                                //if (LOCAL_LOGV) Log.v("MapDebugging", "Not hiding " + f);
                                Marker mMarker = mMap.addMarker(new MarkerOptions()      // Adds a balloon for every stop to the map.
                                                                    .position(f.getLocation()).title(f.getName()).anchor(0.5f, 0.5f).icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_map_stop))));
                                clickableMapMarkers.put(mMarker.getId(), true);
                            }
                        }
                    }
                    updateMapWithNewBusLocations();
                    // Adds the segments of every Route to the map.
                    for (PolylineOptions p : r.getSegments()) {
                        //if (LOCAL_LOGV) Log.v("MapDebugging", "Trying to add a segment to the map");
                        if (p != null) {
                            for (LatLng loc : p.getPoints()) {
                                validBuilder = true;
                                builder.include(loc);
                            }
                            p.color(getResources().getColor(R.color.main_buttons));
                            mMap.addPolyline(p);
                            //if (LOCAL_LOGV) Log.v("MapDebugging", "Success!");
                        }
                        //else if (LOCAL_LOGV) Log.v("MapDebugging", "Segment was null for " + r.getID());
                    }
                }
            }
            if (validBuilder) {
                LatLngBounds bounds = builder.build();
                try {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 30));
                } catch (IllegalStateException e) {      // In case the view is not done being created.
                    //e.printStackTrace();
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, this.getResources().getDisplayMetrics().widthPixels, this.getResources().getDisplayMetrics().heightPixels, 100));
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
            if (routes.size() > 0 && stop != startStop) {
                endStop = stop;
                ((TextView) findViewById(R.id.end_stop)).setText(stop.getUltimateName());
                if (startStop != null) {
                    setNextBusTime();    // Don't set the next bus if we don't have a valid route.
                    if (routesBetweenStartAndEnd != null && haveAMap) updateMapWithNewStartOrEnd();
                }
            }
            else {
                ArrayList<Stop> connected = BusManager.getBusManager().getConnectedStops(startStop);
                int stopIndex = 0;
                while (connected.size() > stopIndex && !checkStop(connected.get(stopIndex))) {
                    stopIndex++;
                }
                if (stopIndex < connected.size()) setEndStop(connected.get(stopIndex));
                else downloadEverything();
            }
        }
    }

    private boolean checkStop(Stop stop) {
        if (stop != null) {     // Make sure we actually have a stop!
            // Check there is a route between these stops.
            ArrayList<Route> routes = new ArrayList<Route>();               // All the routes connecting the two.
            for (Route r : startStop.getRoutes()) {
                if (r.hasStop(stop)) {
                    routes.add(r);
                }
            }
            if (routes.size() > 0 && stop != startStop) {
                return true;
            }
        }
        return false;
    }

    private void setStartStop(Stop stop) {
        if (stop != null) {
            if (endStop == stop) {    // We have an end stop and its name is the same as stopName.
                // Swap the start and end stops.
                Stop temp = startStop;
                startStop = endStop;
                ((TextView) findViewById(R.id.start_stop)).setText(startStop.getUltimateName());
                setEndStop(temp);
            }
            else { // We have a new start. So, we must ensure the end is actually connected. If not, pick a random connected stop.
                startStop = stop;
                ((TextView) findViewById(R.id.start_stop)).setText(stop.getUltimateName());
                if (endStop != null) {
                    // Loop through all connected Routes.
                    for (Route r : startStop.getRoutes()) {
                        if (r.hasStop(endStop) && startStop.getTimesOfRoute(r.getLongName()).size() > 0) {  // If the current endStop is connected, we don't have to change endStop.
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
            //if (LOCAL_LOGV) Log.v("Routes", "Start Route: " + r);
            if (endRoutes.contains(r) && !availableRoutes.contains(r)) {
                //if (LOCAL_LOGV) Log.v("Greenwich", "*  " + r + " is available.");
                availableRoutes.add(r);
            }
        }
        BusManager sharedManager = BusManager.getBusManager();
        int bestDistance = sharedManager.distanceBetween(startStop, endStop);

        int testDistance = sharedManager.distanceBetween(startStop.getOppositeStop(), endStop.getOppositeStop());
        if (testDistance < bestDistance) {
            startStop = startStop.getOppositeStop();
            endStop = endStop.getOppositeStop();
        }

        testDistance = sharedManager.distanceBetween(startStop, endStop.getOppositeStop());
        if (testDistance < bestDistance) {
            endStop = endStop.getOppositeStop();
        }

        testDistance = sharedManager.distanceBetween(startStop.getOppositeStop(), endStop);
        if (testDistance < bestDistance) {
            startStop = startStop.getOppositeStop();
        }

        if (availableRoutes.size() > 0) {
            //Log.d("Greenwich", "Have a route available from " + startStop + " to " + endStop);
            ArrayList<Time> tempTimesBetweenStartAndEnd = new ArrayList<Time>();
            for (Route r : availableRoutes) {
                //Log.d("Greenwich", "  " + r + " is available");
                //if (!startStop.getOtherRoute().isEmpty()) Log.d("Greenwich", "  " + startStop.getOtherRoute() + " is available too!");
                //if (!r.getOtherLongName().isEmpty()) Log.d("Greenwich", "  " + r.getOtherLongName() + " is available too!");
                // Get the Times at this stop for this route.
                ArrayList<Time> times = startStop.getTimesOfRoute(r.getLongName());
                ArrayList<Time> otherTimes = startStop.getTimesOfRoute(r.getOtherLongName());
                //Log.d("Greenwich", "  has " + times.size() + " times ");
                //Log.d("Greenwich", "  has " + otherTimes.size() + " other times.");

                if (otherTimes.size() > 0 && endStop.getTimesOfRoute(r.getOtherLongName()).size() > 0) {
                    for (Time t : otherTimes) {
                        if (!tempTimesBetweenStartAndEnd.contains(t)) {
                            tempTimesBetweenStartAndEnd.add(t);
                        }
                    }
                }
                else if (times.size() > 0) {
                    for (Time t : times) {
                        if (!tempTimesBetweenStartAndEnd.contains(t)) {
                            tempTimesBetweenStartAndEnd.add(t);
                        }
                    }
                }
            }
            if (tempTimesBetweenStartAndEnd.size() > 0) {    // We actually found times.
                // Here, we grab the list of all times of all routes between the start and end, add in the current
                // time, then sort that list of times. That way, we know the first bus Time after the current time
                // is the Time of the soonest next Bus.
                timesBetweenStartAndEnd = new ArrayList<Time>(tempTimesBetweenStartAndEnd);
                routesBetweenStartAndEnd = availableRoutes;
                final Time currentTime = new Time(rightNow.get(Calendar.HOUR_OF_DAY), rightNow.get(Calendar.MINUTE));
                tempTimesBetweenStartAndEnd.add(currentTime);
                Collections.sort(tempTimesBetweenStartAndEnd, Time.compare);
                Collections.sort(timesBetweenStartAndEnd, Time.compare);

                int index = tempTimesBetweenStartAndEnd.indexOf(currentTime);
                nextBusTime = tempTimesBetweenStartAndEnd.get((index + 1) % tempTimesBetweenStartAndEnd.size());

                MultipleOrientationSlidingDrawer drawer = (MultipleOrientationSlidingDrawer) findViewById(R.id.sliding_drawer);
                final String newSwitcherText = currentTime.getTimeAsStringUntil(nextBusTime, getResources());
                if (!drawer.isMoving() && !mSwitcherCurrentText.equals(newSwitcherText)) {
                    mSwitcher.setText(newSwitcherText);  // Pass resources so we return the proper string value.
                    mSwitcherCurrentText = newSwitcherText;
                }
                // Handle a bug where the time until text disappears when the drawer is being moved. So, just wait for it to finish.
                // We don't know if the drawer will end up open or closed, though. So handle both cases.
                else if (!mSwitcherCurrentText.equals(newSwitcherText)) {
                    drawer.setOnDrawerCloseListener(new MultipleOrientationSlidingDrawer.OnDrawerCloseListener() {
                        @Override
                        public void onDrawerClosed() {
                            mSwitcher.setText(newSwitcherText);
                            mSwitcherCurrentText = newSwitcherText;
                        }
                    });
                    drawer.setOnDrawerOpenListener(new MultipleOrientationSlidingDrawer.OnDrawerOpenListener() {
                        @Override
                        public void onDrawerOpened() {
                            mSwitcher.setText(newSwitcherText);
                            mSwitcherCurrentText = newSwitcherText;
                        }
                    });
                }

                timesAdapter.setDataSet(timesBetweenStartAndEnd);
                timesAdapter.notifyDataSetChanged();

                final int nextTimeIndex = timesBetweenStartAndEnd.indexOf(nextBusTime);
                timesList.clearFocus();
                timesList.post(new Runnable() {
                    @Override
                    public void run() {
                        timesList.setSelection(nextTimeIndex);
                    }
                });
                timesAdapter.setTime(currentTime);

                if (BusManager.getBusManager().isOnline()) {
                    String routeText;
                    String[] routeArray = nextBusTime.getRoute().split("\\s");
                    String route = nextBusTime.getRoute();
                    if (routeArray[0].length() == 1) {
                        routeText = getString(R.string.route) + route;
                    }
                    else {
                        routeText = route;
                    }
                    ((TextView) findViewById(R.id.next_route)).setText(getString(R.string.via) + routeText);
                    ((TextView) findViewById(R.id.next_bus)).setText(getString(R.string.next_bus_in));
                    findViewById(R.id.safe_ride_button).setVisibility(View.GONE);
                }
                else {
                    ((TextView) findViewById(R.id.next_route)).setText("");
                    ((TextView) findViewById(R.id.next_bus)).setText("");
                    if (rightNow.get(Calendar.HOUR_OF_DAY) < 7) {
                        findViewById(R.id.safe_ride_button).setVisibility(View.VISIBLE);
                    }
                    else {
                        findViewById(R.id.safe_ride_button).setVisibility(View.GONE);
                    }
                }
                updateMapWithNewStartOrEnd();
            }
            else {
                setEndStop(startStop);
            }
        }
        renewBusRefreshTimer();
        renewTimeUntilTimer();
    }

    @SuppressWarnings("UnusedParameters")
    public void callSafeRide(View view) {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:12129928267"));
        startActivity(callIntent);
    }

    @SuppressWarnings("UnusedParameters")
    public void createEndDialog(View view) {
        // Get all stops connected to the start stop.
        final ArrayList<Stop> connectedStops = BusManager.getBusManager().getConnectedStops(startStop);
        if (connectedStops.size() > 0) {
            ListView listView = new ListView(this);     // ListView to populate the dialog.
            listView.setId(R.id.end_stop_list);
            listView.setDivider(new ColorDrawable(getResources().getColor(R.color.time_list_background)));
            listView.setDividerHeight(2);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);    // Used to build the dialog with the list of connected Stops.
            builder.setView(listView);
            final Dialog dialog = builder.create();
            // An adapter takes some data, then adapts it to fit into a view. The adapter supplies the individual view elements of
            // the list view. So, in this case, we supply the StopAdapter with a list of stops, and it gives us back the nice
            // views with a heart button to signify favorites and a TextView with the name of the stop.
            // We provide the onClickListeners to the adapter, which then attaches them to the respective views.
            StopAdapter adapter = new StopAdapter(getApplicationContext(), connectedStops, new View.OnClickListener() {
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
        else {
            displayStopError();
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void createStartDialog(View view) {
        final ArrayList<Stop> stops = BusManager.getBusManager().getStops();    // Show every stop as an option to start.
        if (stops.size() > 0) {
            ListView listView = new ListView(this);
            listView.setId(R.id.start_stop);
            listView.setDivider(new ColorDrawable(getResources().getColor(R.color.time_list_background)));
            listView.setDividerHeight(1);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(listView);
            final Dialog dialog = builder.create();
            StopAdapter adapter = new StopAdapter(getApplicationContext(), stops, new View.OnClickListener() {
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
        else {
            displayStopError();
        }
    }

    public void displayStopError() {
        Context context = getApplicationContext();
        CharSequence text = getString(R.string.no_stops_available);
        int duration = Toast.LENGTH_LONG;

        if (context != null) {
            Toast.makeText(context, text, duration).show();
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void createInfoDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LinearLayout linearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.information_layout, null);
        builder.setView(linearLayout);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    @SuppressWarnings("UnusedParameters")
    public void goToGitHub(View view) {
        String url = "https://github.com/tpalsulich/NYU-BusTracker-Android";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    @SuppressWarnings("UnusedParameters")
    public void goToWebsite(View view) {
        String url = "http://www.nyubustracker.com";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // a string.
    private String downloadUrl(String myUrl) throws IOException {
        InputStream is = null;

        try {
            URL url = new URL(myUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setRequestProperty("X-Mashape-Authorization", "0gpwrDtINCQRxnhWEyJpEgdfYdQjZYSp");
            // Starts the QUERY
            conn.connect();
            //int response = conn.getResponseCode();
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

    private void deleteEverythingInMemory() {
        if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "Trying to delete all files.");
        File directory = new File(getFilesDir(), CREATED_FILES_DIR);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.delete()) {
                    if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "Deleted " + f.toString());
                }
                else if (LOCAL_LOGV) Log.v(REFACTOR_LOG_TAG, "Could not delete " + f.toString());
            }
        }
    }

    // Reads an InputStream and converts it to a String.
    private String readIt(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "iso-8859-1"), 128);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private class Downloader extends AsyncTask<String, Void, JSONObject> {
        final DownloaderHelper helper;

        public Downloader(DownloaderHelper helper) {
            this.helper = helper;
        }

        @Override
        public void onPreExecute() {
            downloadsOnTheWire++;
        }

        @Override
        public JSONObject doInBackground(String... urls) {
            try {
                return new JSONObject(downloadUrl(urls[0]));
            } catch (IOException e) {
                //Log.e("JSON", "DownloadURL IO error.");
                e.printStackTrace();
            } catch (JSONException e) {
                //Log.e("JSON", "DownloadURL JSON error.");
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            try {
                helper.parse(result);
                downloadsOnTheWire--;
                pieceDownloadsTogether();
            } catch (JSONException e) {
                Log.d(REFACTOR_LOG_TAG, "JSON Exception while parsing in onPostExecute.");
                e.printStackTrace();
            } catch (IOException e) {
                Log.d(REFACTOR_LOG_TAG, "IO Exception while parsing in onPostExecute.");
            }
        }
    }

    public abstract class DownloaderHelper {
        public abstract void parse(JSONObject jsonObject) throws JSONException, IOException;
    }
}
