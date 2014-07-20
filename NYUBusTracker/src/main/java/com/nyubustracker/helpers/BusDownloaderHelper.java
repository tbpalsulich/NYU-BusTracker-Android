package com.nyubustracker.helpers;

import com.nyubustracker.models.Bus;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class BusDownloaderHelper implements DownloaderHelper {
    @Override
    public void parse(JSONObject jsonObject) throws JSONException, IOException {
        Bus.parseJSON(jsonObject);
    }
}