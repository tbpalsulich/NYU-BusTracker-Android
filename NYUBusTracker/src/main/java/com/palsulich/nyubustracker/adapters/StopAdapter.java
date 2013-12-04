package com.palsulich.nyubustracker.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.palsulich.nyubustracker.R;
import com.palsulich.nyubustracker.models.Stop;

import java.util.ArrayList;

public class StopAdapter extends BaseAdapter{

        private LayoutInflater mInflater;
        ArrayList<Stop> stops = new ArrayList<Stop>();

        public StopAdapter(Context context, ArrayList<Stop> mStops) {
            // Cache the LayoutInflate to avoid asking for a new one each time.
            mInflater = LayoutInflater.from(context);
            stops = mStops;
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
            final ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.stop_list_item, null);

                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.stop_text);
                holder.checkbox = (CheckBox) convertView.findViewById(R.id.stop_checkbox);

                holder.checkbox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        CheckBox cb = (CheckBox) view;
                        cb.toggle();
                        stops.get(position).setFavorite(cb.isChecked());

                    }
                });
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.text.setText(stops.get(position).getName());
            holder.checkbox.setChecked(stops.get(position).getFavorite());
            return convertView;
        }

        class ViewHolder {
            CheckBox checkbox;
            TextView text;
        }
    }
