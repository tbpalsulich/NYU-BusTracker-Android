package com.palsulich.nyubustracker;

/**
 * Created by tyler on 9/29/13.
 */
public class Stop {
    String name, lat, lng, id;
    String[] routes;
    public Stop(String mName, String mLat, String mLng, String mID, String... mRoutes){
        name = mName;
        lat = mLat;
        lng = mLng;
        id = mID;
        routes = mRoutes;
    }
}
