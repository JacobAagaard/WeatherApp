package com.flonk.weatherapp;

import java.util.ArrayList;

/**
 * Created by Frederik on 11/9/2017.
 */

// class that contains a list of all CityWeatherData.
// It is used by Gson class to serialize and deserialize the entire object and then save it as a string in SharedPreference
public class AllCitiesWeather{
    // lists of all cities
    private ArrayList<CityWeatherData> _listOfCityWeatherData;

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

        // retrieves the old state of the cityData
        CityWeatherData oldCity = _listOfCityWeatherData.get(index);

        // sets the the element with new cityData (this only contains new weather data and not the state in terms of subscription)
        _listOfCityWeatherData.set(index, data);

        // Sets the state if the cityData with the old state
        _listOfCityWeatherData.get(index).scheduledNotificationTime = oldCity.scheduledNotificationTime;
        _listOfCityWeatherData.get(index).isSubscribed = oldCity.isSubscribed;

        return true;
    }

    // gets city weather data for the specified cityName
    public CityWeatherData GetCityWeatherData(String cityName){
        int index = GetIndexOfCity(cityName);

        if(index == -1) return null;

        return _listOfCityWeatherData.get(index);
    }

    // returns the entire list of cities
    public ArrayList<CityWeatherData> GetAllCitiesWeatherData(){
        return _listOfCityWeatherData;
    }

    public boolean CityExists(String cityName){
        if(GetIndexOfCity(cityName) == -1) return false;
        return true;
    }

    // returns the index of the specified city
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

    // returns the CityWeatherData that is subscribed
    // OBS: as for now, this functionality is implemented, so that only one city can be subscribed on, hence the no-parameter.
    public CityWeatherData GetSubscribedCity(){
        for (int i = 0; i < _listOfCityWeatherData.size() ; i++){
            CityWeatherData cityData = _listOfCityWeatherData.get(i);
            if(cityData.isSubscribed) return cityData;
        }
        return null;
    }

    // subscribes the specified city.
    public void SubscribedCity(String cityName){
        CityWeatherData cityData = _listOfCityWeatherData.get(GetIndexOfCity(cityName));
        cityData.isSubscribed = true;
    }

    // unsubscribes the specified city
    public void UnSubScribeCity(String name){
        CityWeatherData cityData = _listOfCityWeatherData.get(GetIndexOfCity(name));
        cityData.isSubscribed = false;
        cityData.scheduledNotificationTime = null;
    }
}
