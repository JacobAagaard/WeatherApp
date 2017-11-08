package com.flonk.weatherapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONException;

import java.nio.channels.NotYetBoundException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;

import static com.flonk.weatherapp.Globals.WEATHER_QUERY_DATA;


// https://github.com/codepath/android_guides/wiki/Using-an-ArrayAdapter-with-ListView

public class CityListActivity extends AppCompatActivity {

    Button buttonAdd, buttonRefresh;
    EditText editTextAdd;
    ListView listViewCities;
    SharedPreferences sharedPreferences;
    String[] listItems = {};
    ArrayList<String> arrayList = new ArrayList<>();
    final static String FILENAME = "StorageFile";
    private BroadcastReceiver broadcastReceiver;
    private WeatherService.WeatherServiceBinder weatherServiceBinder;
    private boolean isBoundToWeatherService = false;

    private boolean mDownloading = false;

    ArrayList<CityWeatherData> arrayWeatherData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city_list);

        final Intent weatherServiceIntent = new Intent(CityListActivity.this, WeatherService.class);
        //startService(weatherServiceIntent);
        bindService(weatherServiceIntent,mConnection, Context.BIND_AUTO_CREATE);

        sharedPreferences = CityListActivity.this.getSharedPreferences(FILENAME , MODE_PRIVATE);

        listViewCities = findViewById(R.id.listViewCities);
        editTextAdd = findViewById(R.id.editTextAdd);

        editTextAdd.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    addCity(editTextAdd.getText().toString().trim());
                    hideKeyboard(CityListActivity.this);
                    return true;
                }
                return false;
            }
        });

        buttonRefresh = findViewById(R.id.buttonRefresh);
        buttonAdd = findViewById(R.id.buttonAdd);

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextAdd.getText().toString().contentEquals(""))
                    Toast.makeText(CityListActivity.this, "You must enter city name", Toast.LENGTH_SHORT).show();
                else{
                    String enteredCityName = editTextAdd.getText().toString().trim();
                    addCity(enteredCityName);
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
                refreshList();
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
                String result = intent.getStringExtra(WEATHER_QUERY_DATA);

                Gson gson = new Gson();
                CityWeatherData newCityWeatherData = gson.fromJson(result, CityWeatherData.class);
                Toast.makeText(CityListActivity.this, newCityWeatherData.Name,Toast.LENGTH_LONG).show();

                CityWeatherDataAdapter cityWeatherDataAdapter = new CityWeatherDataAdapter(CityListActivity.this,
                        0, arrayWeatherData);
                cityWeatherDataAdapter.add(newCityWeatherData);
                listViewCities.setAdapter(cityWeatherDataAdapter);
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
        for (int i = 0; i<listItems.length ; i++){
            listItems[i] = sharedPreferences.getString("#" + i, "String doesn't exist");
        }

        //getAllCitiesWeather().toArray((new String[listItems.length]));

        super.onRestart();
    }

    @Override
    protected void onStop() {
        //Inspired by: https://developer.android.com/training/basics/data-storage/shared-preferences.html
        SharedPreferences.Editor editor = sharedPreferences.edit(); //Initialize editor on sharedPref.

        //Only save list if items exist
        if(listItems.length > 0)
            saveStrings();

        for(int i = 0; i<listItems.length; i++){
            editor.putString("#"+i, listItems[i]);
        }

        editor.commit();    //Commit/apply changes.
        super.onStop();
    }

    private void saveStrings() {
        SharedPreferences.Editor editor = sharedPreferences.edit(); //Initialize editor on sharedPref.

        for(int i = 0; i<listItems.length; i++){
            editor.putString("#"+i, listItems[i]);
        }
        //editor.putString("#" + (listItems.length), cityName);
        editor.clear();
        editor.commit();    //Commit/apply changes.
    }

    private void addCity(String cityName) {
        arrayList.toArray(listItems.clone());
        arrayList.add(cityName);
        listItems = arrayList.toArray(new String[listItems.length]);
    }

    public void removeCity(String cityName){
        arrayList.toArray(listItems);

        //TODO: Remove city based on name
        Object object = new Object();
        arrayList.remove(object);
    }

    private void refreshList() {
        Toast.makeText(this, "Refreshing list...", Toast.LENGTH_SHORT).show();

        if(isBoundToWeatherService){
            arrayWeatherData.addAll(weatherServiceBinder.getAllCitiesWeather());
            CityWeatherDataAdapter cityWeatherDataAdapter = new CityWeatherDataAdapter(CityListActivity.this,
                    0, arrayWeatherData);
            cityWeatherDataAdapter.clear(); //Clear the list and add all cities again
            listViewCities.setAdapter(cityWeatherDataAdapter);
        }
    }

    private void startCityDetailsActivity(int cityID) {
        //Toast.makeText(this, "Opening " + listItems[cityID], Toast.LENGTH_SHORT).show();

        Intent startCityDetailsIntent = new Intent(getApplicationContext(), CityDetailsActivity.class);
        startCityDetailsIntent.putExtra("City_ID", cityID);
        startCityDetailsIntent.putExtra("#"+cityID, listItems[cityID]);
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
        arrayWeatherData.addAll(weatherServiceBinder.getAllCitiesWeather());
        CityWeatherDataAdapter cityWeatherDataAdapter = new CityWeatherDataAdapter(CityListActivity.this,
                0, arrayWeatherData);
        listViewCities.setAdapter(cityWeatherDataAdapter);
    }

}
