package com.flonk.weatherapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;

import static com.flonk.weatherapp.Globals.CITY_WEATHER_NAME;
import static com.flonk.weatherapp.Globals.WEATHER_QUERY_RESULT_FILTER;
import static java.lang.Math.round;

public class WeatherService extends Service implements WeatherQueryCallback {

    private final String PREFERENCE = "com.flonk.weatherapp.preference";
    private final String LIST_OF_ALL_CITY_WEATHER_DATA = "com.flonk.weatherapp.list.of.all.city.weather.data";
    private IBinder _mBinder = new WeatherServiceBinder();
    private AllCitiesWeather _allCityWeatherData;
    private WeatherQueryHelper weatherQueryHelper;

    @Override
    public void onCreate() {

        GetAllCitiesWeatherFromPref();

        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        weatherQueryHelper = new WeatherQueryHelper(connectivityManager);

        workerThread.start();

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public WeatherService() {

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

        CityWeatherData newCityWeatherData = CreateCityWeatherDataFromJson(queryResult.mResultValue);

        if(newCityWeatherData == null){
            Toast.makeText(this, "Query return null", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("WeatherService", "In QueryResult, query return with city: " + newCityWeatherData.Name);


        if (!_allCityWeatherData.CityExists(newCityWeatherData.Name)) {
            _allCityWeatherData.AddCity(newCityWeatherData);
            SaveAllCititesWeatherToPref(); // XXX: dont save for every query
        } else {
            String cityName = newCityWeatherData.Name;
            _allCityWeatherData.UpdateCityWeatherData(cityName, newCityWeatherData);
            SaveAllCititesWeatherToPref(); // XXX dont save for every query
        }

        // broadcasts that new data is available
        Intent resultIntent = new Intent(WEATHER_QUERY_RESULT_FILTER);
        resultIntent.putExtra(CITY_WEATHER_NAME, newCityWeatherData.Name);
        sendBroadcast(resultIntent);
    }

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
        }

        void RefreshCityWeatherList() throws JSONException {
            UpdateListOfCityWeatherData();
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
            timestamp = jsonObject.getString("dt");

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
                    Thread.sleep(60000);
                    Log.d("WeatherService", "Calling UpdateListOfCityWeatherData, containing " + _allCityWeatherData.GetAllCitiesWeatherData().size() + " cities");
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
}


