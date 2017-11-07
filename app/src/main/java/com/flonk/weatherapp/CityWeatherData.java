package com.flonk.weatherapp;

/**
 * Created by Jacob on 07/11/2017.
 */

public class CityWeatherData {
    private int _id;
    private String _name;
    private float _temperature;
    private float _humidity;
    private String _description;


    public CityWeatherData(){
        _id = -1;
        _name = "DefaultCity";
        _temperature = 0.0f;
        _humidity = 0.0f;
        _description = "DefaultDescription";
    }
}
