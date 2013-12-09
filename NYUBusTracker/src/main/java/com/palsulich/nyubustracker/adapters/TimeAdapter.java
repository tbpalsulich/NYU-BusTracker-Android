package com.palsulich.nyubustracker.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.palsulich.nyubustracker.R;
import com.palsulich.nyubustracker.models.Time;

import java.util.ArrayList;

public class TimeAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private ArrayList<Time> times;

    public TimeAdapter(Context context, ArrayList<Time> mTimes){
        // Cache the LayoutInflate to avoid asking for a new one each time.
        mInflater = LayoutInflater.from(context);
        times = mTimes;
    }

    public int getCount() {
        return times.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // if something we pick a different layout to inflate.
        convertView = mInflater.inflate(R.layout.time_list_item, null);
        TextView t = (TextView) convertView.findViewById(R.id.time_text);
        t.setText(times.get(position).toString() + times.get(position).getViaRoute());
        return convertView;
    }
}