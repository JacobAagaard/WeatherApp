package com.flonk.weatherapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import static com.flonk.weatherapp.Globals.CITY_WEATHER_NAME;
import static com.flonk.weatherapp.Globals.DEGREE_SIGN;
import static com.flonk.weatherapp.Globals.WEATHER_QUERY_RESULT_FILTER;
import static java.lang.Math.round;

public class WeatherService extends Service implements WeatherQueryCallback {

    private final String PREFERENCE = "com.flonk.weatherapp.preference";
    private final String LIST_OF_ALL_CITY_WEATHER_DATA = "com.flonk.weatherapp.list.of.all.city.weather.data";
    private IBinder _mBinder = new WeatherServiceBinder();
    private AllCitiesWeather _allCityWeatherData;
    private WeatherQueryHelper weatherQueryHelper;
    private int notificationId = 1234;
    private NotificationManager notificationManager;
    private BroadcastReceiver timerReciever;
    private IntentFilter timerFilter;


    @Override
    public void onCreate() {
        Log.d("WeatherApp","OnCreate was called!");

        // retrieves all the saved Cities
        GetAllCitiesWeatherFromPref();

        // sets up the notification manager
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // is used by the download task to check if the connection is ok
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        weatherQueryHelper = new WeatherQueryHelper(connectivityManager);

        // runs a while(true) loop that updates the weather every X secons
        try {
            Run();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // defines filters that listens for time changes.
        timerFilter = new IntentFilter();
        timerFilter.addAction(Intent.ACTION_TIME_TICK);
        timerFilter.addAction(Intent.ACTION_TIME_CHANGED);

        // broadcastreveicer for when the time changes.
        // it checks if the subscribed city has the same time as the current time
        timerReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(Intent.ACTION_TIME_CHANGED) || action.equals(Intent.ACTION_TIME_TICK))
                {
                    CityWeatherData cityData = _allCityWeatherData.GetSubscribedCity();

                    if(!(cityData == null)){
                        Calendar calendar = Calendar.getInstance();
                        String citySceduledTime = cityData.scheduledNotificationTime;
                        String currentTime = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY) + String.valueOf(calendar.get(Calendar.MINUTE)));

                        // creates a notification if the currentTime matches the subscribed time
                        if(citySceduledTime.equals(currentTime)){
                            Log.d("WeatherApp", "TimerReciever: Creating Notification!");
                            CreateNotification(cityData);
                        }
                    }

                }
            }
        };

        // when ever the service is created, it checks if it is subscribed to a city, and if it is, it starts the broadcastreciever
        if (!(_allCityWeatherData.GetSubscribedCity() == null)){
            try{
                registerReceiver(timerReciever, timerFilter);
                Log.d("WeatherApp", "TimerReciever Registered");
            } catch (Exception e){
                Log.d("WeatherApp", e.getMessage());
            }
        }
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("WeatherApp","OnStartCommand was called!");
        return START_STICKY; // sticky means it starts again whenever the system has enough recources if it was destroyed at some point
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _mBinder;
    }

    // this is the callback function defined whenever a HTTP query for new weather data has returned.
    @Override
    public void QueryResult(WeatherQueryHelper.WeatherQueryResult queryResult) {

        // checks if the result of the query is an error
        if(queryResult.mException != null){

            String errorMsg = queryResult.mException.getMessage();

            if(errorMsg.contains("404")){
                Toast.makeText(this, "No City with that name is registered on the database! : " + errorMsg,Toast.LENGTH_LONG).show();
            }
            else if(errorMsg.contains("429")){
                Toast.makeText(this, "bro...... to many requests... Chill down mate : " + errorMsg, Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this, "error: " + errorMsg,Toast.LENGTH_LONG).show();
            }
            return;
        }

        // the result was not an error and the cityweather is retrieved.
        CityWeatherData newCityWeatherData = CreateCityWeatherDataFromJson(queryResult.mResultValue);

        // error in the recieved query.
        if(newCityWeatherData == null){
            Toast.makeText(this, "Query return null", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("WeatherApp", "In QueryResult, query return with city: " + newCityWeatherData.Name);

        // this checks if the returned result is a new city or fresh data for an existing city.
        if (!_allCityWeatherData.CityExists(newCityWeatherData.Name)) {
            _allCityWeatherData.AddCity(newCityWeatherData);
            SaveAllCititesWeatherToPref(); // TODO: dont save for every query
        } else {
            String cityName = newCityWeatherData.Name;
            _allCityWeatherData.UpdateCityWeatherData(cityName, newCityWeatherData);
            SaveAllCititesWeatherToPref(); // TODO: dont save for every query
        }

        // broadcasts that new data is available
        Intent resultIntent = new Intent(WEATHER_QUERY_RESULT_FILTER);
        resultIntent.putExtra(CITY_WEATHER_NAME, newCityWeatherData.Name);
        sendBroadcast(resultIntent);
    }

    // this defines the API for the two Activities bound to this services.
    public class WeatherServiceBinder extends Binder{

        CityWeatherData getCurrentWeather(String cityName){
            return _allCityWeatherData.GetCityWeatherData(cityName);
        };

        AllCitiesWeather getAllCitiesWeather(){
            return _allCityWeatherData;
        }

        void AddCity(String cityName) throws JSONException {
            weatherQueryHelper.Query(cityName, WeatherService.this);
        }

        void RemoveCity(String cityName){
            _allCityWeatherData.RemoveCity(cityName);
            SaveAllCititesWeatherToPref();
        }

        void RefreshCityWeatherList() throws JSONException {
            UpdateListOfCityWeatherData();
            SaveAllCititesWeatherToPref();
        }

        // everytime subscription occours, the timerBroadcast is registered
        void SubscribedCity(String cityName, String subscribedTime){
            _allCityWeatherData.SubscribedCity(cityName, subscribedTime);
            SaveAllCititesWeatherToPref();
            Log.d("WeatherApp", "SubscribeedCity: registering broadcastReciever!");
            try {
                registerReceiver(timerReciever, timerFilter);
            }
            catch (Exception e){
                Log.d("WeatherApp", "exception: " + e.getMessage());
            }
        }

        // everytime un-subscription occours, the timerBroadcast is unregistered
        void UnSubscribeCity(String name){
            _allCityWeatherData.UnSubScribeCity(name);
            SaveAllCititesWeatherToPref();
            notificationManager.cancel(notificationId);
            Log.d("WeatherApp", "SubscribeedCity: Unregistering broadcastReciever!");
            try {
                unregisterReceiver(timerReciever);
            } catch (Exception e)
            {
                Log.d("WeatherApp", "exception: " + e.getMessage());
            }
        }
    }

    // retreives all the city data from prefs
    private void GetAllCitiesWeatherFromPref() {
        SharedPreferences pref = getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        String weatherDataAsJson = pref.getString(LIST_OF_ALL_CITY_WEATHER_DATA, null);

        // this should only be null the first time the app runs
        if (weatherDataAsJson == null) {
            _allCityWeatherData = new AllCitiesWeather();
        } else {
            Gson myGsonConverter = new Gson();
            _allCityWeatherData = myGsonConverter.fromJson(weatherDataAsJson, AllCitiesWeather.class);
        }
    }

    // saves all the city data to prefs
    private void SaveAllCititesWeatherToPref(){
        SharedPreferences pref = this.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        Gson gsonConverter = new Gson();
        String jsonData = gsonConverter.toJson(_allCityWeatherData);
        editor.putString(LIST_OF_ALL_CITY_WEATHER_DATA, jsonData);
        editor.commit();
    }

    // private helper method that extracts the necessary information from the HTTP query
    private CityWeatherData CreateCityWeatherDataFromJson(String jsonString){

        String name, temp, humidity, description, icon, timestamp;

        double kelvinTempVal = -273.15f;

        try{
            JSONObject jsonObject = new JSONObject(jsonString);
            name = jsonObject.getString("name");
            double tmpTemp = jsonObject.getJSONObject("main").getDouble("temp") + kelvinTempVal; //Object inside object
            temp = String.valueOf(round(tmpTemp));
            description = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");
            icon = jsonObject.getJSONArray("weather").getJSONObject(0).getString("icon");
            humidity = jsonObject.getJSONObject("main").getString("humidity");
            timestamp = jsonObject.getString("dt");

            return new CityWeatherData(name, temp, humidity, description, icon, timestamp);
        }
        catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }

        return null;
    }

    // private helper method that runs the entire list through and creates new queries for each city refreshing the data.
    private void UpdateListOfCityWeatherData() throws JSONException {
        int count = _allCityWeatherData.GetAllCitiesWeatherData().size();

        Log.d("WeatherApp", "UpdateListOfCityWeatherData, Forloop, number of cities: " + count);

        for (int i = 0 ; i < count; i++){
            String cityName = _allCityWeatherData.GetAllCitiesWeatherData().get(i).Name;
            Log.d("WeatherApp", "Forloop, i= " + i + ": Querying city: " + cityName);
            weatherQueryHelper.Query(cityName, WeatherService.this);
        }
    }

    // defines the ever-going run method of the service.
    Thread workerThread = new Thread() {
        @Override
        public void run() {
            try {
                while(true) {
                    Thread.sleep(5000);
                    for(int i = 0; i < _allCityWeatherData.GetAllCitiesWeatherData().size(); i++){
                        CityWeatherData cityData = _allCityWeatherData.GetAllCitiesWeatherData().get(i);
                        Log.d("WeatherApp", cityData.Name + cityData.isSubscribed + cityData.scheduledNotificationTime);
                    }

                    UpdateListOfCityWeatherData();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private void Run() throws JSONException {
        workerThread.start();
    }

    // inspiration from: https://stackoverflow.com/questions/15758980/android-service-needs-to-run-always-never-pause-or-stop
    // here the notification is defined whenever the subscribed city timer goes off.
    private void CreateNotification(CityWeatherData cityData){

        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(cityData.Name)
                .setContentText(cityData.Description + ", " + cityData.Temperature + DEGREE_SIGN + "C")
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), Util.GetIconId(cityData.Icon)))
                .setSmallIcon(R.mipmap.ic_launcher_round);

        // defines the activity that can be created of the notification is pressed on.
        Intent intent = new Intent(this, CityDetailsActivity.class);
        intent.putExtra(Globals.CITY_WEATHER_NAME, cityData.Name);
        //intent.setAction("dummyAction"); // needs to be set for the extras to not go away when using FLAG_ONE_SHOT. Source: https://stackoverflow.com/questions/3127957/why-the-pendingintent-doesnt-send-back-my-custom-extras-setup-for-the-intent
        intent.putExtra(Globals.CITY_DETAIL_ACTIVITY_STARTED_FROM_SERVICE, true);

        // it is wrapped in a pendingintent meaning that it will "pend" until the user clicks on the notification, resulting in the intent to start (CityDeytailsActivity)
        PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendIntent);
        notificationManager.notify(notificationId, builder.build());
    }
}