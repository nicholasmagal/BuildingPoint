package com.example.buildingpoint;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.common.modeldownload.FirebaseRemoteModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ResultPage extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_page);
        try{
            setImage();
        }
        catch (Exception e){
            Log.i("PIC","could not be set");
        }

        // MainActivity myActivity=new MainActivity();
        //myActivity.getInfo("Cox",getApplicationContext());
    }

    public void getEstimate(){
        
    }
    //Sets the image of the activity
    public Bitmap setImage() throws Exception {
        Bitmap pictureMap;
        ImageView pictureZone = findViewById(R.id.preview_photo);

        String selectedURII = this.getIntent().getStringExtra("theURI");
        Uri uriFromString = Uri.parse(selectedURII);

        pictureMap = MediaStore.Images.Media.getBitmap(
                this.getContentResolver(), uriFromString);
        pictureZone.setImageBitmap(pictureMap);

        return pictureMap;
    }
    //Creates option at the action bar
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater;

        inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_camera, menu);



        return (true);
    }
    //Options for selecting the menu that calls the method to bring app back home
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.live_id:
                goHome();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
    //Action to bring activity back home
    public void goHome(){
        Intent homeIntent=new Intent();
        homeIntent.setClass(this,MainActivity.class);
        startActivityForResult(homeIntent,3);

    }


}
