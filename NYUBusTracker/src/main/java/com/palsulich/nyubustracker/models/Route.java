package com.palsulich.nyubustracker.models;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.palsulich.nyubustracker.helpers.BusManager;
import com.palsulich.nyubustracker.helpers.FileGrabber;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Route {
    String longName = "";
    String routeID = "";
    ArrayList<Stop> stops = null;
    BusManager sharedManager;
    PolylineOptions segment;

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

    public String toString(){
        return longName;
    }

    public void setSegment(List<LatLng> p){
        segment = new PolylineOptions().addAll(p);
    }

    public PolylineOptions getSegment(){
        return segment;
    }

    public String getLongName(){
        return longName;
    }

    public String getID(){
        return routeID;
    }

    public boolean hasStop(String stop){
        for (Stop s : stops){
            if (s.name.equals(stop)) return true;
        }

        return false;
    }

    public boolean hasStopByID(String stopID){
        for (Stop s : stops){
            if (s.getID().equals(stopID)) return true;
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
            result[0] = "No stops available.";
        }
        else{
            result = new String[stops.size()];
        }
        int j = 0;
        for(Stop s : stops){
            result[j++] = s.toString();
        }
        return result;
    }

    public void addStop(Stop stop){
        stops.add(stop);
    }

    public static void parseJSON(JSONObject routesJson) throws JSONException{
        JSONArray jRoutes = null;
        BusManager sharedManager = BusManager.getBusManager();
        jRoutes = routesJson.getJSONObject(FileGrabber.TAG_DATA).getJSONArray("72");
        for (int j = 0; j < jRoutes.length(); j++) {
            JSONObject routeObject = jRoutes.getJSONObject(j);
            String routeLongName = routeObject.getString(FileGrabber.TAG_LONG_NAME);
            String routeID = routeObject.getString(FileGrabber.TAG_ROUTE_ID);
            sharedManager.addRoute(new Route(routeLongName, routeID));
            Log.v("JSONDebug", "Route name: " + routeLongName + " | ID:" + routeID + " | Number of stops: " + sharedManager.getRouteByID(routeID).getStops().size());
        }
    }

    public boolean isConnectedTo(String routeName){

        return false;
    }
}
