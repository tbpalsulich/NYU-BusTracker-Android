package com.palsulich.nyubustracker.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
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
import com.palsulich.nyubustracker.adapters.StopAdapter;
import com.palsulich.nyubustracker.adapters.TimeAdapter;
import com.palsulich.nyubustracker.helpers.BusManager;
import com.palsulich.nyubustracker.helpers.FileGrabber;
import com.palsulich.nyubustracker.models.Bus;
import com.palsulich.nyubustracker.models.Route;
import com.palsulich.nyubustracker.models.Stop;
import com.palsulich.nyubustracker.models.Time;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class MainActivity extends Activity{
    Stop startStop;     // Stop object to keep track of the start location of the desired route.
    Stop endStop;       // Keep track of the desired end location.
    ArrayList<Route> routesBetweenStartAndEnd;        // List of all routes between start and end.
    ArrayList<Time> timesBetweenStartAndEnd;        // List of all times between start and end.
    HashMap<String, Boolean> clickableMapMarkers;   // Hash of all markers which are clickable (so we don't zoom in on buses).
    ArrayList<Marker> busesOnMap = new ArrayList<Marker>();

    Time nextBusTime;

    // mFileGrabber helps to manage cached files/pull new files from the network.
    FileGrabber mFileGrabber;

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
            }
            else haveAMap = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Log.v("General Debugging", "onCreate!");
        setContentView(R.layout.activity_main);

        mFileGrabber = new FileGrabber(getCacheDir());

        renewTimeUntilTimer();       // Creates and starts the timer to refresh time until next bus.

        setUpMapIfNeeded(); // Instantiates mMap, if it needs to be.

        if (haveAMap) mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
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
                ConnectivityManager connMgr = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                Stop.parseJSON(mFileGrabber.getStopJSON(networkInfo));
                Route.parseJSON(mFileGrabber.getRouteJSON(networkInfo));
                BusManager.parseTimes(mFileGrabber.getVersionJSON(networkInfo), mFileGrabber, networkInfo);
                // Ensure we start the app with predefined favorite stops.
                BusManager.syncFavoriteStops(getSharedPreferences(Stop.FAVORITES_PREF, MODE_PRIVATE));
                if (haveAMap) Bus.parseJSON(mFileGrabber.getVehicleJSON(networkInfo));
                if (haveAMap) BusManager.parseSegments(mFileGrabber.getSegmentsJSON(networkInfo));
                if (networkInfo == null || !networkInfo.isConnected()){
                    Context context = getApplicationContext();
                    CharSequence text = "Unable to connect to the network.";
                    int duration = Toast.LENGTH_SHORT;

                    if (context != null){
                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Initialize start and end stops. By default, they are Lafayette and Broadway.
        setStartStop(sharedManager.getStopByName(mFileGrabber.getStartStopFile()));
        setEndStop(sharedManager.getStopByName(mFileGrabber.getEndStopFile()));

        // Update the map to show the corresponding stops, buses, and segments.
        if (routesBetweenStartAndEnd != null) updateMapWithNewStartOrEnd();

        // Get the location of the buses every 10 sec.
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
                        if(startStop != null && endStop != null) setNextBusTime();
                    }
                });
            }
        }, (60 - rightNow.get(Calendar.SECOND)) * 1000, 60000);
    }

    private void renewBusRefreshTimer(){
        if(haveAMap){
            if (busRefreshTimer != null) busRefreshTimer.cancel();

            busRefreshTimer = new Timer();
            busRefreshTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                if (routesBetweenStartAndEnd != null){
                                    ConnectivityManager connMgr = (ConnectivityManager)
                                            getSystemService(Context.CONNECTIVITY_SERVICE);
                                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                                    if (networkInfo == null || !networkInfo.isConnected()){
                                        Context context = getApplicationContext();
                                        CharSequence text = "Unable to connect to the network.";
                                        int duration = Toast.LENGTH_SHORT;

                                        if (context != null){
                                            Toast toast = Toast.makeText(context, text, duration);
                                            toast.show();
                                        }
                                    }
                                    else{
                                        Bus.parseJSON(mFileGrabber.getVehicleJSON(networkInfo));
                                        updateMapWithNewBusLocations();
                                    }
                                }
                            } catch (JSONException e){
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }, 0, 10000);
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
        setNextBusTime();
        renewTimeUntilTimer();
        renewBusRefreshTimer();
        setUpMapIfNeeded();
    }

    public void cacheToAndStartStop() {
        FileGrabber mFileGrabber = new FileGrabber(getCacheDir());
        if (endStop != null) mFileGrabber.setEndStop(endStop.getName());         // Creates or updates cache file.
        if (startStop != null) mFileGrabber.setStartStop(startStop.getName());

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


    // Clear the map of all buses and put them all back on in their new locations.
    private void updateMapWithNewBusLocations(){
        if (haveAMap){
            BusManager sharedManager = BusManager.getBusManager();
            for (Marker m : busesOnMap){
                m.remove();
            }
            busesOnMap = new ArrayList<Marker>();
            if (clickableMapMarkers == null) clickableMapMarkers = new HashMap<String, Boolean>();  // New set of buses means new set of clickable markers!
            for (Route r: routesBetweenStartAndEnd){
                for (Bus b : sharedManager.getBuses()){
                    //Log.v("BusLocations", "bus id: " + b.getID() + ", bus route: " + b.getRoute() + " vs route: " + r.getID());
                    if (b.getRoute().equals(r.getID())){
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


    // Clear the map, because we may have just changed what route we wish to display. Then, add everything back onto the map.
    private void updateMapWithNewStartOrEnd(){
        if (haveAMap){
            setUpMapIfNeeded();
            mMap.clear();
            clickableMapMarkers = new HashMap<String, Boolean>();
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            boolean validBuilder = false;
            for (Route r : routesBetweenStartAndEnd){
                //Log.v("MapDebugging", "Updating map with route: " + r.getLongName());
                for (Stop s : r.getStops()){
                    Marker mMarker = mMap.addMarker(new MarkerOptions()      // Adds a balloon for every stop to the map.
                            .position(s.getLocation())
                            .title(s.getName())
                            .anchor(0.5f, 0.5f)
                            .icon(BitmapDescriptorFactory
                                    .fromBitmap(
                                            BitmapFactory.decodeResource(
                                                    this.getResources(),
                                                    R.drawable.ic_map_stop))));
                    clickableMapMarkers.put(mMarker.getId(), true);
                }
                updateMapWithNewBusLocations();
                // Adds the segments of every Route to the map.
                for (PolylineOptions p : r.getSegments()){
                    //Log.v("MapDebugging", "Trying to add a segment to the map.");
                    if (p != null){
                        for (LatLng loc : p.getPoints()){
                            validBuilder = true;
                            builder.include(loc);
                        }
                        p.color(getResources().getColor(R.color.purple));
                        mMap.addPolyline(p);
                    }
                    //else Log.v("MapDebugging", "Segment was null for " + r.getID());
                }
            }
            if (validBuilder){
                LatLngBounds bounds = builder.build();
                try{
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 30));
                } catch(IllegalStateException e) {      // In case the view is not done being created.
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
            endStop = stop;
            ((Button) findViewById(R.id.to_button)).setText(stop.getName());
            if (startStop != null){
                setNextBusTime();    // Don't set the next bus if we don't have a valid route.
                if (routesBetweenStartAndEnd != null && haveAMap) updateMapWithNewStartOrEnd();
            }
        }
    }

    private void setStartStop(Stop stop) {
        if (endStop != null && endStop == stop) {    // We have an end stop and its name is the same as stopName.
            // Swap the start and end stops.
            Stop temp = startStop;
            startStop = endStop;
            ((Button) findViewById(R.id.from_button)).setText(startStop.getName());
            endStop = temp;
            ((Button) findViewById(R.id.to_button)).setText(endStop.getName());
            setNextBusTime();
            updateMapWithNewStartOrEnd();
        } else {
            //Log.v("Debugging", "setStartStop not swapping");
            // We have a new start. So, we must ensure the end is actually connected.
            if (stop != null){      // Don't set Start to an invalid stop. Should never happen.
                startStop = stop;
                //Log.v("Debugging", "New start stop: " + startStop.getName());
                ((Button) findViewById(R.id.from_button)).setText(stop.getName());
                if (endStop != null){
                    // Loop through all connected Routes.
                    for (Route r : startStop.getRoutes()){
                        // If the current endStop is connected, we don't have to change endStop.
                        if (r.hasStop(endStop.getName())){
                            //Log.v("Debugging", "Found a connected end stop: " + endStop.getName() + " through " + r.getLongName());
                            setNextBusTime();
                            updateMapWithNewStartOrEnd();
                            return;
                        }
                    }
                    ArrayList<Stop> connectedStops = startStop.getRoutes().get(0).getStops();
                    //Log.v("Debugging", "setStartStop picking default endStop: " + connectedStops.get(connectedStops.indexOf(startStop) + 1).getName());
                    // If we did not return above, the current endStop is not connected to the new
                    // startStop. So, by default pick the first connected stop.
                    setEndStop(connectedStops.get(connectedStops.indexOf(startStop) - 1));
                }

            }
        }
    }

    private void setNextBusTime() {
        if (timeUntilTimer != null) timeUntilTimer.cancel();        // Don't want to be interrupted in the middle of this.
        if (busRefreshTimer != null) busRefreshTimer.cancel();
        Calendar rightNow = Calendar.getInstance();
        ArrayList<Route> fromRoutes = startStop.getRoutes();        // All the routes leaving the start stop.
        ArrayList<Route> routes = new ArrayList<Route>();               // All the routes connecting the two.
        for (Route r : fromRoutes) {
            if (r.hasStop(endStop.getName()) && endStop.getTimesOfRoute(r.getLongName()).size() != 0) {
                Log.v("Route Debugging", "Adding a route between " + startStop.getName() + " and " + endStop.getName() + ": " + r.getLongName());
                routes.add(r);
            }
        }
        if (routes.size() > 0) {
            ArrayList<Time> tempTimesBetweenStartAndEnd = new ArrayList<Time>();
            for (Route r : routes) {
                // Get the Times at this stop for this route.
                ArrayList<Time> times;
                if ((times = startStop.getTimesOfRoute(r.getLongName())) != null){
                    tempTimesBetweenStartAndEnd.addAll(times);
                }
            }
            if (tempTimesBetweenStartAndEnd.size() > 0){    // We actually found times.
                timesBetweenStartAndEnd = new ArrayList<Time>(tempTimesBetweenStartAndEnd);
                routesBetweenStartAndEnd = routes;
                Time currentTime = new Time(rightNow.get(Calendar.HOUR_OF_DAY), rightNow.get(Calendar.MINUTE));
                tempTimesBetweenStartAndEnd.add(currentTime);
                Collections.sort(tempTimesBetweenStartAndEnd, Time.compare);
                nextBusTime = tempTimesBetweenStartAndEnd.get(tempTimesBetweenStartAndEnd.indexOf(currentTime) + 1);
                String timeOfNextBus = nextBusTime.toString();
                String timeUntilNextBus = currentTime.getTimeAsStringUntil(nextBusTime);
                ((TextView) findViewById(R.id.times_button)).setText(timeOfNextBus);
                ((TextView) findViewById(R.id.next_bus)).setText(timeUntilNextBus);
            }
            else{
                Context context = getApplicationContext();
                CharSequence text = "No available times.";
                int duration = Toast.LENGTH_LONG;

                if (context != null){
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
            }
        } else
        {
            if (busRefreshTimer != null) busRefreshTimer.cancel();
            if (timeUntilTimer != null) timeUntilTimer.cancel();

            Context context = getApplicationContext();
            CharSequence text = "No routes available!";
            int duration = Toast.LENGTH_LONG;
            
            if (context != null){
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        }

        renewBusRefreshTimer();
        renewTimeUntilTimer();
    }

    CompoundButton.OnCheckedChangeListener cbListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
            Stop s = (Stop) buttonView.getTag();
            s.setFavorite(isChecked);
            getSharedPreferences(Stop.FAVORITES_PREF, MODE_PRIVATE)
                    .edit().putBoolean(s.getID(), isChecked)
                    .commit();
            Log.v("Dialog", "Checkbox is " + isChecked);
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
        StopAdapter adapter = new StopAdapter(getApplicationContext(), connectedStops, dialog,
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
        StopAdapter adapter = new StopAdapter(getApplicationContext(), stops, dialog,
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
        // Library provided ListView with headers that (gasp) stick to the top.
        StickyListHeadersListView listView = new StickyListHeadersListView(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        TimeAdapter adapter = new TimeAdapter(getApplicationContext(), timesBetweenStartAndEnd);
        listView.setAdapter(adapter);
        // listView.setSelection(timesBetweenStartAndEnd.indexOf(nextBusTime));
        builder.setView(listView);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }
}
