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

/**
 * Created by tyler on 10/4/13.
 */
public class FileGrabber {
    File cacheDir;
    // Creating JSON Parser instance
    JSONParser jParser;
    File[] files;

    public FileGrabber(File mCacheDir){
        jParser = new JSONParser();
        cacheDir = mCacheDir;
        files = cacheDir.listFiles();
    }

    public String getFile(String fileName){
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

    public void put(String content, String fileName){
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

    public JSONObject getJSON(String url, String cacheFile){
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
}
