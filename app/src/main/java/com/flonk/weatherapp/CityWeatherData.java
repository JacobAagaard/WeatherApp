package com.flonk.weatherapp;

/**
 * Created by Jacob on 07/11/2017.
 */

public class CityWeatherData {
    // Name is made final to use the name in the sharedpref as the name og the pref: and when doing so it cannot change, else the pref will not return
    public final String Name;
    public String Temperature;
    public String Humidity;
    public String Description;
    public String Icon;
    public String TimeStamp;

    public CityWeatherData(String name, String temp, String humidity, String description, String icon, String timeStamp){
        Name = name;
        Temperature = temp;
        Humidity = humidity;
        Description = description;
        Icon = icon;
        TimeStamp = timeStamp;
    }

    public CityWeatherData(){
        Name = "DefaultCity";
        Temperature = "0.0";
        Humidity = "0.0";
        Description = "DefaultDescription";
        Icon = "04n";
        TimeStamp = "1999-01-01: 00:00:00";
    }

}
