package com.nyubustracker.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.nyubustracker.R;
import com.nyubustracker.models.Time;

import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class TimeAdapter extends BaseAdapter implements StickyListHeadersAdapter {

    private final LayoutInflater inflater;
    private final Context context;
    private List<Time> times;
    private Time currentTime;

    public TimeAdapter(Context context, ArrayList<Time> mTimes) {
        // Cache the LayoutInflate to avoid asking for a new one each time.
        inflater = LayoutInflater.from(context);
        times = mTimes;
        this.context = context;
    }

    public void setDataSet(List<Time> mTimes) {
        times = mTimes;
    }

    public void setTime(Time currentTime) {
        this.currentTime = currentTime;
    }

    public int getCount() {
        return times.size();
    }

    public Object getItem(int position) {
        position = Math.min(position, times.size() - 1);
        return times.get(position);
    }

    public long getItemId(int position) {
        position = Math.min(position, times.size() - 1);
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        position = Math.min(position, times.size() - 1);
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.time_list_item, parent, false);
            if (convertView != null) {
                viewHolder = new ViewHolder();
                viewHolder.timeText = (TextView) convertView.findViewById(R.id.time_text);
                viewHolder.viaRouteText = (TextView) convertView.findViewById(R.id.route_text);
                convertView.setTag(viewHolder);
            } else return new View(null);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        if (viewHolder.timeText != null) {
            Time thisTime = times.get(position);
            viewHolder.timeText.setText(thisTime.toString() +
                    ((currentTime.getTimeAsTimeUntil(thisTime).compareTo(new Time(1, 0)) <= 0 &&
                            currentTime.getTimeOfWeek() == thisTime.getTimeOfWeek())
                            ? " (" + currentTime.getTimeAsStringUntil(thisTime, context.getResources()) + ")"
                            : ""));
        }
        if (viewHolder.viaRouteText != null) {
            String[] routeArray = times.get(position).getRoute().split("\\s");
            String route = times.get(position).getRoute();
            if (routeArray[0].length() == 1) {
                viewHolder.viaRouteText.setText("Route " + route);
            } else {
                viewHolder.viaRouteText.setText(route);
            }
        }
        return convertView;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        position = Math.min(position, times.size() - 1);
        HeaderViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.time_list_header, parent, false);
            if (convertView != null) {
                holder = new HeaderViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.time_text);
                convertView.setTag(holder);
            } else return new View(null); // Should never reach here.
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }
        // Set the header text to the time of week of this chunk of times.
        String headerText = times.get(position).getTimeOfWeekAsString();
        holder.text.setText(headerText);
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        position = Math.min(position, times.size() - 1);
        return times.get(position).getTimeOfWeek().ordinal();
    }

    class HeaderViewHolder {
        TextView text;
    }

    class ViewHolder {
        TextView timeText;
        TextView viaRouteText;
    }
}