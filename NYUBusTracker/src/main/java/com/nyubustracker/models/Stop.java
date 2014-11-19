package com.nyubustracker.models;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.nyubustracker.activities.MainActivity;
import com.nyubustracker.helpers.BusManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Stop implements Comparable<Stop> {
    public static final String FAVORITES_PREF = "favorites";
    final ArrayList<Stop> childStops;
    String name, id;
    LatLng loc;
    String[] routesString;
    ArrayList<Route> routes = null;
    String otherRoute = null;
    ArrayList<Time> times = null;
    boolean favorite;
    Stop parent;
    Stop oppositeStop;
    boolean hidden;

    public Stop(String mName, String mLat, String mLng, String mID, String[] mRoutes) {
        name = cleanName(mName);
        loc = new LatLng(Double.parseDouble(mLat), Double.parseDouble(mLng));
        id = mID;
        routesString = mRoutes;
        times = new ArrayList<Time>();
        routes = new ArrayList<Route>();
        otherRoute = "";
        childStops = new ArrayList<Stop>();
        BusManager sharedManager = BusManager.getBusManager();
        for (String s : mRoutes) {
            Route r = sharedManager.getRouteByID(s);
            if (r != null && !r.getStops().contains(this)) r.addStop(this);
        }
    }

    public static String cleanName(String name) {
        name = name.replaceAll("at", "@");
        name = name.replaceAll("[Aa]venue", "Ave");
        name = name.replaceAll("bound", "");
        name = name.replaceAll("[Ss]treet", "St");
        return name;
    }

    public static int compareStartingNumbers(String stop, String stop2) {
        int stopN = getStartingNumber(stop);
        int stopN2 = getStartingNumber(stop2);
        if (stopN > -1 && stopN2 > -1) return Integer.signum(stopN - stopN2);
        if (stopN > -1) return -1;
        if (stopN2 > -1) return 1;
        return Integer.signum(stopN - stopN2);
    }

    public static int getStartingNumber(String s) {
        if (Character.isDigit(s.charAt(0))) {
            int n = 0;
            while (n < s.length() && Character.isDigit(s.charAt(n))) {
                n++;
            }
            return Integer.parseInt(s.substring(0, n));
        }
        else return -1;
    }

    @Override
    public int compareTo(Stop stop2) {
        if (this.getFavorite()) {
            if (stop2.getFavorite()) {
                return compareStartingNumbers(this.getName(), stop2.getName());
            }
            else return -1;
        }
        else if (stop2.getFavorite()) return 1;
        else return compareStartingNumbers(this.getName(), stop2.getName());
    }

    public static void parseJSON(JSONObject stopsJson) throws JSONException {
        JSONArray jStops = new JSONArray();
        BusManager sharedManager = BusManager.getBusManager();
        if (stopsJson != null) jStops = stopsJson.getJSONArray(BusManager.TAG_DATA);
        if (MainActivity.LOCAL_LOGV) Log.v(MainActivity.REFACTOR_LOG_TAG, "BusManager current # stops: " + sharedManager.getStops());
        if (MainActivity.LOCAL_LOGV) Log.v(MainActivity.REFACTOR_LOG_TAG, "Parsing # stops: " + jStops.length());
        for (int i = 0; i < jStops.length(); i++) {
            JSONObject stopObject = jStops.getJSONObject(i);
            String stopID = stopObject.getString(BusManager.TAG_STOP_ID);
            String stopName = stopObject.getString(BusManager.TAG_STOP_NAME);
            if (MainActivity.LOCAL_LOGV) Log.v(MainActivity.REFACTOR_LOG_TAG, "*   Stop: " + stopID + " | " + stopName);
            JSONObject location = stopObject.getJSONObject(BusManager.TAG_LOCATION);
            String stopLat = location.getString(BusManager.TAG_LAT);
            String stopLng = location.getString(BusManager.TAG_LNG);
            JSONArray stopRoutes = stopObject.getJSONArray(BusManager.TAG_ROUTES);
            String[] routes = new String[stopRoutes.length()];
            for (int j = 0; j < stopRoutes.length(); j++) {
                routes[j] = stopRoutes.getString(j);
            }
            Stop s = sharedManager.getStop(stopName, stopLat, stopLng, stopID, routes);
            //if (MainActivity.LOCAL_LOGV) Log.v(MainActivity.REFACTOR_LOG_TAG, "Number of stops in manager: " + sharedManager.numStops());
            //if (MainActivity.LOCAL_LOGV) Log.v(MainActivity.REFACTOR_LOG_TAG, "___after adding " + s.name);
        }
    }

    public Stop getOppositeStop() {
        return oppositeStop;
    }

    public void setOppositeStop(Stop stop) {
        oppositeStop = stop;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean hasTimes() {
        if (times.size() > 0) return true;
        for (Stop childStop : childStops) {
            if (childStop.hasTimes()) return true;
        }
        return false;
    }

    public void setOtherRoute(String r) {
        otherRoute = r;
    }

    public void setParentStop(Stop parent) {
        this.parent = parent;
    }

    public Stop getUltimateParent() {
        Stop result = this;
        while (result.getParent() != null) {
            result = result.getParent();
        }
        return result;
    }

    private Stop getParent() {
        return parent;
    }

    public void addChildStop(Stop stop) {
        if (!childStops.contains(stop)) {
            childStops.add(stop);
        }
    }

    public ArrayList<Stop> getFamily() {
        ArrayList<Stop> result = new ArrayList<Stop>(childStops);
        if (parent != null) {
            result.add(parent);
            if (parent.oppositeStop != null) {
                result.add(parent.oppositeStop);
            }
        }
        if (oppositeStop != null) {
            result.add(oppositeStop);
        }
        result.add(this);
        return result;
    }

    public ArrayList<Stop> getChildStops() {
        return childStops;
    }

    public void setValues(String mName, String mLat, String mLng, String mID, String[] mRoutes) {
        if (name.equals("")) name = cleanName(mName);
        if (loc == null) loc = new LatLng(Double.parseDouble(mLat), Double.parseDouble(mLng));
        id = mID;
        if (routesString == null) routesString = mRoutes;
        BusManager sharedManager = BusManager.getBusManager();
        for (String s : mRoutes) {
            Route r = sharedManager.getRouteByID(s);
            if (r != null && !r.getStops().contains(this)) r.addStop(this);
        }
    }

    public LatLng getLocation() {
        return loc;
    }

    public String toString() {
        return name + " " + getID();
    }

    public boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(boolean checked) {
        favorite = checked;
    }

    public boolean hasRouteByString(String routeID) {
        for (String route : routesString) {
            if (route.equals(routeID)) return true;
        }
        return false;
    }
    public boolean hasRouteByName(String name) {
        if (name.trim().isEmpty()) return false;
        for (Route r : routes) {
            if (r.getLongName().equals(name)) return true;
        }
        return false;
    }

    public ArrayList<Route> getRoutes() {
        ArrayList<Route> result = new ArrayList<Route>(routes);
        for (Stop child : childStops) {
            for (Route childRoute : child.getRoutes()) {
                if (!result.contains(childRoute)) {
                    result.add(childRoute);
                }
            }
        }
        if (parent != null) {
            for (Stop child : parent.getChildStops()) {
                if (child != this) {
                    for (Route childRoute : child.getRoutes()) {
                        if (!result.contains(childRoute)) {
                            result.add(childRoute);
                        }
                    }
                }
            }
        }
        if (oppositeStop != null) {
            for (Route r : oppositeStop.routes) {
                if (!result.contains(r)) {
                    result.add(r);
                }
            }
            for (Stop child : oppositeStop.getChildStops()) {
                if (child != this) {
                    for (Route childRoute : child.getRoutes()) {
                        if (!result.contains(childRoute)) {
                            result.add(childRoute);
                        }
                    }
                }
            }
        }
        return result;
    }

    public void addRoute(Route route) {
        routes.add(route);
    }

    public String getID() {
        return id;
    }

    public void addTime(Time t) {
        times.add(t);
    }

    public List<Time> getTimes() {
        return times;
    }

    public ArrayList<Time> getTimesOfRoute(String route) {
        ArrayList<Time> result = new ArrayList<Time>();
        for (Time t : times) {
            if (t.getRoute().equals(route)) {
                result.add(t);
            }
        }
        for (Stop childStop : childStops) {
            result.addAll(childStop.getTimesOfRoute(route));
        }
        return result;
    }

    public boolean isRelatedTo(Stop stop) {
        return (this.getUltimateName().equals(stop.getUltimateName()));
    }

    public String getUltimateName() {
        return getUltimateParent().getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Route> getRoutesTo(Stop endStop) {
        Stop startStop = this;
        ArrayList<Route> startRoutes = startStop.getUltimateParent().getRoutes();        // All the routes leaving the start stop.
        ArrayList<Route> endRoutes = endStop.getUltimateParent().getRoutes();
        ArrayList<Route> availableRoutes = new ArrayList<Route>();               // All the routes connecting the two.
        for (Route r : startRoutes) {
            if (endRoutes.contains(r) && !availableRoutes.contains(r)) {
                availableRoutes.add(r);
            }
        }
        return availableRoutes;
    }

    public List<Time> getTimesTo(Stop endStop){
        List<Time> result = new ArrayList<Time>();
        for (Time t : times) {
            if (endStop.hasRouteByName(t.getRoute())) {
                result.add(t);
            }
        }
        Collections.sort(result);
        return result;
    }

    public boolean isConnectedTo(Stop stop) {
        return stop == null || !this.getRoutesTo(stop).isEmpty();
    }

    public int distanceTo(Stop stop) {
        if (stop == null) return Integer.MAX_VALUE;
        List<Route> routes = getRoutesTo(stop);
        if (routes.isEmpty()) return Integer.MAX_VALUE;
        else {
            Route r = routes.get(0);
            List<Stop> stops = r.getStops();
            for (int i = 0; i < stops.size(); i++) {
                if (stops.get(i).getID().equals(getID())) {
                    for (int j = 1; ((i + j) % stops.size()) != i; j++) {
                        if (stops.get((i + j) % stops.size()).getID().equals(stop.getID())) {
                            return j;
                        }
                    }
                }
            }
        }
        return Integer.MAX_VALUE;
    }
}
