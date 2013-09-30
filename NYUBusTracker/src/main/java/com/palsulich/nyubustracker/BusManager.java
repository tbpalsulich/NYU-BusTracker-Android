package com.palsulich.nyubustracker;

import java.util.ArrayList;

/**
 * Created by tyler on 9/30/13.
 */
public class BusManager {
    static BusManager sharedBusManager = null;
    private ArrayList<Stop> stops = null;
    private ArrayList<Route> routes = null;
    private ArrayList<Bus> buses = null;
    public static BusManager getBusManager(){
        if (sharedBusManager == null){
            return new BusManager();
        }
        else return sharedBusManager;
    }
    private BusManager(){
        stops = new ArrayList<Stop>();
        routes = new ArrayList<Route>();
        buses = new ArrayList<Bus>();
    }

    public ArrayList<Stop> getStops(){
        return stops;
    }
    public ArrayList<Route> getRoutes(){
        return routes;
    }
    public ArrayList<Bus> getBuses(){
        return buses;
    }

    public void addStop(Stop stop){
        stops.add(stop);
    }
    public void addRoute(Route route){
        routes.add(route);
    }
    public void addBus(Bus bus){
        buses.add(bus);
    }

}
