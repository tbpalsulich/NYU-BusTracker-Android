package com.palsulich.nyubustracker.helpers;

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

/**
 * Created by tyler on 10/4/13.
 */
public class FileGrabber {
    String charset = "UTF-8";
    String agencies = "72";
    String query = makeQuery("agencies", agencies, charset);

    private String stopsURL = "http://api.transloc.com/1.2/stops.json?" + query;
    private String routesURL = "http://api.transloc.com/1.2/routes.json?" + query;
    private String segmentsURL = "http://api.transloc.com/1.2/segments.json?" + query;
    private String vehiclesURL = "http://api.transloc.com/1.2/vehicles.json?" + query;
    private String versionURL = "https://s3.amazonaws.com/nyubustimes/1.0/version.json";
    private static String timesURL = "https://s3.amazonaws.com/nyubustimes/1.0/";


    private String stopsFileName = "stopsJSON";
    private String routesFileName = "routesJSON";
    private String vehiclesFileName = "vehiclesJSON";
    private String versionFileName = "versionJSON";
    private String segmentsFileName = "segmentsJSON";

    private static final String FROM_STOP_FILE_NAME = "fromStop";
    private static final String TO_STOP_FILE_NAME = "toStop";


    private String makeQuery(String param, String value, String charset) {
        try {
            return String.format(param + "=" + URLEncoder.encode(agencies, charset));
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
            for(int i = 0; i < files.length; i++){
                if(files[i].isFile() && files[i].getName().equals(fileName)){
                    try {
                    String currentLine;
                    StringBuilder builder = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new FileReader(files[i]));

                    while ((currentLine = reader.readLine()) != null) {
                        builder.append(currentLine);
                    }
                    return builder.toString();
                    } catch (FileNotFoundException e){
                        Log.e("JSON Parser", "File not found: " + files[i].toString());
                    } catch (IOException e){
                        Log.e("JSON Parser", "IO Exception: " + files[i].toString());
                    }
                }
            }
        }
        return "";
    }

    private void put(String content, String fileName){
        File file = new File(cacheDir, fileName);
        try {
            Log.v("JSONDebug", "Creating a new cache file.");
            file.createNewFile();
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONObject getJSON(String url, String cacheFile){
        if(files != null){
            for(int i = 0; i < files.length; i++){
                if(files[i].isFile() && files[i].getName().equals(cacheFile)){
                    Log.v("JSONDebug", "Loading file from cache.");
                    // try parse the string to a JSON object
                    JSONObject jObj = null;
                    try {
                        String json = "";
                        String currentLine;
                        StringBuilder builder = new StringBuilder();
                        BufferedReader reader = new BufferedReader(new FileReader(files[i]));

                        while ((currentLine = reader.readLine()) != null) {
                            builder.append(currentLine);
                        }
                        jObj = new JSONObject(builder.toString());
                    } catch (JSONException e) {
                        Log.e("JSON Parser", files[i].toString());
                        Log.e("JSON Parser", "Error parsing data " + e.toString());
                    } catch (FileNotFoundException e){
                        Log.e("JSON Parser", "File not found: " + files[i].toString());
                    } catch (IOException e){
                        Log.e("JSON Parser", "IO Exception: " + files[i].toString());
                    }
                    return jObj;
                }
            }
        }
        File file = new File(cacheDir, cacheFile);
        JSONObject jObj = jParser.getJSONFromUrl(url);
        try {
            Log.v("JSONDebug", "Creating a new cache file.");
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

    public JSONObject getTimesFromFile(String file){
        return getJSON(timesURL + file, file);
    }

    public JSONObject getSegmentsJSON(){
        return getJSON(segmentsURL, segmentsFileName);
    }

    public JSONObject getStopJSON(){
        return getJSON(stopsURL, stopsFileName);
    }

    public JSONObject getRouteJSON(){
        return getJSON(routesURL, routesFileName);
    }

    public JSONObject getVersionJSON(){
        return getJSON(versionURL, versionFileName);
    }

    public JSONObject getVehicleJSON(){
        return getJSON(vehiclesURL, vehiclesFileName);
    }

    public String getStartStopFile(){
        return getFile(FROM_STOP_FILE_NAME);
    }

    public void setStartStop(String stop){
        put(stop, FROM_STOP_FILE_NAME);
    }

    public String getEndStopFile(){
        return getFile(TO_STOP_FILE_NAME);
    }

    public void setEndStop(String stop){
        put(stop, TO_STOP_FILE_NAME);
    }

}
