package com.palsulich.nyubustracker;

/**
 * Created by tyler on 9/29/13.
 */
public class Stop {
    String name, lat, lng, id;
    String[] routes;
    public Stop(String name, String lat, String lng, String id, String... routes){
        name = name;
        lat = lat;
        lng = lng;
        id = id;
        routes = routes;
    }
}
