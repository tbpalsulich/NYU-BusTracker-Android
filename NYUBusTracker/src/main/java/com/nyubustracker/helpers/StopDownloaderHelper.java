package com.nyubustracker.helpers;

import com.nyubustracker.models.Stop;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class StopDownloaderHelper implements DownloaderHelper {
    public static final String STOP_JSON_FILE = "stopJson";

    @Override
    public void parse(JSONObject jsonObject) throws JSONException, IOException {
        Stop.parseJSON(jsonObject);
        Downloader.cache(STOP_JSON_FILE, jsonObject);
    }
}
