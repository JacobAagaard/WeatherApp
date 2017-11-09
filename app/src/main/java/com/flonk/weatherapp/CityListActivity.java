package com.flonk.weatherapp;

import android.app.Activity;
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

import com.google.gson.Gson;

import org.json.JSONException;

import java.nio.channels.NotYetBoundException;

import static com.flonk.weatherapp.Globals.NEW_WEATHER_DATA;

public class CityListActivity extends AppCompatActivity {

    private Button buttonAdd, buttonRefresh;
    private EditText editTextAdd;
    private ListView listViewCities;

    private BroadcastReceiver broadcastReceiver;

    private WeatherService.WeatherServiceBinder weatherServiceBinder;
    private boolean isBoundToWeatherService = false;

    private AllCitiesWeather allCitiesWeather;
    private CityWeatherDataAdapter cityWeatherDataAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city_list);

        final Intent weatherServiceIntent = new Intent(CityListActivity.this, WeatherService.class);
        //startService(weatherServiceIntent);
        bindService(weatherServiceIntent,mConnection, Context.BIND_AUTO_CREATE);

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

        IntentFilter filter = new IntentFilter(Globals.WEATHER_QUERY_RESULT_FILTER);

        //Retrieve new data from Weather Service
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle weatherData;
                String result = intent.getStringExtra(NEW_WEATHER_DATA);

                Gson gson = new Gson();
                CityWeatherData newCityWeatherData = gson.fromJson(result, CityWeatherData.class);
                String cityName = newCityWeatherData.Name;

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
        registerReceiver(broadcastReceiver, filter);
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
    protected void onRestart() {
        allCitiesWeather = weatherServiceBinder.getAllCitiesWeather();
        super.onRestart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void refreshList() throws JSONException {
        Toast.makeText(this, "Refreshing list...", Toast.LENGTH_SHORT).show();

        if(isBoundToWeatherService){
            weatherServiceBinder.RefreshCityWeatherList();
        }
    }

    private void startCityDetailsActivity(int position) {
        CityWeatherData currentData = allCitiesWeather.GetAllCitiesWeatherData().get(position);

        Gson gson = new Gson();
        String jsonString = gson.toJson(currentData);

        Intent startCityDetailsIntent = new Intent(getApplicationContext(), CityDetailsActivity.class);
        startCityDetailsIntent.putExtra("City_ID", jsonString);
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

}
