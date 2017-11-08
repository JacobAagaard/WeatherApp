package com.flonk.weatherapp;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.lang.reflect.Method;

public class CityDetailsActivity extends AppCompatActivity {

    Button buttonOK, buttonRemove;
    TextView textViewCityName, textViewHumidity, textViewWeatherDescription, textViewTemperature;
    ImageView imvIcon;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city_details);

        buttonOK = findViewById(R.id.buttonOK);
        buttonRemove = findViewById(R.id.buttonRemove);
        textViewCityName = findViewById(R.id.textViewCityName);
        textViewHumidity = findViewById(R.id.textViewHumidity);
        textViewWeatherDescription = findViewById(R.id.textViewWeatherDescription);
        textViewTemperature = findViewById(R.id.textViewTemperature);
        imvIcon = findViewById(R.id.imvIcon);

        Gson gson = new Gson();
        String intentString = getIntent().getStringExtra("City_ID");

        CityWeatherData cityWeatherData = gson.fromJson(intentString, CityWeatherData.class);

        textViewCityName.setText(cityWeatherData.Name);
        textViewHumidity.setText(cityWeatherData.Humidity);
        textViewTemperature.setText(cityWeatherData.Temperature);
        textViewWeatherDescription.setText(cityWeatherData.Description);
        imvIcon.setImageResource(setIcon(cityWeatherData.Icon));

        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_OK);
                finish();
            }
        });

        buttonRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_OK);
                Method[] methods = CityListActivity.class.getDeclaredMethods();
                //Remove based on City ID received from List Activity
            }
        });
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
