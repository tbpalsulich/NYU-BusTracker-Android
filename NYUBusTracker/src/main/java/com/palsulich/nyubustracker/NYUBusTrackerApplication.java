package com.palsulich.nyubustracker;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;


public class NYUBusTrackerApplication extends Application {
    Tracker tracker = null;
    public NYUBusTrackerApplication() {
        super();
    }

    public synchronized Tracker getTracker(){
        if (tracker == null){
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            tracker = analytics.newTracker(R.xml.app_tracker);
        }
        return tracker;
    }
}
