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

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class TimeAdapter extends BaseAdapter implements StickyListHeadersAdapter{

    private LayoutInflater inflater;
    private ArrayList<Time> times;

    public TimeAdapter(Context context, ArrayList<Time> mTimes){
        // Cache the LayoutInflate to avoid asking for a new one each time.
        inflater = LayoutInflater.from(context);
        times = mTimes;
    }

    public int getCount() {
        return times.size();
    }

    public Object getItem(int position) {
        return times.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // if (something) we pick a different layout to inflate.
        convertView = inflater.inflate(R.layout.time_list_item, null);
        TextView t = (TextView) convertView.findViewById(R.id.time_text);
        t.setText(times.get(position).toString());
        return convertView;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (convertView == null) {
            holder = new HeaderViewHolder();
            convertView = inflater.inflate(R.layout.time_list_header, parent, false);
            holder.text = (TextView) convertView.findViewById(R.id.time_text);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }
        //set header text as first char in name
        String headerText = times.get(position).getTimeOfWeekAsString();
        holder.text.setText(headerText);
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        //return the first character of the country as ID because this is what headers are based upon
        return times.get(position).getTimeOfWeek().ordinal();
    }

    class HeaderViewHolder {
        TextView text;
    }

    class ViewHolder {
        TextView text;
    }
}