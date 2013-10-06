package com.palsulich.nyubustracker;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by tyler on 9/30/13.
 */
public class Route {
    String longName = "";
    String routeID = "";
    ArrayList<Stop> stops = null;
    BusManager sharedManager;
    public Route(String mLongName, String mRouteID){
        longName = mLongName;
        routeID = mRouteID;
        sharedManager = BusManager.getBusManager();
        stops = sharedManager.getStopsByRouteID(routeID);
        for (int j = 0; j < stops.size(); j++){
            stops.get(j).addRoute(this);
        }
        Log.v("Debugging", longName + "'s number of stops:" + stops.size());
    }

    public String toString(){
        return longName;
    }

    public boolean hasStop(String stop){
        return stops.contains(stop);
    }

    public ArrayList<Stop> getStops(){
        return stops;
    }
    public String[] getStopsAsArray(){
        String[] result;
        if(stops.size() == 0){
            result = new String[1];
            result[0] = sharedManager.getContext().getString(R.string.no_stops);
        }
        else{
            result = new String[stops.size()];
        }
        for(int j = 0; j < stops.size(); j++){
            result[j] = stops.get(j).toString();
        }
        return result;
    }

    public void addStop(Stop stop){
        stops.add(stop);
    }

    public static void parseJSON(JSONObject routesJson) throws JSONException{
        JSONArray jRoutes = null;
        BusManager sharedManager = BusManager.getBusManager();
        jRoutes = routesJson.getJSONObject(MainActivity.TAG_DATA).getJSONArray("72");
        for (int j = 0; j < jRoutes.length(); j++) {
            JSONObject routeObject = jRoutes.getJSONObject(j);
            String routeLongName = routeObject.getString(MainActivity.TAG_LONG_NAME);
            String routeID = routeObject.getString(MainActivity.TAG_ROUTE_ID);
            sharedManager.addRoute(new Route(routeLongName, routeID));
            Log.v("JSONDebug", "Route name: " + routeLongName + " | ID:" + routeID + " | Number of stops: " + sharedManager.getRouteByID(routeID).getStops().size());
        }
    }
}
