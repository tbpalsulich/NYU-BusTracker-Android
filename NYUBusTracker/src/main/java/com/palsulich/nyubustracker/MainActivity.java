package com.palsulich.nyubustracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MainActivity extends Activity {

    String charset = "UTF-8";
    String agencies = "72";
    String query = makeQuery("agencies", agencies, charset);

    private String stopsURL = "http://api.transloc.com/1.2/stops.json?" + query;
    private String routesURL = "http://api.transloc.com/1.2/routes.json?" + query;
    private String segmentsURL = "http://api.transloc.com/1.2/segments.json?" + query;
    private String vehiclesURL = "http://api.transloc.com/1.2/vehicles.json?" + query;
    private String versionURL = "https://s3.amazonaws.com/nyubustimes/1.0/version.json";

    private String makeQuery(String param, String value, String charset) {
        try {
            return String.format(param + "=" + URLEncoder.encode(agencies, charset));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    static final String TAG_DATA = "data";
    static final String TAG_LONG_NAME = "long_name";
    static final String TAG_LOCATION = "location";
    static final String TAG_LAT = "lat";
    static final String TAG_LNG = "lng";
    static final String TAG_HEADING = "heading";
    static final String TAG_STOP_NAME = "name";
    static final String TAG_STOP_ID = "stop_id";
    static final String TAG_ROUTES = "routes";
    static final String TAG_ROUTE = "route";
    static final String TAG_ROUTE_ID = "route_id";
    static final String TAG_WEEKDAY = "Weekday";
    static final String TAG_FRIDAY = "Friday";
    static final String TAG_WEEKEND = "Weekend";
    static final String TAG_VEHICLE_ID = "vehicle_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ListView listView = (ListView) findViewById(R.id.mainActivityList);

        final BusManager sharedManager = BusManager.getBusManager();
        if (!sharedManager.hasStops() && !sharedManager.hasRoutes()) {
            FileGrabber mFileGrabber = new FileGrabber(getCacheDir());
            try {
                Stop.parseJSON(mFileGrabber.getJSON(stopsURL, "stopsJSON"));
                Route.parseJSON(mFileGrabber.getJSON(routesURL, "routesJSON"));
                BusManager.parseTimes(mFileGrabber.getJSON(versionURL, "versionJSON"), mFileGrabber);
                Bus.parseJSON(mFileGrabber.getJSON(vehiclesURL, "vehiclesJSON"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        ArrayAdapter<String> mAdapter =
                new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_1,
                        sharedManager.getRoutesAsArray());
        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String routeName = listView.getItemAtPosition(position).toString();
                Log.v("Debugging", "Clicked on route:" + routeName);
                Intent myIntent = new Intent(MainActivity.this, RouteActivity.class);
                myIntent.putExtra("route_name", routeName);
                startActivity(myIntent);
            }
        });
        listView.setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


}
