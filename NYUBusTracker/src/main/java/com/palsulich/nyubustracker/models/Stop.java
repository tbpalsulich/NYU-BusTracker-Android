package com.palsulich.nyubustracker.models;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.palsulich.nyubustracker.activities.MainActivity;
import com.palsulich.nyubustracker.helpers.BusManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by tyler on 9/29/13.
 */
public class Stop {
    String name, id;
    LatLng loc;
    String[] routesString;
    ArrayList<Route> routes = null;
    HashMap<String, HashMap<String, Time[]>> times = null;

    public Stop(String mName, String mLat, String mLng, String mID, String[] mRoutes){
        name = mName;
        loc = new LatLng(Double.parseDouble(mLat), Double.parseDouble(mLng));
        id = mID;
        routesString = mRoutes;
        times = new HashMap<String, HashMap<String, Time[]>>();
        times.put("Weekday", new HashMap<String, Time[]>());
        times.put("Weekend", new HashMap<String,Time[]>());
        times.put("Friday", new HashMap<String, Time[]>());
        routes = new ArrayList<Route>();
        BusManager sharedManager = BusManager.getBusManager();
        for (int j = 0; j < mRoutes.length; j++){
            Route r = sharedManager.getRouteByID(mRoutes[j]);
            if (r != null) r.addStop(this);
        }
    }

    public LatLng getLocation(){
        return loc;
    }

    public String getName(){
        return name;
    }

    public String toString(){
        return name;
    }

    public boolean hasRouteByString(String routeID){
        for (int j = 0; j < routesString.length; j++){
            String route = routesString[j];
            if (route.equals(routeID)) {
                return true;
            }
        }
        return false;
    }
    public ArrayList<Route> getRoutes(){
        return routes;
    }
    public void addRoute(Route route){
        routes.add(route);
    }

    public String getID(){
        return id;
    }

    public void addTime(String route, String dayOfWeek, Time[] mTimes){
        times.get(dayOfWeek).put(route, mTimes);
        Log.v("Debugging", "Adding " + mTimes.length + " times to " + name + "/" + route + " for " + dayOfWeek);
    }

    public HashMap<String, HashMap<String, Time[]>> getTimes(){
        return times;
    }

    public static void parseJSON(JSONObject stopsJson) throws JSONException{
        JSONArray jStops = null;
        BusManager sharedManager = BusManager.getBusManager();
        jStops = stopsJson.getJSONArray(MainActivity.TAG_DATA);
        Log.v("JSONDebug", "Number of stops: " + jStops.length());
        for (int i = 0; i < jStops.length(); i++) {
            JSONObject stopObject = jStops.getJSONObject(i);
            String stopID = stopObject.getString(MainActivity.TAG_STOP_ID);
            String stopName = stopObject.getString(MainActivity.TAG_STOP_NAME);
            JSONObject location = stopObject.getJSONObject(MainActivity.TAG_LOCATION);
            String stopLat = location.getString(MainActivity.TAG_LAT);
            String stopLng = location.getString(MainActivity.TAG_LNG);
            JSONArray stopRoutes = stopObject.getJSONArray(MainActivity.TAG_ROUTES);
            String[] routes = new String[stopRoutes.length()];
            String routesString = "";
            for (int j = 0; j < stopRoutes.length(); j++) {
                routes[j] = stopRoutes.getString(j);
                routesString += routes[j];
                if (j != stopRoutes.length() - 1) routesString += ", ";
            }
            Stop s = new Stop(stopName, stopLat, stopLng, stopID, routes);
            sharedManager.addStop(s);
            Log.v("JSONDebug", "Stop name: " + stopName + ", stop ID: " + stopID + ", routes: " + routesString);
        }
    }
}
