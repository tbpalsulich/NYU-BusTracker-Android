package com.palsulich.nyubustracker;

/**
 * Created by tyler on 9/30/13.
 */
public class Bus {
    String vehicleID = "";
    String busLat = "";
    String busLng = "";
    String heading = "";
    String route;
    public Bus(String mVehicleID){
        vehicleID = mVehicleID;
    }

    public Bus setLocation(String lat, String lng){
        busLat = lat;
        busLng = lng;
        return this;
    }
    public Bus setHeading(String mHeading){
        heading = mHeading;
        return this;
    }
    public Bus setRoute(String mRoute){
        route = mRoute;
        return this;
    }
}
