package com.nyubustracker.helpers;

import android.util.Log;

import com.nyubustracker.BuildConfig;
import com.nyubustracker.activities.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class TimeDownloaderHelper implements DownloaderHelper {
    @Override
    public void parse(JSONObject jsonObject) throws JSONException, IOException {
        if (jsonObject != null && jsonObject.toString().length() > 0) {
            BusManager.parseTime(jsonObject);
            if (BuildConfig.DEBUG) {
                Log.v(MainActivity.LOG_TAG, "Creating time cache file: " + jsonObject.getString("stop_id"));
                Log.v(MainActivity.LOG_TAG, "*   result: " + jsonObject.toString());
            }
            Downloader.cache(jsonObject.getString("stop_id"), jsonObject);
        } else {
            throw new JSONException(jsonObject == null
                    ? "TimeDownloaderHelper#parse given null jsonObject"
                    : "TimeDownloaderHelper#parse given empty jsonObject");
        }
    }
}