package com.palsulich.nyubustracker.models;

import com.google.android.gms.maps.model.LatLng;
import com.palsulich.nyubustracker.helpers.BusManager;
import com.palsulich.nyubustracker.helpers.FileGrabber;

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

    public LatLng getLocation(){
        return loc;
    }

    public Bus setHeading(String mHeading){
        if (heading.equals("null")) heading = "0";
        else heading = mHeading;
        return this;
    }
    public Bus setRoute(String mRoute){
        route = mRoute;
        return this;
    }

    public String getRoute() {
        return route;
    }

    public Float getHeading() {
        return Float.parseFloat(heading);
    }

    public String getID(){
        return vehicleID;
    }
    public static void parseJSON(JSONObject vehiclesJson) throws JSONException{
        BusManager sharedManager = BusManager.getBusManager();
        JSONObject jVehiclesData = null;
        if(vehiclesJson != null) jVehiclesData = vehiclesJson.getJSONObject(FileGrabber.TAG_DATA);
        JSONArray jVehicles = new JSONArray();
        if (jVehiclesData != null) jVehicles = jVehiclesData.getJSONArray("72");
        for (int j = 0; j < jVehicles.length(); j++) {
            JSONObject busObject = jVehicles.getJSONObject(j);
            JSONObject busLocation = busObject.getJSONObject(FileGrabber.TAG_LOCATION);
            String busLat = busLocation.getString(FileGrabber.TAG_LAT);
            String busLng = busLocation.getString(FileGrabber.TAG_LNG);
            String busRoute = busObject.getString(FileGrabber.TAG_ROUTE_ID);
            String vehicleID = busObject.getString(FileGrabber.TAG_VEHICLE_ID);
            String busHeading = busObject.getString(FileGrabber.TAG_HEADING);
            // getBus will either return an existing bus, or create a new one for us. We'll have to parse the bus JSON often.
            Bus b = sharedManager.getBus(vehicleID);
            b.setHeading(busHeading).setLocation(busLat, busLng).setRoute(busRoute);
            //Log.v("BusLocations", "Parsing buses: bus id: " + vehicleID + " | bus' route: " + busRoute);

            //Log.v("JSONDebug", "Bus ID: " + vehicleID + " | Heading: " + busHeading + " | (" + busLat + ", " + busLng + ")");
        }
    }
}
