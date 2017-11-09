package com.flonk.weatherapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;

import java.nio.channels.NotYetBoundException;

import static com.flonk.weatherapp.Globals.CITY_WEATHER_NAME;

import static com.flonk.weatherapp.Globals.REQUEST_CODE_DETAILS;
import static com.flonk.weatherapp.Globals.RESULT_CODE_REMOVE;

public class CityListActivity extends AppCompatActivity {

    private Button buttonAdd, buttonRefresh;
    private EditText editTextAdd;
    private ListView listViewCities;

    private BroadcastReceiver broadcastReceiver;
    private IntentFilter filter = new IntentFilter(Globals.WEATHER_QUERY_RESULT_FILTER);
    private WeatherService.WeatherServiceBinder weatherServiceBinder;
    private boolean isBoundToWeatherService = false;

    private AllCitiesWeather allCitiesWeather;
    private CityWeatherDataAdapter cityWeatherDataAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city_list);

        listViewCities = findViewById(R.id.listViewCities);
        editTextAdd = findViewById(R.id.editTextAdd);
        buttonRefresh = findViewById(R.id.buttonRefresh);
        buttonAdd = findViewById(R.id.buttonAdd);

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextAdd.getText().toString().contentEquals(""))
                    Toast.makeText(CityListActivity.this, "You must enter city name", Toast.LENGTH_SHORT).show();
                else{
                    String enteredCityName = editTextAdd.getText().toString().trim();
                    hideKeyboard(CityListActivity.this);

                    if(isBoundToWeatherService){
                        try {
                            weatherServiceBinder.AddCity(enteredCityName);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        throw new NotYetBoundException();
                    }
                }
            }
        });

        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    refreshList();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        listViewCities.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startCityDetailsActivity(position);
            }
        });

        //Retrieve new data from Weather Service
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String cityName = intent.getStringExtra(CITY_WEATHER_NAME);
                CityWeatherData newCityWeatherData = weatherServiceBinder.getCurrentWeather(cityName);

                Log.d("CityListActivity", "broadcastReciever: newcityData from: " + cityName);

                if(allCitiesWeather.CityExists(cityName)){
                    allCitiesWeather.UpdateCityWeatherData(cityName, newCityWeatherData);
                    cityWeatherDataAdapter.notifyDataSetChanged();
                }
                else{
                    allCitiesWeather.AddCity(newCityWeatherData);
                    cityWeatherDataAdapter.add(newCityWeatherData);
                    cityWeatherDataAdapter.notifyDataSetChanged();
                }
            }
        };
    }

    //Inspired by: https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE_DETAILS){
            switch (resultCode){
                case RESULT_CODE_REMOVE:
                    allCitiesWeather = weatherServiceBinder.getAllCitiesWeather();
                    cityWeatherDataAdapter.notifyDataSetChanged();
            }

            cityWeatherDataAdapter.notifyDataSetChanged();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        Intent weatherServiceIntent = new Intent(CityListActivity.this, WeatherService.class);

        if(!isMyServiceRunning(WeatherService.class)){
            startService(weatherServiceIntent);
            Log.d("CityListActivity", "Service was not already running!");
        }
        else{
            Log.d("CityListActivity", "Service was already running!");
        }

        bindService(weatherServiceIntent,mConnection, Context.BIND_AUTO_CREATE);
        registerReceiver(broadcastReceiver, filter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        if(isBoundToWeatherService){
            unbindService(mConnection);
        }
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }
        super.onPause();
    }

    private void refreshList() throws JSONException {
        Toast.makeText(this, "Refreshing list...", Toast.LENGTH_SHORT).show();

        if(isBoundToWeatherService){
            weatherServiceBinder.RefreshCityWeatherList();
        }
    }

    private void startCityDetailsActivity(int position) {
        CityWeatherData currentData = allCitiesWeather.GetAllCitiesWeatherData().get(position);

        Intent startCityDetailsIntent = new Intent(getApplicationContext(), CityDetailsActivity.class);
        startCityDetailsIntent.putExtra(Globals.CITY_WEATHER_NAME, currentData.Name);
        //startActivityForResult(startCityDetailsIntent, REQUEST_CODE_DETAILS);
        startActivity(startCityDetailsIntent);
    }

    private ServiceConnection mConnection= new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            weatherServiceBinder = (WeatherService.WeatherServiceBinder) iBinder;

            isBoundToWeatherService = true;
            setupMyCrazyAdapterArrayList();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBoundToWeatherService = false;
        }
    };

    private void setupMyCrazyAdapterArrayList() {
        allCitiesWeather = new AllCitiesWeather(weatherServiceBinder.getAllCitiesWeather().GetAllCitiesWeatherData());

        cityWeatherDataAdapter = new CityWeatherDataAdapter(CityListActivity.this,
                0, allCitiesWeather.GetAllCitiesWeatherData());
        listViewCities.setAdapter(cityWeatherDataAdapter);
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
