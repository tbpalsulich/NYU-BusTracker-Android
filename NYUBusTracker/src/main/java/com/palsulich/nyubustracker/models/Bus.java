package com.palsulich.nyubustracker.models;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.palsulich.nyubustracker.helpers.BusManager;
import com.palsulich.nyubustracker.activities.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Bus {
    String vehicleID = "";
    LatLng loc;
    String heading = "";
    String route;

    public Bus(String mVehicleID){
        vehicleID = mVehicleID;
    }

    public Bus setLocation(String lat, String lng){
        loc = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
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

    public static void parseJSON(JSONObject vehiclesJson) throws JSONException{
        BusManager sharedManager = BusManager.getBusManager();
        JSONArray jVehicles = vehiclesJson.getJSONObject(MainActivity.TAG_DATA).getJSONArray("72");
        for (int j = 0; j < jVehicles.length(); j++) {
            JSONObject busObject = jVehicles.getJSONObject(j);
            JSONObject busLocation = busObject.getJSONObject(MainActivity.TAG_LOCATION);
            String busLat = busLocation.getString(MainActivity.TAG_LAT);
            String busLng = busLocation.getString(MainActivity.TAG_LNG);
            String busRoute = busObject.getString(MainActivity.TAG_ROUTE_ID);
            String vehicleID = busObject.getString(MainActivity.TAG_VEHICLE_ID);
            String busHeading = busObject.getString(MainActivity.TAG_HEADING);
            sharedManager.addBus(new Bus(vehicleID).setHeading(busHeading).setLocation(busLat, busLng).setRoute(busRoute));
            Log.v("JSONDebug", "Bus ID: " + vehicleID + " | Heading: " + busHeading + " | (" + busLat + ", " + busLng + ")");
        }
    }
}
