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
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
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
    private boolean createNotification = false;


    @Override
    public void onCreate() {
        Log.d("WeatherService","OnCreate was called!");

        // retrieves all the saved Cities
        GetAllCitiesWeatherFromPref();

        // sets up the notification manager
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // is used by the download task to check if the connection is ok
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        weatherQueryHelper = new WeatherQueryHelper(connectivityManager);

        timerFilter = new IntentFilter();
        timerFilter.addAction(Intent.ACTION_TIME_TICK);
        timerFilter.addAction(Intent.ACTION_TIME_CHANGED);

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


                        if(citySceduledTime.equals(currentTime)){
                            // boolean that specifies when a query result returns it has to create a notification also!
                            createNotification = true;

                            try {
                                weatherQueryHelper.Query(cityData.Name, WeatherService.this);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }
            }
        };


        // registers reciever for the timer broadcast
        try{
            registerReceiver(timerReciever, timerFilter);
            Log.d("WeatherService", "TimerReciever Registered");
        } catch (Exception e){
            Log.d("WeatherService", e.getMessage());
        }


        try {
            Run();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("WeatherService","OnStartCommand was called!");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _mBinder;
    }

    @Override
    public void QueryResult(WeatherQueryHelper.WeatherQueryResult queryResult) {

        // checks if the result of the query is an error
        if(queryResult.mException != null){

            String errorMsg = queryResult.mException.getMessage();

            if(errorMsg.contains("404")){
                Toast.makeText(this, R.string.error404, Toast.LENGTH_LONG).show();
            }
            else if(errorMsg.contains("429")){
                Toast.makeText(this, R.string.error429, Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this, R.string.error + errorMsg,Toast.LENGTH_LONG).show();
            }
            return;
        }

        CityWeatherData newCityWeatherData = CreateCityWeatherDataFromJson(queryResult.mResultValue);

        if(newCityWeatherData == null){
            Toast.makeText(this, R.string.nullQuery, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("WeatherService", "In QueryResult, query return with city: " + newCityWeatherData.Name);


        if (!_allCityWeatherData.CityExists(newCityWeatherData.Name)) {
            _allCityWeatherData.AddCity(newCityWeatherData);
            SaveAllCititesWeatherToPref(); // TODO: dont save for every query
        } else {
            String cityName = newCityWeatherData.Name;
            _allCityWeatherData.UpdateCityWeatherData(cityName, newCityWeatherData);
            SaveAllCititesWeatherToPref(); // TODO: dont save for every query
        }

        if(createNotification){
            CreateNotification(newCityWeatherData);
            createNotification = false;
        }

        // broadcasts that new data is available
        Intent resultIntent = new Intent(WEATHER_QUERY_RESULT_FILTER);
        resultIntent.putExtra(CITY_WEATHER_NAME, newCityWeatherData.Name);
        sendBroadcast(resultIntent);
    }

    /*Theoretically, each of the methods in the Binder Interface should have their own lock,
    * for the usage of the service to be thread-safe.
    * this defines the API for the two Activities bound to this services.
     */
    public class WeatherServiceBinder extends Binder{

        CityWeatherData getCurrentWeather(String cityName){
            return _allCityWeatherData.GetCityWeatherData(cityName);
        }

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

        void SubscribedCity(String cityName, String chosenTime){
            _allCityWeatherData.SubscribedCity(cityName, chosenTime);
            SaveAllCititesWeatherToPref();
            Log.d("WeatherService", "SubscribeedCity: registering broadcastReciever!");
            try {
                registerReceiver(timerReciever, timerFilter);
            }
            catch (Exception e){
                Log.d("WeatherService", "exception: " + e.getMessage());
            }
        }

        void UnsubscribeCity(String name){
            _allCityWeatherData.UnSubScribeCity(name);
            SaveAllCititesWeatherToPref();
            notificationManager.cancel(notificationId);
            Log.d("WeatherService", "SubscribeedCity: Unregistering broadcastReciever!");
            try {
                unregisterReceiver(timerReciever);
            } catch (Exception e)
            {
                Log.d("WeatherService", "exception: " + e.getMessage());
            }
        }
    }

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

    private void SaveAllCititesWeatherToPref(){
        SharedPreferences pref = this.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        Gson gsonConverter = new Gson();
        String jsonData = gsonConverter.toJson(_allCityWeatherData);
        editor.putString(LIST_OF_ALL_CITY_WEATHER_DATA, jsonData);
        editor.commit();
    }

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

            java.util.Date time = new java.util.Date((long) jsonObject.getLong("dt") * 1000);

            SimpleDateFormat sd = new SimpleDateFormat("HH:mm - dd-MM-yyyy");

            timestamp = sd.format(time);

            return new CityWeatherData(name, temp, humidity, description, icon, timestamp);
        }
        catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }

        return null;
    }

    private void UpdateListOfCityWeatherData() throws JSONException {
        int count = _allCityWeatherData.GetAllCitiesWeatherData().size();

        Log.d("WeatherService", "UpdateListOfCityWeatherData, Forloop, number of cities: " + count);

        for (int i = 0 ; i < count; i++){
            String cityName = _allCityWeatherData.GetAllCitiesWeatherData().get(i).Name;
            Log.d("WeatherService", "Forloop, i= " + i + ": Querying city: " + cityName);
            weatherQueryHelper.Query(cityName, WeatherService.this);
        }
    }

    Thread workerThread = new Thread() {
        @Override
        public void run() {
            try {
                while(true) {
                    Thread.sleep(Globals.DELAY_BETWEEN_UPDATES);
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
    private void CreateNotification(CityWeatherData cityData){
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(cityData.Name + ", Updated: " + cityData.TimeStamp)
                .setContentText(cityData.Description + ", " + cityData.Temperature + DEGREE_SIGN + "C")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setAutoCancel(true); // means when pressed, it goes away!

        // we need to build a basic notification first, then update it
        Intent intent = new Intent(this, CityDetailsActivity.class);
        Log.d("WeatherService", "Creating a notification for city: " + cityData.Name + " isSubscribed: " + cityData.isSubscribed);
        intent.putExtra(Globals.CITY_WEATHER_NAME, cityData.Name);
//        intent.setAction("dummyAction"); // needs to be set for the extras to not go away when using FLAG_ONE_SHOT. Source: https://stackoverflow.com/questions/3127957/why-the-pendingintent-doesnt-send-back-my-custom-extras-setup-for-the-intent
        intent.putExtra(Globals.CITY_DETAIL_ACTIVITY_STARTED_FROM_SERVICE, true);

        PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pendIntent);

        notificationManager.notify(notificationId, builder.build());
    }
}


