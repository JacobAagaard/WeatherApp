package com.flonk.weatherapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class WeatherService extends Service {
    public WeatherService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

}
