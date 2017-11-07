package com.flonk.weatherapp;

/**
 * Created by Jacob on 07/11/2017.
 */

public class CityWeatherData {
    public int ID;
    // Name is made final to use the name in the sharedpref as the name og the pref: and when doing so it cannot change, else the pref will not return
    public final String Name;
    public float Temperature;
    public float Humidity;
    public String Description;

    public CityWeatherData(int id, String name, float temp, float humidity, String description){
        ID = id;
        Name = name;
        Temperature = temp;
        Humidity = humidity;
        Description = description;
    }

    public CityWeatherData(){
        ID = -1;
        Name = "DefaultCity";
        Temperature = 0.0f;
        Humidity = 0.0f;
        Description = "DefaultDescription";
    }

}
