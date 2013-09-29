package com.palsulich.nyubustracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class MainActivity extends Activity {

    String charset = "UTF-8";
    String agencies = "72";
    String query = makeQuery("agencies", agencies, charset);

    private String stopsURL = "http://api.transloc.com/1.2/stops.json?" + query;
    private String routesURL = "http://api.transloc.com/1.2/routes.json?" + query;
    private String segmentsURL = "http://api.transloc.com/1.2/segments.json?" + query;
    private String vehiclesURL = "http://api.transloc.com/1.2/vehicles.json?" + query;

    private String makeQuery(String param, String value, String charset){
        try {
            return String.format(param + "=" + URLEncoder.encode(agencies, charset));
        } catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }
        return "";
    }

    private static final String TAG_API_CURRENT_VERSION = "api_current_version";
    private static final String TAG_API_VERSION = "api_version";
    private static final String TAG_GENERATED_ON = "generated_on";
    private static final String TAG_EXPIRES_IN = "expires_in";
    private static final String TAG_RATE_LIMIT = "rate_limit";
    private static final String TAG_DATA = "data";
    private static final String TAG_AGENCY_ID = "agency_id";
    private static final String TAG_NAME = "name";
    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_CODE = "code";
    private static final String TAG_URL = "url";
    private static final String TAG_LOCATION_TYPE = "location_type";
    private static final String TAG_LOCATION = "location";
    private static final String TAG_LAT = "lat";
    private static final String TAG_LNG = "lng";
    private static final String TAG_VEHICLE_ID = "vehicle_id";
    private static final String TAG_CALL_NAME = "call_name";
    private static final String TAG_TRACKING_STATUS = "tracking_status";
    private static final String TAG_HEADING = "heading";
    private static final String TAG_ARRIVAL_ESTIMATES = "arrival_estimates";
    private static final String TAG_ARRIVAL_AT = "arrival_at";
    private static final String TAG_SEGMENT_ID = "segment_id";
    private static final String TAG_SPEED = "speed";
    private static final String TAG_LAST_UPDATE = "last_updated_on";
    private static final String TAG_STOP_NAME = "name";
    private static final String TAG_STOP_ID = "stop_id";
    private static final String TAG_ROUTES = "routes";
    private static final String TAG_ROUTE = "route";
    private static final String TAG_WEEKDAY = "Weekday";
    private static final String TAG_FRIDAY = "Friday";
    private static final String TAG_WEEKEND = "Weekend";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        JSONArray jStops = null;
        ArrayList<Stop> stops = new ArrayList<Stop>();
        JSONArray jRoutes = null;
        JSONArray jSegments = null;
        JSONArray jVehicles = null;

        // Creating JSON Parser instance
        JSONParser jParser = new JSONParser();

        // getting JSON string from URL
        JSONObject stopsJson = jParser.getJSONFromUrl(stopsURL);/*
        JSONObject routesJson = jParser.getJSONFromUrl(routesURL);
        JSONObject segmentsJson = jParser.getJSONFromUrl(segmentsURL);
        JSONObject vehiclesJson = jParser.getJSONFromUrl(vehiclesURL);*/

        try {
            jStops = stopsJson.getJSONArray(TAG_DATA);
            Log.v("JSONDebug", "Number of stops: " + jStops.length());
            for(int i = 0; i < jStops.length(); i++){
                try {
                    JSONObject stopObject = jStops.getJSONObject(i);
                    String stopID = stopObject.getString(TAG_STOP_ID);
                    String stopName = stopObject.getString(TAG_STOP_NAME);
                    JSONObject location = stopObject.getJSONObject(TAG_LOCATION);
                    String stopLat = location.getString(TAG_LAT);
                    String stopLng = location.getString(TAG_LNG);
                    JSONArray stopRoutes = stopObject.getJSONArray(TAG_ROUTES);
                    String[] routes = new String[5];
                    for(int j = 0; j < stopRoutes.length(); j++){
                        routes[j] = stopRoutes.getString(j);
                    }
                    stops.add(new Stop(stopName, stopLat, stopLng, stopID, routes));
                    Log.v("JSONDebug", "Stop name: " + stopName + ", stop ID: " + stopID);
                } catch (JSONException e) {
                    Log.v("JSONDebug", "Found a bug in parsing the JSON:\n" + jStops.getJSONObject(i).toString());
                    // Oops
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void routeBButton(View view) {
        Intent myIntent = new Intent(this, BusEActivity.class);
        this.startActivity(myIntent);
    }

    public void routeEButton(View view) {
        Intent myIntent = new Intent(this, BusEActivity.class);
        this.startActivity(myIntent);
    }

    public void routeFButton(View view) {
        Intent myIntent = new Intent(this, BusFActivity.class);
        this.startActivity(myIntent);
    }


    
}
