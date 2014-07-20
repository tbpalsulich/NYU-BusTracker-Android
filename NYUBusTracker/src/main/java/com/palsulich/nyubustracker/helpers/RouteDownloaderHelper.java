package com.palsulich.nyubustracker.helpers;

import com.palsulich.nyubustracker.models.Route;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class RouteDownloaderHelper implements DownloaderHelper {
    public static final String ROUTE_JSON_FILE = "routeJson";

    @Override
    public void parse(JSONObject jsonObject) throws JSONException, IOException {
        Route.parseJSON(jsonObject);
        Downloader.cache(ROUTE_JSON_FILE, jsonObject);
    }

    public String getUrl() {
        return ROUTES_URL;
    }
}