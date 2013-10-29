package com.palsulich.nyubustracker.models;

import android.util.Log;

import com.palsulich.nyubustracker.helpers.BusManager;
import com.palsulich.nyubustracker.activities.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    public static void parseJSON(JSONObject vehiclesJson) throws JSONException{
        JSONArray jVehicles = null;
        BusManager sharedManager = BusManager.getBusManager();
        jVehicles = vehiclesJson.getJSONObject(MainActivity.TAG_DATA).getJSONArray("72");
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
