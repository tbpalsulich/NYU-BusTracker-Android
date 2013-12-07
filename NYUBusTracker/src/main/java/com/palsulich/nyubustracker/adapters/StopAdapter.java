package com.palsulich.nyubustracker.adapters;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.palsulich.nyubustracker.R;
import com.palsulich.nyubustracker.models.Stop;

import java.util.ArrayList;

public class StopAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    ArrayList<Stop> stops = new ArrayList<Stop>();
    boolean startStops;
    Dialog dialog;
    View.OnClickListener textOnClickListener;
    CompoundButton.OnCheckedChangeListener checkBoxOnCLickListener;

    public StopAdapter(Context context, ArrayList<Stop> mStops, boolean mStartStops, Dialog mDialog, View.OnClickListener listener, CompoundButton.OnCheckedChangeListener cbListener){
        textOnClickListener = listener;
        checkBoxOnCLickListener = cbListener;
        // Cache the LayoutInflate to avoid asking for a new one each time.
        mInflater = LayoutInflater.from(context);
        stops = mStops;
        startStops = mStartStops;
        dialog = mDialog;
    }

    public int getCount() {
        return stops.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        convertView = mInflater.inflate(R.layout.stop_list_item, null);
        final ViewHolder holder = new ViewHolder();

        holder.text = (TextView) convertView.findViewById(R.id.stop_text);
        holder.text.setTag(position);
        holder.checkbox = (CheckBox) convertView.findViewById(R.id.stop_checkbox);
        holder.checkbox.setTag(position);

        holder.checkbox.setOnCheckedChangeListener(checkBoxOnCLickListener);

        holder.text.setOnClickListener(textOnClickListener);
        convertView.setTag(holder);
        holder.text.setText(stops.get(position).getName());
        holder.checkbox.setChecked(stops.get(position).getFavorite());
        return convertView;
    }

    class ViewHolder {
        CheckBox checkbox;
        TextView text;
    }
}