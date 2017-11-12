package com.flonk.weatherapp;

import android.app.ActivityManager;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;
import java.util.Calendar;
import static com.flonk.weatherapp.Globals.RESULT_CODE_REMOVE;

public class CityDetailsActivity extends AppCompatActivity {

    private Button buttonOK, buttonRemove;
    private TextView textViewCityName, textViewHumidity, textViewWeatherDescription, textViewTemperature;
    private ImageView imvIcon;
    private ToggleButton toggleButton;
    private WeatherService.WeatherServiceBinder weatherServiceBinder;
    private boolean isBoundToWeatherService = false;
    private AllCitiesWeather allCitiesWeather;
    private CityWeatherData currentData;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city_details);

        // initialies all the UI elements
        buttonOK = findViewById(R.id.buttonOK);
        buttonRemove = findViewById(R.id.buttonRemove);
        textViewCityName = findViewById(R.id.textViewCityName);
        textViewHumidity = findViewById(R.id.textViewHumidity);
        textViewWeatherDescription = findViewById(R.id.textViewWeatherDescription);
        textViewTemperature = findViewById(R.id.textViewTemperature);
        imvIcon = findViewById(R.id.imvIcon);
        toggleButton = findViewById(R.id.toggleButtonSubscription);

        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        buttonRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // checks if still bound to the service
                if(isBoundToWeatherService){
                    // when removing a cityData that is subscribed, it gets unsubscribed first
                    if(currentData.isSubscribed){
                        weatherServiceBinder.UnSubscribeCity(currentData.Name);
                    }
                    weatherServiceBinder.RemoveCity(currentData.Name);
                }
                else
                {
                    Toast.makeText(CityDetailsActivity.this, "NOT BOUND", Toast.LENGTH_SHORT).show();
                    return;
                }

                // this is done to check if the activity was started from the SERVICE, more specifically by the PendingIntent created in the Notification.Builder
                // this is done because we think the natual flow for the user should be to return to the CityListsActivity when removing a city.
                if(getIntent().getBooleanExtra(Globals.CITY_DETAIL_ACTIVITY_STARTED_FROM_SERVICE, false)){
                    Intent intent = new Intent(CityDetailsActivity.this, CityListActivity.class);
                    intent.putExtra(Globals.CITY_LIST_ACTIVITY_STARTED_FROM_CITY_DETIAL_ACTIVITY, true);

                    // this call automatiacally calls finish and terminates the activity because "android:noHistory="true" was set for the activity in the XML.
                    // that tag, specifies that if creating a new intent it should terminate because it has "no history".
                    startActivity(intent);
                }
                finish();
            }
        });

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!toggleButton.isChecked()){
                    weatherServiceBinder.UnSubscribeCity(currentData.Name);
                    toggleButton.setChecked(false);
                }
                else{
                    // sets the toggle to false in case the user cancels the timerpicker dialog
                    toggleButton.setChecked(false);
                    OpenTimePickerDialog();
                }
            }
        });
    }

    // from : http://abhiandroid.com/ui/timepicker
    // this opens up a dialog that gives the user a choice of setting the time of when the notification should come for the subscribed city
    private void OpenTimePickerDialog(){
        Calendar mcurrentTime = Calendar.getInstance();
        int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(Calendar.MINUTE);
        TimePickerDialog mTimePicker;
        mTimePicker = new TimePickerDialog(CityDetailsActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {

                Log.d("WeatherApp", "selected hour: "+ selectedHour + " selected minut: "+selectedMinute);

                // retrieves the latest weather information from the service.
                allCitiesWeather = weatherServiceBinder.getAllCitiesWeather();

                // returns the subscribed city (if null, it means no city has been subscribed on before)!
                CityWeatherData cityData = allCitiesWeather.GetSubscribedCity();

                if(!(cityData == null)){
                    weatherServiceBinder.UnSubscribeCity(cityData.Name);
                }


                // retrieves the chosen time by the user
                String chosenTimeByUser = String.valueOf(selectedHour) + String.valueOf(selectedMinute);

                // subscribes the current city
                weatherServiceBinder.SubscribedCity(currentData.Name, chosenTimeByUser);

                // sets the toggle to true
                toggleButton.setChecked(true);

            }
        }, hour, minute, true);//Yes 24 hour time
        mTimePicker.setTitle("Select Time");
        mTimePicker.show();
    }

    @Override
    protected void onResume() {
        Intent weatherServiceIntent = new Intent(CityDetailsActivity.this, WeatherService.class);

        // checks if the service is already running.
        if(!isMyServiceRunning(WeatherService.class)){
            startService(weatherServiceIntent);
            Log.d("WeatherApp", "Service was not already running!");
        }
        else{
            Log.d("WeatherApp", "Service was already running!");
        }

        // binds to the service
        bindService(weatherServiceIntent,serviceConnection, Context.BIND_AUTO_CREATE);

        super.onResume();
    }

    @Override
    protected void onPause() {

        // unbinds to service
        if(isBoundToWeatherService){
            unbindService(serviceConnection);
        }
        super.onPause();
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            // gets the binder (API to the service)
            weatherServiceBinder = (WeatherService.WeatherServiceBinder) iBinder;
            isBoundToWeatherService = true;

            // gets the name of the game: ie. the city name of the context the detail view was opened with.
            String cityName = getIntent().getStringExtra(Globals.CITY_WEATHER_NAME);
            currentData = weatherServiceBinder.getCurrentWeather(cityName);

            // sets up the UI
            setupUIWithServiceData();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBoundToWeatherService = false;
        }
    };

    // used to setup all the UI elements
    private void setupUIWithServiceData() {
        textViewCityName.setText(currentData.Name);
        textViewHumidity.setText(currentData.Humidity);
        textViewTemperature.setText(currentData.Temperature);
        textViewWeatherDescription.setText(currentData.Description);
        imvIcon.setImageResource(Util.GetIconId(currentData.Icon));

        Log.d("WeatherApp", "SetupUI" + currentData.Name + "isSubscribed: " + currentData.isSubscribed);
        toggleButton.setChecked(currentData.isSubscribed);
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
}
