package com.palsulich.nyubustracker.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.palsulich.nyubustracker.R;
import com.palsulich.nyubustracker.helpers.BusManager;
import com.palsulich.nyubustracker.models.Route;

public class RouteActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        final ListView listView = (ListView) findViewById(R.id.route_list_view);

        final BusManager sharedManager = BusManager.getBusManager();
        if (sharedManager.hasStops() && sharedManager.hasRoutes()) {
            Intent intent = getIntent();
            String routeName = intent.getStringExtra("route_name");
            Route route = sharedManager.getRouteByName(routeName);

            ArrayAdapter<String> mAdapter =
                    new ArrayAdapter<String>(this,
                            android.R.layout.simple_list_item_1,
                            route.getStopsAsArray());
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String stopName = listView.getItemAtPosition(position).toString();
                    if (!stopName.equals(getApplicationContext().getString(R.string.no_stops))) {
                        Intent myIntent = new Intent(RouteActivity.this, DayActivity.class);
                        myIntent.putExtra("route_name", getIntent().getStringExtra("route_name"));
                        myIntent.putExtra("stop_name", stopName);
                        startActivity(myIntent);
                    }
                }
            });
            listView.setAdapter(mAdapter);
        } else {
            Intent myIntent = new Intent(RouteActivity.this, MainActivity.class);
            startActivity(myIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.route, menu);
        return true;
    }

}
