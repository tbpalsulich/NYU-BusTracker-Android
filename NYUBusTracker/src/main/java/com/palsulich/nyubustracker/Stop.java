package com.palsulich.nyubustracker;

import java.util.ArrayList;

/**
 * Created by tyler on 9/29/13.
 */
public class Stop {
    String name, lat, lng, id;
    String[] routesString;
    ArrayList<Route> routes = null;
    public Stop(String mName, String mLat, String mLng, String mID, String[] mRoutes){
        name = mName;
        lat = mLat;
        lng = mLng;
        id = mID;
        routesString = mRoutes;
        routes = new ArrayList<Route>();
        BusManager sharedManager = BusManager.getBusManager();
        for (int j = 0; j < mRoutes.length; j++){
            Route r = sharedManager.getRouteByID(mRoutes[j]);
            if (r != null) r.addStop(this);
        }
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
    public void addRoute(Route route){
        routes.add(route);
    }
}
