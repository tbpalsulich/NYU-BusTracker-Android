package com.palsulich.nyubustracker.helpers;

import android.content.Context;
import android.util.Log;

import com.palsulich.nyubustracker.activities.MainActivity;
import com.palsulich.nyubustracker.models.Route;
import com.palsulich.nyubustracker.models.Stop;
import com.palsulich.nyubustracker.models.Bus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public final class BusManager {
    static BusManager sharedBusManager = null;
    private ArrayList<Stop> stops = null;
    private ArrayList<Route> routes = null;
    private ArrayList<Bus> buses = null;
    static Context mContext = null;

    public static BusManager getBusManager(Context context) {
        if (sharedBusManager == null) {
            sharedBusManager = new BusManager(context);
        }
        return sharedBusManager;
    }
    public static BusManager getBusManager() {
        if (sharedBusManager == null) {
            sharedBusManager = new BusManager(mContext);
        }
        return sharedBusManager;
    }

    private BusManager(Context context) {
        mContext = context;
        stops = new ArrayList<Stop>();
        routes = new ArrayList<Route>();
        buses = new ArrayList<Bus>();
    }

    public ArrayList<Stop> getStops() {
        return stops;
    }

    public boolean hasStops() {
        return stops.size() > 0;
    }

    public Stop getStopByName(String stopName) {
        for (int j = 0; j < stops.size(); j++) {
            if (stops.get(j).getName().equals(stopName)) return stops.get(j);
        }
        return null;
    }

    public Stop getStopByID(String stopID) {
        for (int j = 0; j < stops.size(); j++) {
            if (stops.get(j).getID().equals(stopID)) return stops.get(j);
        }
        return null;
    }

    public String[] getStopsAsArray() {
        String[] stopsArray = new String[stops.size()];
        for (int i = 0; i < stopsArray.length; i++) {
            stopsArray[i] = stops.get(i).toString();
        }
        return stopsArray;
    }

    public ArrayList<Stop> getStopsByRouteID(String routeID) {
        ArrayList<Stop> result = new ArrayList<Stop>();
        for (int j = 0; j < stops.size(); j++) {
            Stop stop = stops.get(j);
            //Log.v("Debugging", "Number of routes of stop " + j + ": " + stop.routes.size());
            if (stop.hasRouteByString(routeID)) {
                result.add(stop);
            }
        }
        return result;
    }

    public boolean hasRoutes() {
        return routes.size() > 0;
    }

    public String[] getRoutesAsArray() {
        String[] routesArray;
        if (routes.size() == 0) {
            routesArray = new String[1];
            routesArray[0] = "No routes available";
        } else {
            routesArray = new String[routes.size()];
        }
        for (int i = 0; i < routesArray.length; i++) {
            routesArray[i] = routes.get(i).toString();
        }
        return routesArray;
    }

    public Route getRouteByID(String id) {
        for (int j = 0; j < routes.size(); j++) {
            Route route = routes.get(j);
            if (route.getID().equals(id)) {
                return route;
            }
        }
        return null;
    }

    public Route getRouteByName(String name) {
        for (int j = 0; j < routes.size(); j++) {
            Route route = routes.get(j);
            if (route.getLongName().equals(name)) {
                return route;
            }
        }
        return null;
    }

    public String[] getConnectedStops(Stop stop){
        int resultSize = 0;
        String temp[] = new String[64];
        ArrayList<Route> stopRoutes = stop.getRoutes();
        for (int i = 0; i < stopRoutes.size(); i++) {       // For every route servicing this stop:
            Route route = stopRoutes.get(i);
            String routeStops[] = route.getStopsAsArray();
            for (int j = 0; j < routeStops.length; j++){    // add all of that route's stops.
                String connectedStop = routeStops[j];
                if (!connectedStop.equals(stop.getName())){
                    temp[resultSize++] = connectedStop;
                }
            }
        }
        String result[] = new String[resultSize];
        for (int i = 0; i < resultSize; i++){
            result[i] = temp[i];
        }
        return result;
    }

    public void addStop(Stop stop) {
        stops.add(stop);
        Log.v("Debugging", "Added " + stop.toString() + " to list of stops (" + stops.size() + ")");
    }

    public void addRoute(Route route) {
        routes.add(route);
    }

    public void addBus(Bus bus) {
        buses.add(bus);
    }

    public Context getContext(){
        return mContext;
    }

    public static void parseTimes(JSONObject versionJson, FileGrabber mFileGrabber) throws JSONException {
        ArrayList<Stop> stops = sharedBusManager.getStops();
        Log.v("Debugging", "Looking for times for " + stops.size() + " stops.");
        JSONArray jVersion = versionJson.getJSONArray("versions");
        for (int j = 0; j < jVersion.length(); j++) {
            JSONObject stopObject = jVersion.getJSONObject(j);
            String file = stopObject.getString("file");
            Log.v("Debugging", "Looking for times for " + file);
            JSONObject timesJson = mFileGrabber.getTimesFromFile(file);
            JSONObject routes = timesJson.getJSONObject(MainActivity.TAG_ROUTES);
            Stop s = sharedBusManager.getStopByID(file.substring(0, file.indexOf(".")));
            for (int i = 0; i < s.getRoutes().size(); i++) {
                if (routes.has(s.getRoutes().get(i).getID())) {
                    JSONObject routeTimes = routes.getJSONObject(s.getRoutes().get(i).getID());
                    if (routeTimes.has(MainActivity.TAG_WEEKDAY)) {
                        JSONArray weekdayTimesJson = routeTimes.getJSONArray(MainActivity.TAG_WEEKDAY);
                        String[] weekdayTimes = new String[weekdayTimesJson.length()];
                        if (weekdayTimesJson != null) {
                            for (int k = 0; k < weekdayTimes.length; k++) {
                                weekdayTimes[k] = weekdayTimesJson.getString(k);
                            }
                            String weekdayRoute = routeTimes.getString(MainActivity.TAG_ROUTE);
                            s.addTime(weekdayRoute.substring(weekdayRoute.indexOf("Route ") + "Route ".length()), "Weekday", weekdayTimes);

                        }
                    }
                    if (routeTimes.has(MainActivity.TAG_FRIDAY)) {
                        JSONArray fridayTimesJson = routeTimes.getJSONArray(MainActivity.TAG_FRIDAY);
                        String[] fridayTimes = new String[fridayTimesJson.length()];
                        if (fridayTimesJson != null) {
                            for (int k = 0; k < fridayTimes.length; k++) {
                                fridayTimes[k] = fridayTimesJson.getString(k);
                            }
                            String fridayRoute = routeTimes.getString(MainActivity.TAG_ROUTE);
                            s.addTime(fridayRoute.substring(fridayRoute.indexOf("Route ") + "Route ".length()), "Friday", fridayTimes);

                        }
                    }
                    if (routeTimes.has(MainActivity.TAG_WEEKEND)) {
                        JSONArray weekendTimesJson = routeTimes.getJSONArray(MainActivity.TAG_WEEKEND);
                        String[] weekendTimes = new String[weekendTimesJson.length()];
                        if (weekendTimesJson != null) {
                            for (int k = 0; k < weekendTimes.length; k++) {
                                weekendTimes[k] = weekendTimesJson.getString(k);
                            }
                            String weekendRoute = routeTimes.getString(MainActivity.TAG_ROUTE);
                            s.addTime(weekendRoute.substring(weekendRoute.indexOf("Route ") + "Route ".length()), "Weekend", weekendTimes);

                        }
                    }
                }
            }

        }
    }

}
