package com.flonk.weatherapp;

import android.content.Context;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Jacob on 07/11/2017.
 * Inspired by https://github.com/codepath/android_guides/wiki/Using-an-ArrayAdapter-with-ListView
 */


public class CityWeatherDataAdapter extends ArrayAdapter<CityWeatherData> {

    public CityWeatherDataAdapter(@NonNull Context context, int resource, @NonNull ArrayList<CityWeatherData> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final CityWeatherData cityWeatherData = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row, parent, false);
        }

        // Lookup view for data population
        TextView tvCityName = convertView.findViewById(R.id.tvCityName);
        TextView tvTemperature = convertView.findViewById(R.id.tvTemperature);
        TextView tvHumidity = convertView.findViewById(R.id.tvHumidity);
        final ImageView imvIcon = convertView.findViewById(R.id.imvIcon);


        if(cityWeatherData != null){
            // Populate the data into the template view using the data object
            tvCityName.setText(cityWeatherData.Name);
            tvTemperature.setText(cityWeatherData.Temperature);
            tvHumidity.setText(cityWeatherData.Humidity);
            //set imageView based on description
            imvIcon.setImageResource(setIcon(cityWeatherData.Icon));
            //notifyDataSetChanged(); // Notifies UI thread that content has changed
        }
        else
            Toast.makeText(getContext(), "object is null", Toast.LENGTH_SHORT).show();

        // Return the completed view to render on screen
        return convertView;
    }

    private int setIcon(String iconID){
        switch (iconID){
            case "01d":
                return R.drawable.ic_01d;
            case "01n":
                return R.drawable.ic_01n;
            case "02d":
                return R.drawable.ic_02d;
            case "02n":
                return R.drawable.ic_02n;
            case "03d":
                return R.drawable.ic_03d;
            case "03n":
                return R.drawable.ic_03n;
            case "04d":
                return R.drawable.ic_04d;
            case "04n":
                return R.drawable.ic_04n;
            case "09d":
                return R.drawable.ic_09d;
            case "09n":
                return R.drawable.ic_09n;
            case "10d":
                return R.drawable.ic_10d;
            case "10n":
                return R.drawable.ic_10n;
            case "11d":
                return R.drawable.ic_11d;
            case "11n":
                return R.drawable.ic_11n;
            case "13d":
                return R.drawable.ic_13d;
            case "13n":
                return R.drawable.ic_13n;
            case "50d":
                return R.drawable.ic_50d;
            case "50n":
                return R.drawable.ic_50n;
            default:
                return R.mipmap.ic_launcher;
        }
    }

}
