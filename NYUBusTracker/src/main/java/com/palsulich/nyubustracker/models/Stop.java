package com.palsulich.nyubustracker.models;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.palsulich.nyubustracker.helpers.BusManager;
import com.palsulich.nyubustracker.helpers.FileGrabber;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class Stop {
    String name, id;
    LatLng loc;
    String[] routesString;
    ArrayList<Route> routes = null;
    HashMap<String, HashMap<String, Time[]>> times = null;
    boolean favorite;

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
        for (String s : mRoutes){
            Route r = sharedManager.getRouteByID(s);
            if (r != null && !r.getStops().contains(this)) r.addStop(this);
        }
    }

    public void setValues(String mName, String mLat, String mLng, String mID, String[] mRoutes){
        if(name.equals("")) name = mName;
        if(loc == null) loc = new LatLng(Double.parseDouble(mLat), Double.parseDouble(mLng));
        id = mID;
        if(routesString == null) routesString = mRoutes;
        BusManager sharedManager = BusManager.getBusManager();
        for (String s : mRoutes){
            Route r = sharedManager.getRouteByID(s);
            if (r != null && !r.getStops().contains(this)) r.addStop(this);
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

    public boolean getFavorite(){
        return favorite;
    }

    public void setFavorite(boolean checked){
        favorite = checked;
    }

    public boolean hasRouteByString(String routeID){
        for (String route : routesString){
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

    public static Comparator<Stop> compare = new Comparator<Stop>() {
        @Override
        public int compare(Stop stop, Stop stop2) {
            if (stop.getFavorite()){
                if (stop2.getFavorite()){
                    return Integer.signum(stop.getName().compareTo(stop2.getName()));
                }
                else return -1;
            }
            else if(stop2.getFavorite()) return 1;
            else return Integer.signum(stop.getName().compareTo(stop2.getName()));
        }
    };

    public HashMap<String, HashMap<String, Time[]>> getTimes(){
        return times;
    }

    public static void parseJSON(JSONObject stopsJson) throws JSONException{
        JSONArray jStops = new JSONArray();
        BusManager sharedManager = BusManager.getBusManager();
        if (stopsJson != null) jStops = stopsJson.getJSONArray(FileGrabber.TAG_DATA);
        Log.v("JSONDebug", "Number of stops: " + jStops.length());
        for (int i = 0; i < jStops.length(); i++) {
            JSONObject stopObject = jStops.getJSONObject(i);
            String stopID = stopObject.getString(FileGrabber.TAG_STOP_ID);
            String stopName = stopObject.getString(FileGrabber.TAG_STOP_NAME);
            JSONObject location = stopObject.getJSONObject(FileGrabber.TAG_LOCATION);
            String stopLat = location.getString(FileGrabber.TAG_LAT);
            String stopLng = location.getString(FileGrabber.TAG_LNG);
            JSONArray stopRoutes = stopObject.getJSONArray(FileGrabber.TAG_ROUTES);
            String[] routes = new String[stopRoutes.length()];
            String routesString = "";
            for (int j = 0; j < stopRoutes.length(); j++) {
                routes[j] = stopRoutes.getString(j);
                routesString += routes[j];
                if (j != stopRoutes.length() - 1) routesString += ", ";
            }
            Stop s = sharedManager.getStop(stopName, stopLat, stopLng, stopID, routes);
            sharedManager.addStop(s);
            Log.v("JSONDebug", "Stop name: " + stopName + ", stop ID: " + stopID + ", routes: " + routesString);
        }
    }
}
