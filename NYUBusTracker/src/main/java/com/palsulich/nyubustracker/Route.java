package com.palsulich.nyubustracker;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by tyler on 9/30/13.
 */
public class Route {
    String longName = "";
    String routeID = "";
    ArrayList<Stop> stops = null;
    public Route(String mLongName, String mRouteID){
        longName = mLongName;
        routeID = mRouteID;
        BusManager sharedManager = BusManager.getBusManager();
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
        String[] result = new String[stops.size()];
        for(int j = 0; j < result.length; j++){
            result[j] = stops.get(j).toString();
        }
        return result;
    }

    public void addStop(Stop stop){
        stops.add(stop);
    }
}
