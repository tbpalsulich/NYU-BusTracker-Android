package com.palsulich.nyubustracker.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.palsulich.nyubustracker.R;
import com.palsulich.nyubustracker.helpers.BusManager;
import com.palsulich.nyubustracker.models.Stop;
import com.palsulich.nyubustracker.models.Time;

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
            Log.v("Debugging", "Looking for times for " + routeName);

            Stop stop = sharedManager.getStopByName(stopName);
            Time[] times = stop.getTimes().get(dayOfWeek).get(routeName);
            if (times == null) {
                times = new String[1];
                times[0] = getApplicationContext().getString(R.string.no_times);
                Log.v("Debugging", "Number of times on " + dayOfWeek + ": 0");
            } else {
                Log.v("Debugging", "Number of times on " + dayOfWeek + ": " + stop.getTimes().get(dayOfWeek).get(routeName).length);
            }
            ArrayAdapter<Time> mAdapter =
                    new ArrayAdapter<Time>(this,
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
