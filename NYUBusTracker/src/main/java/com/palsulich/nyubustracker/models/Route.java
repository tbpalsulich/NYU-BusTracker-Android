package com.palsulich.nyubustracker.models;

import android.util.Log;

import com.google.android.gms.maps.model.PolylineOptions;
import com.palsulich.nyubustracker.helpers.BusManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Route {
    String longName = "";
    String routeID = "";
    ArrayList<Stop> stops = null;
    BusManager sharedManager;
    ArrayList<String> segmentIDs;
    ArrayList<PolylineOptions> segments;

    public Route(String mLongName, String mRouteID){
        segmentIDs = new ArrayList<String>();
        segments = new ArrayList<PolylineOptions>();
        longName = mLongName;
        routeID = mRouteID;
        sharedManager = BusManager.getBusManager();
        stops = sharedManager.getStopsByRouteID(routeID);
        for (Stop s : stops){
            s.addRoute(this);
        }
        //Log.v("Debugging", longName + "'s number of stops:" + stops.size());
    }

    public String toString(){
        return longName;
    }

    public Route setName(String name){
        longName = name;
        return this;
    }

    public ArrayList<String> getSegmentIDs() {
        return segmentIDs;
    }

    public String getLongName(){
        return longName;
    }

    public String getID(){
        return routeID;
    }

    public boolean hasStop(Stop stop){
        return stops.contains(stop);
    }

    public ArrayList<Stop> getStops(){
        return stops;
    }

    public void addStop(Stop stop){
        if(!stops.contains(stop)) stops.add(stop);
    }

    public void addStop(int index, Stop stop){
        if(stops.contains(stop)) stops.remove(stop);
        if (stops.size() == index) stops.add(stop);
        else stops.add(index, stop);
    }

    public boolean isActive(){
        ArrayList<Time> times = sharedManager.getStopByName("715 Broadway @ Washington Square").getTimesOfRoute(longName);
        Time currentTime = Time.getCurrentTime();
        for (Time t : times){
            if (!t.isStrictlyBefore(currentTime) && currentTime.getTimeOfWeek() == t.getTimeOfWeek()){
                return true;
            }
        }
        return false;
    }

    public static void parseJSON(JSONObject routesJson) throws JSONException{
        JSONArray jRoutes = new JSONArray();
        BusManager sharedManager = BusManager.getBusManager();
        if (routesJson != null) jRoutes = routesJson.getJSONObject(BusManager.TAG_DATA).getJSONArray("72");
        for (int j = 0; j < jRoutes.length(); j++) {
            JSONObject routeObject = jRoutes.getJSONObject(j);
            String routeLongName = routeObject.getString(BusManager.TAG_LONG_NAME);
            String routeID = routeObject.getString(BusManager.TAG_ROUTE_ID);
            Route r = sharedManager.getRoute(routeLongName, routeID);
            JSONArray stops = routeObject.getJSONArray(BusManager.TAG_STOPS);
            for (int i = 0; i < stops.length(); i++){
                r.addStop(i, sharedManager.getStopByID(stops.getString(i)));
            }
            JSONArray segments = routeObject.getJSONArray(BusManager.TAG_SEGMENTS);
            //Log.v("MapDebugging", "Found " + segments.length() + " segments for route " + routeID);
            for (int i = 0; i < segments.length(); i++){
                //Log.v("MapDebugging", "parseJSON of Route adding segment ID " + segments.getJSONArray(i).getString(0) + " for " + routeID + "(" + r.getSegmentIDs().size() + " total)");
                r.getSegmentIDs().add(segments.getJSONArray(i).getString(0));
            }
            sharedManager.addRoute(r);
            //Log.v("JSONDebug", "Route name: " + routeLongName + " | ID:" + routeID + " | Number of stops: " + sharedManager.getRouteByID(routeID).getStops().size());
        }
    }
}
