package com.nyubustracker.models;

import com.google.android.gms.maps.model.LatLng;
import com.nyubustracker.helpers.BusManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Bus {
    private String vehicleID = "";
    private LatLng loc;
    private String heading = "";
    private String route;

    public Bus(String mVehicleID) {
        vehicleID = mVehicleID;
    }

    public static void parseJSON(JSONObject vehiclesJson) throws JSONException {
        BusManager sharedManager = BusManager.getBusManager();
        if (vehiclesJson != null) {
            JSONObject jVehiclesData = vehiclesJson.getJSONObject(BusManager.TAG_DATA);
            if (jVehiclesData != null && jVehiclesData.has("72")) {
                JSONArray jVehicles = jVehiclesData.getJSONArray("72");
                for (int j = 0; j < jVehicles.length(); j++) {
                    JSONObject busObject = jVehicles.getJSONObject(j);
                    JSONObject busLocation = busObject.getJSONObject(BusManager.TAG_LOCATION);
                    String busLat = busLocation.getString(BusManager.TAG_LAT);
                    String busLng = busLocation.getString(BusManager.TAG_LNG);
                    String busRoute = busObject.getString(BusManager.TAG_ROUTE_ID);
                    String vehicleID = busObject.getString(BusManager.TAG_VEHICLE_ID);
                    String busHeading = busObject.getString(BusManager.TAG_HEADING);
                    // getBus will either return an existing bus, or create a new one for us. We'll have to parse the bus JSON often.
                    Bus b = sharedManager.getBus(vehicleID);
                    b.setHeading(busHeading).setLocation(busLat, busLng).setRoute(busRoute);
                    //if (MainActivity.LOCAL_LOGV) Log.v("BusLocations", "Parsing buses: bus id: " + vehicleID + " | bus' route: " + busRoute);
                    //if (MainActivity.LOCAL_LOGV) Log.v("JSONDebug", "Bus ID: " + vehicleID + " | Heading: " + busHeading + " | (" + busLat + ", " + busLng + ")");
                }
            }
        }
    }

    Bus setLocation(String lat, String lng) {
        loc = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
        return this;
    }

    public LatLng getLocation() {
        return loc;
    }

    public String getRoute() {
        return route;
    }

    Bus setRoute(String mRoute) {
        route = mRoute;
        return this;
    }

    public Float getHeading() {
        try {
            return Float.parseFloat(heading);
        } catch (Exception e) {
            return 0f;
        }
    }

    Bus setHeading(String mHeading) {
        heading = mHeading;
        return this;
    }

    public String getID() {
        return vehicleID;
    }
}
