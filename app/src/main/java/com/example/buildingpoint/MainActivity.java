package com.example.buildingpoint;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
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

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    final int ACTIVITY_SELECT_PICTURE = 1;
    final int ACTIVITY_RESULT = 2;
    /*
    //For camera surface
    private String cameraFileName;
    private static final boolean SAVE_TO_FILE = true;
    private SurfaceView cameraPreview;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private ImageView currentPhoto;
    private byte[] dataa;
*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //goToGallery();
        //Let us prepare the surface view

        /*cameraPreview = findViewById(R.id.surface_view);
        surfaceHolder = cameraPreview.getHolder();
        surfaceHolder.addCallback(this);
        openCamera(); */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        camera.release();
    }

    private void openCamera() {

        if ((camera = Camera.open(0)) == null) {
            Toast.makeText(getApplicationContext(), "Camera not available!",
                    Toast.LENGTH_LONG).show();
        } else {

//----This will make the surface be created
            cameraPreview.setVisibility(View.VISIBLE);
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
//----This will make the surface be changed
            camera.startPreview();
        } catch (Exception e) {
            //----Do something
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

        Camera.Parameters cameraParameters;
        boolean sizeFound;

        camera.stopPreview();
        sizeFound = false;
        cameraParameters = camera.getParameters();
        for (Camera.Size size : cameraParameters.getSupportedPreviewSizes()) {
            if (size.width == width || size.height == height) {
                width = size.width;
                height = size.height;
                sizeFound = true;
                break;
            }
        }
        if (sizeFound) {
            cameraParameters.setPreviewSize(width, height);
            camera.setParameters(cameraParameters);
        } else {
            Toast.makeText(getApplicationContext(),
                    "Camera cannot do " + width + "x" + height, Toast.LENGTH_LONG).show();

        }
        camera.startPreview();

    }
    public void surfaceDestroyed(SurfaceHolder holder) {

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
                    Intent resultIntent = new Intent();
                    resultIntent.setClassName("com.example.buildingpoint",
                            "com.example.buildingpoint.ResultPage");
                    resultIntent.putExtra("theURI", selectedURI.toString());
                    startActivityForResult(resultIntent, ACTIVITY_RESULT);
                }
                break;
            default:
                Log.i("ERROR", "U DONE MESSED UP");
                break;
        }
    }


}
