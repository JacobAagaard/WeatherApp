package com.flonk.weatherapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.Preference;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.flonk.weatherapp.Globals.WEATHER_QUERY_DATA;
import static com.flonk.weatherapp.Globals.WEATHER_QUERY_RESULT_FILTER;

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

        weatherQueryHelper = new WeatherQueryHelper(connectivityManager, this);

        super.onCreate();
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
            else{
                Toast.makeText(this, "error: " + errorMsg,Toast.LENGTH_LONG).show();
            }

            return;
        }

        CityWeatherData newCityWeatherData = CreateCityWeatherDataFromJson(queryResult.mResultValue);

        int index = GetIndexOfCity(newCityWeatherData.Name);

        if (index == -1) {
            _allCityWeatherData._listOfCityWeatherData.add(newCityWeatherData);
            SaveAllCititesWeatherToPref(); // XXX: dont save for every query
        } else {
            _allCityWeatherData._listOfCityWeatherData.set(index, newCityWeatherData);
            SaveAllCititesWeatherToPref(); // XXX dont save for every query
        }

        // broadcasts the new weather data to listeners
        // intens cant hold custom classes, and Gson is used to convert it to a string (Json)
        Intent resultIntent = new Intent(WEATHER_QUERY_RESULT_FILTER);
        Gson gson = new Gson();
        resultIntent.putExtra(WEATHER_QUERY_DATA, gson.toJson(newCityWeatherData));
        sendBroadcast(resultIntent);
    }

    public class WeatherServiceBinder extends Binder{

        CityWeatherData getCurrentWeather(String cityName){
            int index = GetIndexOfCity(cityName);

            if(index == -1){
                return null;
            }
            else{
                return _allCityWeatherData._listOfCityWeatherData.get(index);
            }
        };

        List<CityWeatherData> getAllCitiesWeather(){
            return _allCityWeatherData._listOfCityWeatherData;
        }

        void AddCity(String cityName) throws JSONException {
            weatherQueryHelper.Query(cityName);
        }

        void RemoveCity(String cityName){
            throw new UnsupportedOperationException();
        }

        void AddCityTester(String cityName){
            _allCityWeatherData._listOfCityWeatherData.add(new CityWeatherData());
            SaveAllCititesWeatherToPref();
        }
    }

    private void SaveNewWeatherData(List<CityWeatherData> data){
        throw new UnsupportedOperationException();
    }


    public class AllCitiesWeather{
        public ArrayList<CityWeatherData> _listOfCityWeatherData = new ArrayList<CityWeatherData>();
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

        String name,temp,humidity, description,icon, timestamp;

        String returnString = "Parse went wrong";
        try{
            JSONObject jsonObject = new JSONObject(jsonString);
            name = jsonObject.getString("name");
            temp = jsonObject.getJSONObject("main").getString("temp"); //Object inside object
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

    private int GetIndexOfCity(String cityName){
        int count = _allCityWeatherData._listOfCityWeatherData.size();
        for (int i = 0 ; i < count; i++){

            if(_allCityWeatherData._listOfCityWeatherData.get(i).equals(cityName)){
                return i;
            }
        }
        return -1; // returns -1 if city was not found
    }
}
