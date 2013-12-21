package com.palsulich.nyubustracker.helpers;

import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class FileGrabber {
    private static final String charset = "UTF-8";
    private static final String agencies = "72";
    private static final String query = makeQuery("agencies", agencies, charset);

    public static final String TAG_DATA = "data";
    public static final String TAG_LONG_NAME = "long_name";
    public static final String TAG_LOCATION = "location";
    public static final String TAG_LAT = "lat";
    public static final String TAG_LNG = "lng";
    public static final String TAG_HEADING = "heading";
    public static final String TAG_STOP_NAME = "name";
    public static final String TAG_STOP_ID = "stop_id";
    public static final String TAG_ROUTES = "routes";
    public static final String TAG_ROUTE = "route";
    public static final String TAG_ROUTE_ID = "route_id";
    public static final String TAG_WEEKDAY = "Weekday";
    public static final String TAG_FRIDAY = "Friday";
    public static final String TAG_WEEKEND = "Weekend";
    public static final String TAG_VEHICLE_ID = "vehicle_id";
    public static final String TAG_SEGMENTS = "segments";
    public static final String TAG_STOPS = "stops";

    private static final String stopsURL = "http://api.transloc.com/1.2/stops.json?" + query;
    private static final String routesURL = "http://api.transloc.com/1.2/routes.json?" + query;
    private static final String segmentsURL = "http://api.transloc.com/1.2/segments.json?" + query;
    private static final String vehiclesURL = "http://api.transloc.com/1.2/vehicles.json?" + query;
    private static final String versionURL = "https://s3.amazonaws.com/nyubustimes/1.0/version.json";
    private static final String timesURL = "https://s3.amazonaws.com/nyubustimes/1.0/";


    private static final String stopsFileName = "stopsJSON";
    private static final String routesFileName = "routesJSON";
    private static final String vehiclesFileName = "vehiclesJSON";
    private static final String versionFileName = "versionJSON";
    private static final String segmentsFileName = "segmentsJSON";

    private static final String FROM_STOP_FILE_NAME = "fromStop";
    private static final String TO_STOP_FILE_NAME = "toStop";


    private static String makeQuery(String param, String value, String charset) {
        try {
            return String.format(param + "=" + URLEncoder.encode(value, charset));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }


    File cacheDir;
    // Creating JSON Parser instance
    JSONParser jParser;
    File[] files;




    public FileGrabber(File mCacheDir){
        jParser = new JSONParser();
        cacheDir = mCacheDir;
        files = cacheDir.listFiles();
    }

    private String getFile(String fileName){
        if(files != null){
            for(File f : files){
                if(f.isFile() && f.getName().equals(fileName)){
                    try {
                    String currentLine;
                    StringBuilder builder = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new FileReader(f));

                    while ((currentLine = reader.readLine()) != null) {
                        builder.append(currentLine);
                    }
                    return builder.toString();
                    } catch (FileNotFoundException e){
                        Log.e("JSON Parser", "File not found: " + f.toString());
                    } catch (IOException e){
                        Log.e("JSON Parser", "IO Exception: " + f.toString());
                    }
                }
            }
        }
        return "";
    }

    private void put(String content, String fileName){
        File file = new File(cacheDir, fileName);
        try {
            Log.v("JSONDebug", "Creating/putting a new cache file (" + fileName + ").");
            file.createNewFile();
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONObject getJSON(String url, String cacheFile, boolean doNotCache, NetworkInfo networkInfo){
        if(!doNotCache){
            if(files != null){
                for(File f : files){
                    if(f.isFile() && f.getName().equals(cacheFile)){
                        Log.v("JSONDebug", "Loading file " + cacheFile + " from cache.");
                        // try parse the string to a JSON object
                        JSONObject jObj = null;
                        try {
                            String currentLine;
                            StringBuilder builder = new StringBuilder();
                            BufferedReader reader = new BufferedReader(new FileReader(f));

                            while ((currentLine = reader.readLine()) != null) {
                                builder.append(currentLine);
                            }
                            jObj = new JSONObject(builder.toString());
                        } catch (JSONException e) {
                            Log.e("JSON Parser", f.toString());
                            Log.e("JSON Parser", "Error parsing data " + e.toString());
                        } catch (FileNotFoundException e){
                            Log.e("JSON Parser", "File not found: " + f.toString());
                        } catch (IOException e){
                            Log.e("JSON Parser", "IO Exception: " + f.toString());
                        }
                        return jObj;
                    }
                }
            }
            if(networkInfo != null && networkInfo.isConnected()){
                File file = new File(cacheDir, cacheFile);
                JSONObject jObj = jParser.getJSONFromUrl(url);
                try {
                    Log.v("JSONDebug", "Creating a new cache file (" + cacheFile + ").");
                    file.createNewFile();
                    FileWriter fw = new FileWriter(file);
                    BufferedWriter bw = new BufferedWriter(fw);
                    bw.write(jObj.toString());
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return jObj;
            }
        }
        else if (networkInfo != null && networkInfo.isConnected())
            return jParser.getJSONFromUrl(url);
        return null;
    }

    public JSONObject getTimesFromFile(String file, NetworkInfo networkInfo){
        return getJSON(timesURL + file, file, false, networkInfo);
    }

    public JSONObject getSegmentsJSON(NetworkInfo networkInfo){
        return getJSON(segmentsURL, segmentsFileName, false, networkInfo);
    }

    public JSONObject getStopJSON(NetworkInfo networkInfo){
        return getJSON(stopsURL, stopsFileName, false, networkInfo);
    }

    public JSONObject getRouteJSON(NetworkInfo networkInfo){
        return getJSON(routesURL, routesFileName, false, networkInfo);
    }

    public JSONObject getVersionJSON(NetworkInfo networkInfo){
        return getJSON(versionURL, versionFileName, false, networkInfo);
    }

    public JSONObject getVehicleJSON(NetworkInfo networkInfo){
        return getJSON(vehiclesURL, vehiclesFileName, true, networkInfo);
    }

    public String getStartStopFile(){
        String result = getFile(FROM_STOP_FILE_NAME);
        if (result.equals("")) return "715 Broadway @ Washington Square";
        else return result;
    }

    public void setStartStop(String stop){
        put(stop, FROM_STOP_FILE_NAME);
    }

    public String getEndStopFile(){
        String result = getFile(TO_STOP_FILE_NAME);
        if (result.equals("")) return "80 Lafayette St";
        else return result;
    }

    public void setEndStop(String stop){
        put(stop, TO_STOP_FILE_NAME);
    }

}
