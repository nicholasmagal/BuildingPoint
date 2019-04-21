package com.example.buildingpoint;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); //set the layout of the splash screen (grabbing from xml)
        ImageView mySplash=findViewById(R.id.splash_screen); //grabbing splash screen element from xml
        mySplash.setScaleType(ImageView.ScaleType.FIT_XY); //fits background to the screen

        //setting timer for activity to finish (3 seconds)
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                Intent myIntent = new Intent();
                myIntent.setClassName("com.example.buildingpoint","com.example.buildingpoint.MainActivity");

                startActivity(myIntent);
                finish(); } }, 3000);
    }
}
