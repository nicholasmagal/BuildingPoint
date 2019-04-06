package com.example.buildingpoint;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                Intent myIntent = new Intent();
                myIntent.setClassName("com.example.buildingpoint","com.example.buildingpoint.MainActivity");

                startActivity(myIntent);
                finish(); } }, 3000);
    }
}
