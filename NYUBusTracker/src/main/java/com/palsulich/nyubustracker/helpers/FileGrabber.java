package com.palsulich.nyubustracker.helpers;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileGrabber {
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

    private static final String FROM_STOP_FILE_NAME = "fromStop";
    private static final String TO_STOP_FILE_NAME = "toStop";


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
                        Log.v("Volley Debugging", "Found file: " + builder.toString());
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

    public String getStartStopFile(){
        String result = getFile(FROM_STOP_FILE_NAME);
        if (result.equals("")){
            Log.v("Volley Debugging", "Default start stop");
            return "715 Broadway at Washington Square";
        }
        else return result;
    }

    public void setStartStop(String stop){
        put(stop, FROM_STOP_FILE_NAME);
    }

    public String getEndStopFile(){
        String result = getFile(TO_STOP_FILE_NAME);
        if (result.equals("")) return "80 Lafayette Street";
        else return result;
    }

    public void setEndStop(String stop){
        put(stop, TO_STOP_FILE_NAME);
    }

}
