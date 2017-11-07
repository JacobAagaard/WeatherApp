package com.flonk.weatherapp;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;

public class CityDetailsActivity extends AppCompatActivity {

    Button buttonOK, buttonRemove;
    TextView textViewCityName, textViewHumidity, textViewPercentage, textViewTemperature, textViewCelsius;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city_details);

        sharedPreferences = this.getSharedPreferences(CityListActivity.FILENAME , MODE_PRIVATE);

        buttonOK = findViewById(R.id.buttonOK);
        buttonRemove = findViewById(R.id.buttonRemove);
        textViewCityName = findViewById(R.id.textViewCityName);
        textViewHumidity = findViewById(R.id.textViewHumidity);
        textViewPercentage = findViewById(R.id.textViewPercentage);
        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewCelsius = findViewById(R.id.textViewCelsius);

        textViewCityName.setText(sharedPreferences.getString(
                "#"+getIntent().getIntExtra("City_ID", -1),"City Name not found"));

        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_OK);
                finish();
            }
        });

        buttonRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_OK);
                Method[] methods = CityListActivity.class.getDeclaredMethods();
                //Remove based on City ID received from List Activity
            }
        });
    }
}
