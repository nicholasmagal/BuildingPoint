package com.example.buildingpoint;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;

import org.w3c.dom.Text;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    final int ACTIVITY_SELECT_PICTURE = 1;
    final int ACTIVITY_RESULT = 2;
    Bitmap bitmap;



    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        goToGallery();
       /*
        try{
            getTFModel();
            Log.i("SUCCESS", "getTFModel works");
        }
        catch (Exception e){
            Log.i("ERROR","Error in the onCreate");
        }
*/

    }



    private void goToGallery() {
        Intent galleryIntent;
        galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, ACTIVITY_SELECT_PICTURE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Variables
        ImageView displayedPicture = findViewById(R.id.preview_photo);
        Uri selectedURI;
        super.onActivityResult(requestCode, resultCode, data);
        //If it works
        switch (requestCode) {
            case ACTIVITY_SELECT_PICTURE:
                if (resultCode == Activity.RESULT_OK) {
                    selectedURI = data.getData();
                    Intent resultIntent=new Intent();
                    resultIntent.setClassName("com.example.buildingpoint",
                            "com.example.buildingpoint.ResultPage");
                    resultIntent.putExtra("theURI",selectedURI.toString());
                    startActivityForResult(resultIntent,ACTIVITY_RESULT);
                }
                break;
            default:
                Log.i("ERROR","U DONE MESSED UP");
                break;
        }
    }


}
