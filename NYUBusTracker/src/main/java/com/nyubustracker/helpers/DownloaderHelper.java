package com.nyubustracker.helpers;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public interface DownloaderHelper {
    String TRANSLOC_URL = "https://transloc-api-1-2.p.mashape.com";
    String QUERY = Downloader.makeQuery("agencies", "72", "UTF-8");
    String STOPS_URL = TRANSLOC_URL + "/stops.json?" + QUERY;
    String ROUTES_URL = TRANSLOC_URL + "/routes.json?" + QUERY;
    String SEGMENTS_URL = TRANSLOC_URL + "/segments.json?" + QUERY;
    String VEHICLES_URL = TRANSLOC_URL + "/vehicles.json?" + QUERY;
    String AMAZON_URL = "https://s3.amazonaws.com/nyubustimes/1.0/";
    String VERSION_URL = AMAZON_URL + "version.json";

    void parse(JSONObject jsonObject) throws JSONException, IOException;
}
