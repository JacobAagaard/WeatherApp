package com.flonk.weatherapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.Preference;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class WeatherService extends Service {

    private final String PREFERENCE = "com.flonk.weatherapp.preference";
    private final String LIST_OF_ALL_CITY_WEATHER_DATA = "com.flonk.weatherapp.list.of.all.city.weather.data";
    private IBinder _mBinder = new WeatherServiceBinder();
    private AllCitiesWeather _allCityWeatherData;

    public WeatherService() {
        SharedPreferences pref = this.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        String weatherDataAsJson = pref.getString(LIST_OF_ALL_CITY_WEATHER_DATA, null);

        // this should only be null the first time the app runs
        if(weatherDataAsJson == null){
            _allCityWeatherData = new AllCitiesWeather();
        }
        else{
            Gson myGsonConverter = new Gson();
            _allCityWeatherData = myGsonConverter.fromJson(weatherDataAsJson, AllCitiesWeather.class);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    public class WeatherServiceBinder extends Binder{

        CityWeatherData getCurrentWeather(String cityName){

            // XXX should retreieve the latest version of the weather from the weather api!

            for (CityWeatherData data : _allCityWeatherData._listOfCityWeatherData){
                if(data.equals(cityName)){
                    return data;
                }
            }
            return null;
        };

        List<CityWeatherData> getAllCitiesWeather(){
            return _allCityWeatherData._listOfCityWeatherData;
        }

        void AddCity(String cityName){
            throw new UnsupportedOperationException();
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

    private void SaveAllCititesWeatherToPref(){
        SharedPreferences pref = this.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        Gson gsonConverter = new Gson();
        String jsonData = gsonConverter.toJson(_allCityWeatherData);
        editor.putString(LIST_OF_ALL_CITY_WEATHER_DATA, jsonData);
        editor.commit();
    }

}
