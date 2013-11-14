package com.palsulich.nyubustracker.models;

import android.util.Log;

import com.palsulich.nyubustracker.R;
import com.palsulich.nyubustracker.activities.MainActivity;
import com.palsulich.nyubustracker.helpers.BusManager;

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
        for (Stop s : stops){
            s.addRoute(this);
        }
        Log.v("Debugging", longName + "'s number of stops:" + stops.size());
    }

    public Route(String mRouteID){
        routeID = mRouteID;
    }

    public String toString(){
        return longName;
    }

    public String getLongName(){
        return longName;
    }

    public String getID(){
        return routeID;
    }

    public boolean hasStop(String stop){
        for (int i = 0; i < stops.size(); i++){
            if (stops.get(i).name.equals(stop)) return true;
        }

        return false;
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

    public boolean isConnectedTo(String routeName){

        return false;
    }
}
