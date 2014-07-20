package com.nyubustracker.helpers;

import android.util.Log;

import com.nyubustracker.activities.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TimeDownloaderHelper implements DownloaderHelper {
    @Override
    public void parse(JSONObject jsonObject) throws JSONException, IOException {
        BusManager.parseTime(jsonObject);
        if (jsonObject != null && jsonObject.length() > 0) {
            if (MainActivity.LOCAL_LOGV)
                Log.v(MainActivity.REFACTOR_LOG_TAG, "Creating time cache file: " + jsonObject.getString("stop_id"));

            File path = new File(Downloader.getContext().getFilesDir(), Downloader.CREATED_FILES_DIR);
            if (path.mkdir()) {
                File file = new File(path, jsonObject.getString("stop_id"));
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                bufferedWriter.write(jsonObject.toString());
                bufferedWriter.close();
            }
        }
    }
}