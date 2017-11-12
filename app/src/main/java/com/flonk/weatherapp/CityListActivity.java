package com.flonk.weatherapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
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

public class CityListActivity extends AppCompatActivity{

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

        // sets up all the UI views
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

                    // when bound, it calls the method AddCity on the service.
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
                    if(isBoundToWeatherService){
                        weatherServiceBinder.RefreshCityWeatherList();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        // when clicking on one of the items in the lists, it starts the detailed view
        listViewCities.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startCityDetailsActivity(position);
            }
        });

        // specifies the filter that catches the new result message from the service
        IntentFilter filter = new IntentFilter(Globals.WEATHER_QUERY_RESULT_FILTER);

        // Defines the broadcast reciever for listening on new data from the server
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String cityName = intent.getStringExtra(CITY_WEATHER_NAME);
                CityWeatherData newCityWeatherData = weatherServiceBinder.getCurrentWeather(cityName);

                Log.d("WeatherApp", "broadcastReciever: newcityData from: " + cityName);

                if(allCitiesWeather.CityExists(cityName)){
                    allCitiesWeather.UpdateCityWeatherData(cityName, newCityWeatherData);
                    cityWeatherDataAdapter.notifyDataSetChanged();
                }
                else{
                    cityWeatherDataAdapter.notifyDataSetChanged();
                }
            }
        };
    }

    @Override
    protected void onResume() {
        // defines the intent for the WeatherService
        Intent weatherServiceIntent = new Intent(CityListActivity.this, WeatherService.class);

        if(!isMyServiceRunning(WeatherService.class)){
            startService(weatherServiceIntent);
            Log.d("WeatherApp", "Service was not already running!");
        }
        else{
            Log.d("WeatherApp", "Service was already running!");
        }
        // binds to the service
        bindService(weatherServiceIntent,mConnection, Context.BIND_AUTO_CREATE);

        // registers receivers
        registerReceiver(broadcastReceiver, filter);

        super.onResume();
    }

    @Override
    protected void onPause() {
        if(isBoundToWeatherService){
            try{
                unbindService(mConnection);
            } catch (Exception e){
                Log.d("WeatherApp", e.getMessage());
            }
        }
        if(broadcastReceiver != null){
            try {
                unregisterReceiver(broadcastReceiver);
            } catch (Exception e){
                Log.d("WeatherApp", "onPause in CityListActivity: " + e.getMessage());
            }
        }
        super.onPause();
    }

    private void startCityDetailsActivity(int position) {
        CityWeatherData currentData = allCitiesWeather.GetAllCitiesWeatherData().get(position);

        Intent startCityDetailsIntent = new Intent(getApplicationContext(), CityDetailsActivity.class);
        startCityDetailsIntent.putExtra(CITY_WEATHER_NAME, currentData.Name);
        startActivity(startCityDetailsIntent);
    }

    private ServiceConnection mConnection= new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // gets the binder from the service
            weatherServiceBinder = (WeatherService.WeatherServiceBinder) iBinder;

            // updates the local copy of all the city data
            allCitiesWeather = new AllCitiesWeather(weatherServiceBinder.getAllCitiesWeather().GetAllCitiesWeatherData());

            isBoundToWeatherService = true;
            setupMyCrazyAdapterArrayList();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBoundToWeatherService = false;
        }
    };

    // used to setup the adaptor for the listview
    private void setupMyCrazyAdapterArrayList() {
        // defines the adaptor for the listview
        cityWeatherDataAdapter = new CityWeatherDataAdapter(CityListActivity.this,
                0, allCitiesWeather.GetAllCitiesWeatherData());
        // sets the adaptor
        listViewCities.setAdapter(cityWeatherDataAdapter);
    }

    // from : https://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-on-android
    // used to check whether the service is already running
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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
}
