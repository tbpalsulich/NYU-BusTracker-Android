package com.palsulich.nyubustracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class DayActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day);

        final ListView listView = (ListView) findViewById(R.id.day_activity_list_view);
        final BusManager sharedManager = BusManager.getBusManager(getApplicationContext());

        if (sharedManager.hasStops() && sharedManager.hasRoutes()) {
            ArrayAdapter<String> mAdapter =
                    new ArrayAdapter<String>(this,
                            android.R.layout.simple_list_item_1,
                            new String[]{"Weekday", "Friday", "Weekend"});
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String dayOfWeek = listView.getItemAtPosition(position).toString();
                    Intent myIntent = new Intent(DayActivity.this, TimeActivity.class);
                    myIntent.putExtra("day_of_week", dayOfWeek);
                    myIntent.putExtra("route_name", getIntent().getStringExtra("route_name"));
                    myIntent.putExtra("stop_name", getIntent().getStringExtra("stop_name"));
                    startActivity(myIntent);
                }
            });
            listView.setAdapter(mAdapter);
        } else {
            Intent myIntent = new Intent(DayActivity.this, MainActivity.class);
            startActivity(myIntent);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.day, menu);
        return true;
    }

}
