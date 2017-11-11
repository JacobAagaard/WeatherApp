package com.flonk.weatherapp;

import java.util.ArrayList;

/**
 * Created by Frederik on 11/9/2017.
 */

// class that contains a list of all CityWeatherData.
// It is used by Gson class to serialize and deserialize the entire object and then save it as a string in SharedPreference
public class AllCitiesWeather{
    private ArrayList<CityWeatherData> _listOfCityWeatherData;
    private int subscribedCity = -1;


    public AllCitiesWeather(){
        _listOfCityWeatherData = new ArrayList<CityWeatherData>();
    }

    public AllCitiesWeather(ArrayList<CityWeatherData> listOfCityWeatherData){
        _listOfCityWeatherData = listOfCityWeatherData;
    }

    public void AddCity(CityWeatherData data){
        _listOfCityWeatherData.add(data);
    }

    public boolean RemoveCity(String cityName){
        int index = GetIndexOfCity(cityName);

        if(index == -1){
            return false;
        }

        _listOfCityWeatherData.remove(index);

        return true;
    }

    public boolean UpdateCityWeatherData(String cityName, CityWeatherData data){
        int index = GetIndexOfCity(cityName);

        if(index == -1){
            return false;
        }
        _listOfCityWeatherData.set(index, data);
        return true;
    }

    public CityWeatherData GetCityWeatherData(String cityName){
        int index = GetIndexOfCity(cityName);

        if(index == -1) return null;

        return _listOfCityWeatherData.get(index);
    }

    public ArrayList<CityWeatherData> GetAllCitiesWeatherData(){
        return _listOfCityWeatherData;
    }

    public boolean CityExists(String cityName){
        if(GetIndexOfCity(cityName) == -1) return false;
        return true;
    }

    private int GetIndexOfCity(String cityName){
        int count = _listOfCityWeatherData.size();

        for (int i = 0 ; i < count; i++){
            String elemCityName = _listOfCityWeatherData.get(i).Name;
            if(elemCityName.equals(cityName)){
                return i;
            }
        }
        return -1; // returns -1 if city was not found
    }

    public int GetSubscribedCity(){
        return subscribedCity;
    }

    public boolean SetSubscribedCity(int index){
        if(-1 == index) {

         return false;
        }
        if(index == subscribedCity){
            subscribedCity = -1;
            _listOfCityWeatherData.get(index).isSubscribed = false;
            return false;
        }
        subscribedCity = index;
        _listOfCityWeatherData.get(index).isSubscribed = true;
        return true;
    }
}
