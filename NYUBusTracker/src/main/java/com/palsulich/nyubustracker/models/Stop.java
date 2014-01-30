package com.palsulich.nyubustracker.models;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.palsulich.nyubustracker.helpers.BusManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;

public class Stop {
    String name, id;
    LatLng loc;
    String[] routesString;
    ArrayList<Route> routes = null;
    ArrayList<Time> times = null;
    boolean favorite;
    public static String FAVORITES_PREF = "favorites";
    ArrayList<Stop> childStops;
    Stop parent;
    Stop oppositeStop;
    boolean hidden;

    public Stop(String mName, String mLat, String mLng, String mID, String[] mRoutes){
        name = cleanName(mName);
        loc = new LatLng(Double.parseDouble(mLat), Double.parseDouble(mLng));
        id = mID;
        routesString = mRoutes;
        times = new ArrayList<Time>();
        routes = new ArrayList<Route>();
        childStops = new ArrayList<Stop>();
        BusManager sharedManager = BusManager.getBusManager();
        for (String s : mRoutes){
            Route r = sharedManager.getRouteByID(s);
            if (r != null && !r.getStops().contains(this)) r.addStop(this);
        }
    }

    public static String cleanName(String name){
        name = name.replaceAll("at", "@");
        name = name.replaceAll("[Aa]venue", "Ave");
        name = name.replaceAll("bound", "");
        name = name.replaceAll("[Ss]treet", "St");
        return name;
    }

    public void setOppositeStop(Stop stop){
        oppositeStop = stop;
    }

    public Stop getOppositeStop(){
        return oppositeStop;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setHidden(boolean hidden){
        this.hidden = hidden;
    }

    public boolean isHidden(){
        return hidden;
    }

    public void setParentStop(Stop parent){
        this.parent = parent;
    }

    public Stop getParent(){
        return parent;
    }

    public Stop getUltimateParent(){
        Stop result = this;
        while (result.getParent() != null){
            result = result.getParent();
        }
        return result;
    }

    public void addChildStop(Stop stop){
        if (!childStops.contains(stop)){
            childStops.add(stop);
        }
    }

    public String getUltimateName(){
        Stop s = this;
        while (s.getParent() != null){
            s = s.getParent();
        }
        return s.getName();
    }

    public ArrayList<Stop> getFamily(){
        ArrayList<Stop> result = new ArrayList<Stop>(childStops);
        if (parent != null){
            result.add(parent);
            if (parent.oppositeStop != null){
                result.add(parent.oppositeStop);
            }
        }
        if (oppositeStop != null){
            result.add(oppositeStop);
        }
        result.add(this);
        return result;
    }

    public ArrayList<Stop> getChildStops(){
        return childStops;
    }

    public void setFavorite(SharedPreferences preferences){
        favorite = preferences.getBoolean(id, false);
    }

    public void setValues(String mName, String mLat, String mLng, String mID, String[] mRoutes){
        if(name.equals("")) name = cleanName(mName);
        if(loc == null) loc = new LatLng(Double.parseDouble(mLat), Double.parseDouble(mLng));
        id = mID;
        if(routesString == null) routesString = mRoutes;
        BusManager sharedManager = BusManager.getBusManager();
        for (String s : mRoutes){
            Route r = sharedManager.getRouteByID(s);
            if (r != null && !r.getStops().contains(this)) r.addStop(this);
        }
    }

    public LatLng getLocation(){
        return loc;
    }

    public String getName(){
        return name;
    }

    public String toString(){
        return name;
    }

    public boolean getFavorite(){
        return favorite;
    }

    public void setFavorite(boolean checked){
        favorite = checked;
    }

    public boolean hasRouteByString(String routeID){
        for (String route : routesString){
            if (route.equals(routeID)) {
                return true;
            }
        }
        return false;
    }
    public ArrayList<Route> getRoutes(){
        ArrayList<Route> result = new ArrayList<Route>(routes);
        for (Stop child : childStops){
            for (Route childRoute : child.getRoutes()){
                if (!result.contains(childRoute)){
                    result.add(childRoute);
                }
            }
        }
        if (parent != null){
            for (Stop child : parent.getChildStops()){
                if (child != this){
                    for (Route childRoute : child.getRoutes()){
                        if (!result.contains(childRoute)){
                            result.add(childRoute);
                        }
                    }
                }
            }
        }
        if (oppositeStop != null){
            for (Route r : oppositeStop.routes){
                if (!result.contains(r)){
                    result.add(r);
                }
            }
            for (Stop child : oppositeStop.getChildStops()){
                if (child != this){
                    for (Route childRoute : child.getRoutes()){
                        if (!result.contains(childRoute)){
                            result.add(childRoute);
                        }
                    }
                }
            }
        }
        return result;
    }
    public void addRoute(Route route){
        routes.add(route);
    }

    public String getID(){
        return id;
    }

    public void addTime(Time t){
        times.add(t);
    }

    public static Comparator<Stop> compare = new Comparator<Stop>() {
        @Override
        public int compare(Stop stop, Stop stop2) {
            if (stop.getFavorite()){
                if (stop2.getFavorite()){
                    return Integer.signum(stop.getName().compareTo(stop2.getName()));
                }
                else return -1;
            }
            else if(stop2.getFavorite()) return 1;
            else return Integer.signum(stop.getName().compareTo(stop2.getName()));
        }
    };

    public ArrayList<Time> getTimesOfRoute(String route){
        ArrayList<Time> result = new ArrayList<Time>();
        for (Time t : times){
            if (t.getRoute().equals(route)){
                result.add(t);
            }
        }
        for (Stop childStop : childStops){
            result.addAll(childStop.getTimesOfRoute(route));
        }
        return result;
    }

    public boolean isRelatedTo(Stop stop){
        boolean result = (this.getUltimateName().equals(stop.getUltimateName()));
        if (result){
            return true;
        }
        else{
            //Log.v("Combine Debugging", this + " is not related to " + stop);
            return false;
        }
    }

    public static void parseJSON(JSONObject stopsJson) throws JSONException{
        JSONArray jStops = new JSONArray();
        BusManager sharedManager = BusManager.getBusManager();
        if (stopsJson != null) jStops = stopsJson.getJSONArray(BusManager.TAG_DATA);
        //Log.v("JSONDebug", "Number of stops: " + jStops.length());
        for (int i = 0; i < jStops.length(); i++) {
            JSONObject stopObject = jStops.getJSONObject(i);
            String stopID = stopObject.getString(BusManager.TAG_STOP_ID);
            String stopName = stopObject.getString(BusManager.TAG_STOP_NAME);
            JSONObject location = stopObject.getJSONObject(BusManager.TAG_LOCATION);
            String stopLat = location.getString(BusManager.TAG_LAT);
            String stopLng = location.getString(BusManager.TAG_LNG);
            JSONArray stopRoutes = stopObject.getJSONArray(BusManager.TAG_ROUTES);
            String[] routes = new String[stopRoutes.length()];
            String routesString = "";
            for (int j = 0; j < stopRoutes.length(); j++) {
                routes[j] = stopRoutes.getString(j);
                routesString += routes[j];
                if (j != stopRoutes.length() - 1) routesString += ", ";
            }
            Stop s = sharedManager.getStop(stopName, stopLat, stopLng, stopID, routes);
            sharedManager.addStop(s);
            //Log.v("JSONDebug", "Stop name: " + stopName + ", stop ID: " + stopID + ", routes: " + routesString);
        }
    }
}
