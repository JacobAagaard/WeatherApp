package com.flonk.weatherapp;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import static com.flonk.weatherapp.Globals.CITY_WEATHER_NAME;
import static com.flonk.weatherapp.Globals.RESULT_CODE_REMOVE;

public class CityDetailsActivity extends AppCompatActivity {

    Button buttonOK, buttonRemove;
    TextView textViewCityName, textViewHumidity, textViewWeatherDescription, textViewTemperature;
    ImageView imvIcon;

    private WeatherService.WeatherServiceBinder weatherServiceBinder;
    private boolean isBoundToWeatherService = false;

    AllCitiesWeather allCitiesWeather;
    CityWeatherData currentData;

    String cityName;

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
                if(isBoundToWeatherService){
                    weatherServiceBinder.RemoveCity(cityName);
                }
                else
                {
                    Toast.makeText(CityDetailsActivity.this, "NOT BOUND", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(getIntent().getBooleanExtra(Globals.CITY_DETAIL_ACTIVITY_STARTED_FROM_SERVICE, false)){
                    Intent intent = new Intent(CityDetailsActivity.this, CityListActivity.class);
                    //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(Globals.CITY_LIST_ACTIVITY_STARTED_FROM_CITY_DETIAL_ACTIVITY, true);
                    startActivity(intent);
                }

                setResult(RESULT_CODE_REMOVE);
                //setIntent(new Intent().putExtra(CITY_WEATHER_NAME, cityName));
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        Intent weatherServiceIntent = new Intent(CityDetailsActivity.this, WeatherService.class);

        if(!isMyServiceRunning(WeatherService.class)){
            startService(weatherServiceIntent);
            Log.d("CityDetailActivity", "Service was not already running!");
        }
        else{
            Log.d("CityDetailActivity", "Service was already running!");
        }

        if(!isBoundToWeatherService){
            bindService(weatherServiceIntent,serviceConnection, Context.BIND_AUTO_CREATE);
        }

        // gets the name of the game: ie. the city name of the context the detail view was opened with.
        cityName = getIntent().getStringExtra(Globals.CITY_WEATHER_NAME);

        super.onResume();
    }

    @Override
    protected void onPause() {
        if(isBoundToWeatherService){
            unbindService(serviceConnection);
        }
        super.onPause();
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            weatherServiceBinder = (WeatherService.WeatherServiceBinder) iBinder;
            isBoundToWeatherService = true;

            setupUIWithServiceData();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBoundToWeatherService = false;
        }
    };

    private void setupUIWithServiceData(){
        allCitiesWeather = weatherServiceBinder.getAllCitiesWeather();
        CityWeatherData currentData = allCitiesWeather.GetCityWeatherData(cityName);
        textViewCityName.setText(currentData.Name);
        textViewHumidity.setText(currentData.Humidity);
        textViewTemperature.setText(currentData.Temperature);
        textViewWeatherDescription.setText(currentData.Description);
        imvIcon.setImageResource(setIcon(currentData.Icon));
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

    // from : https://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-on-android
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
