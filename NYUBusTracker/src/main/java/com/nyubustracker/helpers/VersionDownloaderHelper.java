package com.nyubustracker.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.nyubustracker.BuildConfig;
import com.nyubustracker.activities.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class VersionDownloaderHelper implements DownloaderHelper {
    public static final String VERSION_JSON_FILE = "versionJson";
    private static final String TIME_VERSION_PREF = "stopVersions";

    @Override
    public void parse(JSONObject jsonObject) throws JSONException, IOException {

        BusManager sharedManager = BusManager.getBusManager();
        BusManager.parseVersion(jsonObject);
        Downloader.cache(VERSION_JSON_FILE, jsonObject);
        for (String timeURL : sharedManager.getTimesToDownload()) {
            SharedPreferences preferences = Downloader.getContext().getSharedPreferences(TIME_VERSION_PREF, Context.MODE_PRIVATE);
            String stopID = timeURL.substring(timeURL.lastIndexOf("/") + 1, timeURL.indexOf(".json"));
            if (BuildConfig.DEBUG)
                Log.v(MainActivity.REFACTOR_LOG_TAG, "Time to download: " + stopID);
            int newestStopTimeVersion = sharedManager.getTimesVersions().get(stopID);
            if (preferences.getInt(stopID, 0) != newestStopTimeVersion) {
                MainActivity.downloadsOnTheWire++;
                if (BuildConfig.DEBUG)
                    Log.v(MainActivity.REFACTOR_LOG_TAG, "*   Actually downloading it!");
                new Downloader(new TimeDownloaderHelper(), Downloader.getContext()).execute(timeURL);
                preferences.edit().putInt(stopID, newestStopTimeVersion).apply();
            } else if (BuildConfig.DEBUG)
                Log.v(MainActivity.REFACTOR_LOG_TAG, "*   Not actually downloading it, because we already have the current version.");
        }
    }
}