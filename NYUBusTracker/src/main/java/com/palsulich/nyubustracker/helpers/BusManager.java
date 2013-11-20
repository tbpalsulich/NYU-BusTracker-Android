package com.palsulich.nyubustracker.helpers;

import android.util.Log;

import com.google.maps.android.PolyUtil;
import com.palsulich.nyubustracker.models.Bus;
import com.palsulich.nyubustracker.models.Route;
import com.palsulich.nyubustracker.models.Stop;
import com.palsulich.nyubustracker.models.Time;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public final class BusManager {
    private static BusManager sharedBusManager = null;      // Singleton instance.
    private static ArrayList<Stop> stops = null;            // Hold all known stops.
    private static ArrayList<Route> routes = null;
    private static ArrayList<String> hideRoutes = null;     // Routes to not show the user.
    private static ArrayList<String> hideStops = null;      // Stops to not show the user.
    private static ArrayList<Bus> buses = null;

    private BusManager(){
        stops = new ArrayList<Stop>();
        routes = new ArrayList<Route>();
        hideRoutes = new ArrayList<String>();
        hideStops = new ArrayList<String>();
        buses = new ArrayList<Bus>();
    }

    public static BusManager getBusManager() {
        if (sharedBusManager == null) {
            sharedBusManager = new BusManager();
        }
        return sharedBusManager;
    }

    public ArrayList<Stop> getStops() {
        return stops;
    }

    public ArrayList<Route> getRoutes() {
        return routes;
    }

    public boolean hasStops() {
        return stops != null && stops.size() > 0;
    }

    /*
    Given the name of a stop (e.g. "715 Broadway"), getStopByName returns the Stop with that name.
     */
    public Stop getStopByName(String stopName) {
        for (Stop s : stops) {
            if (s.getName().equals(stopName)) return s;
        }
        return null;
    }

    /*
    Given the ID of a stop, getStopByID returns the Stop with that ID.
     */
    public Stop getStopByID(String stopID) {
        for (Stop s : stops) {
            if (s.getID().equals(stopID)) return s;
        }
        return null;
    }

    /*
    Returns an array of strings with the names of all stops.
     */
    public String[] getStopsAsArray() {
        String[] stopsArray = new String[stops.size()];
        for (int i = 0; i < stopsArray.length; i++) {
            stopsArray[i] = stops.get(i).toString();
        }
        return stopsArray;
    }

    /*
    Given a route ID, getStopsByRouteID returns an ArrayList of all Stops visited by that Route.
     */
    public ArrayList<Stop> getStopsByRouteID(String routeID) {
        ArrayList<Stop> result = new ArrayList<Stop>();
        for (Stop stop : stops) {
            //Log.v("Debugging", "Number of routes of stop " + j + ": " + stop.routes.size());
            if (stop.hasRouteByString(routeID)) {
                result.add(stop);
            }
        }
        return result;
    }

    public boolean hasRoutes() {
        return routes != null && routes.size() > 0;
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

    /*
    Given an ID (e.g. "81374"), returns the Route with that ID.
     */
    public Route getRouteByID(String id) {
        if (routes != null){
            for (Route route : routes) {
                if (route.getID().equals(id)) {
                    return route;
                }
            }
        }
        return null;
    }

    /*
    Given the name of a route (e.g. "E"), returns the Route with that name.
     */
    public Route getRouteByName(String name) {
        for (Route route : routes) {
            if (route.getLongName().equals(name)) {
                return route;
            }
        }
        return null;
    }

    /*
    Given a Stop, getConnectedStops returns an array of Strings corresponding to every stop which has
    some route between it and the given stop.
     */
    public String[] getConnectedStops(Stop stop){
        int resultSize = 0;
        String temp[] = new String[64];     // Shouldn't have more than 64 stops...
        ArrayList<Route> stopRoutes = stop.getRoutes();
        for (Route route : stopRoutes) {       // For every route servicing this stop:
            String routeStops[] = route.getStopsAsArray();
            for (String connectedStop : routeStops){    // add all of that route's stops.
                if (!connectedStop.equals(stop.getName())){
                    temp[resultSize++] = connectedStop;
                }
            }
        }
        String result[] = new String[resultSize];       // Only return an array of the proper size.
        System.arraycopy(temp, 0, result, 0, resultSize);
        return result;
    }

    /*
    addStop will add a Stop to our ArrayList of Stops, unless we're supposed to hide it.
     */
    public void addStop(Stop stop) {
        if (hideStops != null && !hideStops.contains(stop.getID())){
            stops.add(stop);
            Log.v("Debugging", "Added " + stop.toString() + " to list of stops (" + stops.size() + ")");
        }
    }

    /*
    addRoute will add a Route to our ArrayList of Routes, unless we're supposed to hide it.
     */
    public void addRoute(Route route) {
        if (!hideRoutes.contains(route.getID())){
            Log.v("JSONDebug", "Adding route: " + route.getID());
            routes.add(route);
        }
    }

    public void addBus(Bus bus) {
        buses.add(bus);
    }

    /*
    Given a JSONObject of the version file and a fFileGrabber, parses all of the times.
    Version also has a list of hideroutes, hidestops, combine, and opposite stops. We also handle
    parsing those here, since we already have the file.
    To parse all of the times, we get the stop name from the version file then make a new request
    to our FileGrabber to get the times JSON object corresponding to that stop ID.
    So, the sequence of events is: we're parsing version.json, we find a stop object (specified by an
    ID), we request the JSON of times for that stop, and we parse those times.
     */
    public static void parseTimes(JSONObject versionJson, FileGrabber mFileGrabber) throws JSONException {
        ArrayList<Stop> stops = sharedBusManager.getStops();
        Log.v("Debugging", "Looking for times for " + stops.size() + " stops.");
        JSONArray jHides = versionJson.getJSONArray("hideroutes");
        for (int j = 0; j < jHides.length(); j++){      // For each element of our list of hideroutes.
            String hideMeID = jHides.getString(j);      // ID of the route to hide.
            Log.v("JSONDebug", "Hiding a route... " + hideMeID);
            Route r = sharedBusManager.getRouteByID(hideMeID);
            hideRoutes.add(hideMeID);           // In case we "hide" the route before it exists.
            if (r != null){
                routes.remove(r);       // If we already parsed this route, remove it.
                for (Stop s : stops){   // But, we must update any stops that have this route.
                    if (s.hasRouteByString(hideMeID)){
                        s.getRoutes().remove(r);
                        Log.v("JSONDebug", "Removing route " + r.getID() + " from " + s.getName());
                    }
                }
            }
        }

        JSONArray jHideStops = versionJson.getJSONArray("hidestops");
        for (int j = 0; j < jHideStops.length(); j++){
            String hideMeID = jHideStops.getString(j);
            Log.v("JSONDebug", "Hiding a stop... " + hideMeID);
            Stop s = sharedBusManager.getStopByID(hideMeID);
            hideStops.add(hideMeID);           // In case we "hide" the stop before it exists.
            if (s != null){
                stops.remove(s);
                for (Route r : routes){
                    if (r.hasStopByID(hideMeID)){
                        r.getStops().remove(s);
                        Log.v("JSONDebug", "Removing stop " + s.getID() + " from " + r.getLongName());
                    }
                }
            }
        }

        JSONArray jVersion = versionJson.getJSONArray("versions");
        for (int j = 0; j < jVersion.length(); j++) {
            JSONObject stopObject = jVersion.getJSONObject(j);
            String file = stopObject.getString("file");
            Log.v("Debugging", "Looking for times for " + file);
            JSONObject timesJson = mFileGrabber.getTimesFromFile(file);
            JSONObject routes = timesJson.getJSONObject(FileGrabber.TAG_ROUTES);
            Stop s = sharedBusManager.getStopByID(file.substring(0, file.indexOf(".")));
            for (int i = 0; i < s.getRoutes().size(); i++) {
                if (routes.has(s.getRoutes().get(i).getID())) {
                    JSONObject routeTimes = routes.getJSONObject(s.getRoutes().get(i).getID());
                    if (routeTimes.has(FileGrabber.TAG_WEEKDAY)) {
                        JSONArray weekdayTimesJson = routeTimes.getJSONArray(FileGrabber.TAG_WEEKDAY);
                        Time[] weekdayTimes = new Time[weekdayTimesJson.length()];
                        Log.v("Debugging", "Found " + weekdayTimes.length + " weekday times.");
                        if (weekdayTimesJson != null) {
                            for (int k = 0; k < weekdayTimes.length; k++) {
                                weekdayTimes[k] = new Time(weekdayTimesJson.getString(k));
                            }
                            String weekdayRoute = routeTimes.getString(FileGrabber.TAG_ROUTE);
                            s.addTime(weekdayRoute.substring(weekdayRoute.indexOf("Route ") + "Route ".length()), "Weekday", weekdayTimes);
                        }
                    }
                    if (routeTimes.has(FileGrabber.TAG_FRIDAY)) {
                        JSONArray fridayTimesJson = routeTimes.getJSONArray(FileGrabber.TAG_FRIDAY);
                        Time[] fridayTimes = new Time[fridayTimesJson.length()];
                        if (fridayTimesJson != null) {
                            for (int k = 0; k < fridayTimes.length; k++) {
                                fridayTimes[k] = new Time(fridayTimesJson.getString(k));
                            }
                            String fridayRoute = routeTimes.getString(FileGrabber.TAG_ROUTE);
                            s.addTime(fridayRoute.substring(fridayRoute.indexOf("Route ") + "Route ".length()), "Friday", fridayTimes);

                        }
                    }
                    if (routeTimes.has(FileGrabber.TAG_WEEKEND)) {
                        JSONArray weekendTimesJson = routeTimes.getJSONArray(FileGrabber.TAG_WEEKEND);
                        Time[] weekendTimes = new Time[weekendTimesJson.length()];
                        if (weekendTimesJson != null) {
                            for (int k = 0; k < weekendTimes.length; k++) {
                                weekendTimes[k] = new Time(weekendTimesJson.getString(k));
                            }
                            String weekendRoute = routeTimes.getString(FileGrabber.TAG_ROUTE);
                            s.addTime(weekendRoute.substring(weekendRoute.indexOf("Route ") + "Route ".length()), "Weekend", weekendTimes);

                        }
                    }
                }
            }

        }
    }

    public static void parseSegments(JSONObject segmentsJSON) throws JSONException{
        final BusManager sharedManager = BusManager.getBusManager();
        JSONObject segments = segmentsJSON.getJSONObject("data");
        for (Route r : sharedManager.getRoutes()){
            Log.v("MapDebugging", "Does this route (" + r.getID() + ") have segments?");
            for (String seg : r.getSegmentIDs()){
                Log.v("MapDebugging", "Yes. r.addSegment to route " + r.getID());
                r.addSegment(PolyUtil.decode(segments.getString(seg)));
            }
        }
    }
}
