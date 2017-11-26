package com.flonk.weatherapp;

import android.net.NetworkInfo;

/**
 * Created by Jacob on 07/11/2017.
 * Inspired by https://developer.android.com/training/basics/network-ops/connecting.html
 */

public interface WeatherQueryCallback {
    /**
     * Indicates that the callback handler needs to update its appearance or information based on
     * the result of the task. Expected to be called from the main thread.
     */
    void QueryResult(WeatherQueryHelper.WeatherQueryResult result);
}
