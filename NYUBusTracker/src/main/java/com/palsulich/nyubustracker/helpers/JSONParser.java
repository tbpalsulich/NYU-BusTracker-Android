package com.palsulich.nyubustracker.helpers;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class JSONParser {
    static InputStream is = null;
    static JSONObject jObj = null;
    static String json = "";

    private class Downloader extends AsyncTask<String, Integer, JSONObject>{
        @Override
        public JSONObject doInBackground(String... urls){
            for(String mURL : urls){
                InputStream is = null;
                // Making HTTP request
                try {
                    Log.v("JSONDebug", "Request URL" + mURL);
                    is = new URL(mURL).openStream();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            is, "iso-8859-1"), 8);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                        sb.append("\n");
                    }
                    is.close();
                    json = sb.toString();
                    Log.v("JSON", json);
                } catch (Exception e) {
                    Log.e("Buffer Error", "Error converting result " + e.toString());
                }

                // try parse the string to a JSON object
                try {
                    jObj = new JSONObject(json);
                } catch (JSONException e) {
                    Log.e("JSON Parser", "Error parsing data " + e.toString());
                }
                return jObj;
            }
            return null;
        }
    }

    // constructor
    public JSONParser() {

    }

    public JSONObject getJSONFromUrl(String url) {

        try {
            return new Downloader().execute(url).get();
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
        catch (ExecutionException e){
            e.printStackTrace();
        }
        return null;
    }
}