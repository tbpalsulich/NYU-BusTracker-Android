package com.palsulich.nyubustracker;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by tyler on 9/30/13.
 */
public final class BusManager {
    static BusManager sharedBusManager = null;
    private ArrayList<Stop> stops = null;
    private ArrayList<Route> routes = null;
    private ArrayList<Bus> buses = null;
    public static BusManager getBusManager(){
        if (sharedBusManager == null){
            sharedBusManager = new BusManager();
        }
        return sharedBusManager;
    }
    private BusManager(){
        stops = new ArrayList<Stop>();
        routes = new ArrayList<Route>();
        buses = new ArrayList<Bus>();
    }

    public ArrayList<Stop> getStops(){
        return stops;
    }
    public Stop getStopByName(String stopName){
        for (int j = 0; j < stops.size(); j++){
            if (stops.get(j).name.equals(stopName)) return stops.get(j);
        }
        return null;
    }
    public Stop getStopByID(String stopID){
        for (int j = 0; j < stops.size(); j++){
            if (stops.get(j).id.equals(stopID)) return stops.get(j);
        }
        return null;
    }
    public String[] getStopsAsArray(){
        String[] stopsArray = new String[stops.size()];
        for (int i = 0; i < stopsArray.length; i++){
            stopsArray[i] = stops.get(i).toString();
        }
        return stopsArray;
    }
    public ArrayList<Stop> getStopsByRouteID(String routeID){
        ArrayList<Stop> result = new ArrayList<Stop>();
        for (int j = 0; j < stops.size(); j++){
            Stop stop = stops.get(j);
            //Log.v("Debugging", "Number of routes of stop " + j + ": " + stop.routes.size());
            if (stop.hasRouteByString(routeID)){
                result.add(stop);
            }
        }
        return result;
    }
    public ArrayList<Route> getRoutes(){
        return routes;
    }
    public String[] getRoutesAsArray(){
        String[] routesArray = new String[routes.size()];
        for (int i = 0; i < routesArray.length; i++){
            routesArray[i] = routes.get(i).toString();
        }
        return routesArray;
    }
    public Route getRouteByID(String id){
        for (int j = 0; j < routes.size(); j++){
            Route route = routes.get(j);
            if(route.routeID.equals(id)){
                return route;
            }
        }
        return null;
    }
    public Route getRouteByName(String name){
        for (int j = 0; j < routes.size(); j++){
            Route route = routes.get(j);
            if(route.longName.equals(name)){
                return route;
            }
        }
        return null;
    }
    public ArrayList<Route> getRoutesByStopID(String stopID){
        ArrayList<Route> result = new ArrayList<Route>();
        for (int j = 0; j < routes.size(); j++){
            Route route = routes.get(j);
            if (route.hasStop(stopID)){
                result.add(route);
            }
        }
        return result;
    }
    public ArrayList<Bus> getBuses(){
        return buses;
    }

    public void addStop(Stop stop){
        stops.add(stop);
        Log.v("Debugging", "Added " + stop.toString() + " to list of stops (" + stops.size() + ")");
    }
    public void addRoute(Route route){
        routes.add(route);
    }
    public void addBus(Bus bus){
        buses.add(bus);
    }

}
