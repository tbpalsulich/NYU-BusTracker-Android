package com.palsulich.nyubustracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class TimeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time);

        final ListView listView = (ListView) findViewById(R.id.time_activity_list_view);
        final BusManager sharedManager = BusManager.getBusManager();
        if (sharedManager.hasStops()) {
            String dayOfWeek = getIntent().getStringExtra("day_of_week");
            String routeName = getIntent().getStringExtra("route_name");
            String stopName = getIntent().getStringExtra("stop_name");

            Stop stop = sharedManager.getStopByName(stopName);
            String[] times = stop.times.get(dayOfWeek).get(routeName);
            Log.v("Debugging", "Looking for times for " + routeName);
            Log.v("Debugging", "Number of times on " + dayOfWeek + ": " + stop.times.get(dayOfWeek).get(routeName).length);
            ArrayAdapter<String> mAdapter =
                    new ArrayAdapter<String>(this,
                            android.R.layout.simple_list_item_1,
                            times);
            listView.setAdapter(mAdapter);
        } else {
            Intent myIntent = new Intent(TimeActivity.this, MainActivity.class);
            startActivity(myIntent);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.time, menu);
        return true;
    }

}
