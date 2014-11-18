package com.nyubustracker.models;

import android.util.Log;

import com.google.android.gms.maps.model.PolylineOptions;
import com.nyubustracker.activities.MainActivity;
import com.nyubustracker.helpers.BusManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Route {
    private final BusManager sharedManager;
    private final ArrayList<String> segmentIDs;
    private final ArrayList<PolylineOptions> segments;
    private String longName = "";
    private String otherLongName = ""; // Useful for Route B Greenwich times.
    private String routeID = "";
    private ArrayList<Stop> stops = null;

    public Route(String mLongName, String mRouteID) {
        segmentIDs = new ArrayList<String>();
        segments = new ArrayList<PolylineOptions>();
        longName = mLongName;
        routeID = mRouteID;
        sharedManager = BusManager.getBusManager();
        stops = sharedManager.getStopsByRouteID(routeID);
        for (Stop s : stops) {
            s.addRoute(this);
        }
        //if (MainActivity.LOCAL_LOGV) Log.v("Debugging", longName + "'s number of stops:" + stops.size());
    }

    public static void parseJSON(JSONObject routesJson) throws JSONException {
        JSONArray jRoutes = new JSONArray();
        BusManager sharedManager = BusManager.getBusManager();
        if (routesJson != null)
            jRoutes = routesJson.getJSONObject(BusManager.TAG_DATA).getJSONArray("72");
        for (int j = 0; j < jRoutes.length(); j++) {
            JSONObject routeObject = jRoutes.getJSONObject(j);
            String routeLongName = routeObject.getString(BusManager.TAG_LONG_NAME);
            String routeID = routeObject.getString(BusManager.TAG_ROUTE_ID);
            Route r = sharedManager.getRoute(routeLongName, routeID);
            JSONArray stops = routeObject.getJSONArray(BusManager.TAG_STOPS);
            for (int i = 0; i < stops.length(); i++) {
                r.addStop(i, sharedManager.getStopByID(stops.getString(i)));
            }
            JSONArray segments = routeObject.getJSONArray(BusManager.TAG_SEGMENTS);
            if (MainActivity.LOCAL_LOGV) Log.v(MainActivity.REFACTOR_LOG_TAG, "Found " + segments.length() + " segments for route " + routeID);
            for (int i = 0; i < segments.length(); i++) {
                //if (MainActivity.LOCAL_LOGV) Log.v("MapDebugging", "parseJSON of Route adding segment ID " + segments.getJSONArray(i).getString(0) + " for " + routeID + "(" + r.getSegmentIDs().size() + " total)");
                r.getSegmentIDs().add(segments.getJSONArray(i).getString(0));
            }
            sharedManager.addRoute(r);
            //if (MainActivity.LOCAL_LOGV) Log.v("JSONDebug", "Route name: " + routeLongName + " | ID:" + routeID + " | Number of stops: " + sharedManager.getRouteByID(routeID).getStops().size());
        }
    }

    void addStop(int index, Stop stop) {
        if (stops.contains(stop)) stops.remove(stop);
        if (stops.size() == index) stops.add(stop);
        else stops.add(index, stop);
    }

    public ArrayList<String> getSegmentIDs() {
        return segmentIDs;
    }

    public String toString() {
        return longName;
    }

    public ArrayList<PolylineOptions> getSegments() {
        return segments;
    }

    public Route setName(String name) {
        longName = name;
        return this;
    }

    public Route setOtherName(String otherName) {
        otherLongName = otherName;
        return this;
    }

    public String getOtherLongName() {
        return otherLongName;
    }

    public String getID() {
        return routeID;
    }

    public boolean hasStop(Stop stop) {
        return stops.contains(stop);
    }

    public ArrayList<Stop> getStops() {
        return stops;
    }

    public void addStop(Stop stop) {
        if (!stops.contains(stop)) stops.add(stop);
    }

    public String getLongName() {
        return longName;
    }
}
