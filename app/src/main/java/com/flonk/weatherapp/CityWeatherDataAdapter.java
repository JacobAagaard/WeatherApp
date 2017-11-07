package com.flonk.weatherapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Jacob on 07/11/2017.
 * Inspired by https://github.com/codepath/android_guides/wiki/Using-an-ArrayAdapter-with-ListView
 */

public class CityWeatherDataAdapter extends ArrayAdapter<CityWeatherData> {
    public CityWeatherDataAdapter(@NonNull Context context, int resource, @NonNull List<CityWeatherData> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        CityWeatherData cityWeatherData = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row, parent, false);
        }
        // Lookup view for data population
        TextView tvCityName = convertView.findViewById(R.id.tvCityName);
        TextView tvTemperature = convertView.findViewById(R.id.tvTemperature);
        TextView tvHumidity = convertView.findViewById(R.id.tvHumidity);
        ImageView imvIcon = convertView.findViewById(R.id.imvIcon);

        // Populate the data into the template view using the data object
        tvCityName.setText(cityWeatherData.Name);
        tvTemperature.setText("" + cityWeatherData.Temperature);
        tvHumidity.setText("" + cityWeatherData.Humidity);
        //set imageView based on description
        imvIcon.setImageResource(setIcon(cityWeatherData.Icon));    //

        // Return the completed view to render on screen
        return convertView;
    }

    private int setIcon(String iconID){
        if (iconID.equals("04n"))
            return R.drawable.ic_04n;

        //Returns icon as default
        return R.mipmap.ic_launcher;
    }
}
