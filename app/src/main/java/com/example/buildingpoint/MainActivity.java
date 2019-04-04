package com.example.buildingpoint;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
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
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.AsyncTask;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener,Camera.PictureCallback {

    final int ACTIVITY_SELECT_PICTURE = 1;
    final int ACTIVITY_RESULT = 2;
    //Camera Stuff
    private Camera mCamera;
    private TextureView mTextureView;
    private boolean cameraInUse;
    private boolean canTakePhoto;
    static final int REQUEST_IMAGE_CAPTURE = 1;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraInUse=false;
        canTakePhoto=true;
        //goToGallery();
        //Let us prepare the Texture View
        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);



        setContentView(mTextureView);
        openCamera();

        mTextureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Tap", "U r clicking");
                if (canTakePhoto){
                    mCamera.takePicture(null,null, null,MainActivity.this);
                    canTakePhoto=false;
                }
            }
        });

    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Bitmap myPhoto;
        myPhoto = BitmapFactory.decodeByteArray(data,0,data.length);
        mCamera.startPreview();
        try{
            getTFModel(myPhoto);
            canTakePhoto=true;
        }
        catch (Exception e){
            Log.i("FireBaseFail","Failed");
        }

    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        mCamera.setDisplayOrientation(270);

        try {
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
        }
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
        try {
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
        }
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.stopPreview();
        mCamera.release();
        return true;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!cameraInUse) {
            openCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraInUse) {
            closeCamera();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (cameraInUse){
            closeCamera();
        }
    }

    private void openCamera() {

        if ((mCamera = Camera.open(0)) == null) {
            Toast.makeText(getApplicationContext(),"Camera not available!",
                    Toast.LENGTH_LONG).show();
        }
        cameraInUse=true;
    }

    private void closeCamera() {

        mCamera.stopPreview();
        mCamera.release();
        cameraInUse = false;
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

    public void getTFModel(Bitmap bitmap) throws FirebaseMLException {
        final TextView myResult = findViewById(R.id.result);
        Log.i("SUCCESS", "-1");
/*
        //Getting the TensorFlowModel from assets folder

        FirebaseLocalModel localSource =
                new FirebaseLocalModel.Builder("my_local_model")  // Assign a name to this model
                        .setAssetFilePath("model.tflite")
                        .build();
        FirebaseModelManager.getInstance().registerLocalModel(localSource);

*/

        //Getting the model from FireBase

        FirebaseModelDownloadConditions.Builder conditionsBuilder =
                new FirebaseModelDownloadConditions.Builder().requireWifi();

        FirebaseModelDownloadConditions conditions = conditionsBuilder.build();


        FirebaseRemoteModel cloudSource = new FirebaseRemoteModel.Builder("building-detector")
                .enableModelUpdates(true)
                .setInitialDownloadConditions(conditions)
                .setUpdatesDownloadConditions(conditions)
                .build();
        FirebaseModelManager.getInstance().registerRemoteModel(cloudSource);
        Log.i("SUCCESS", "0");


        //Creating the interperter from the model

        FirebaseModelOptions options = new FirebaseModelOptions.Builder()
                .setRemoteModelName("building-detector").build();
        FirebaseModelInterpreter firebaseInterpreter =
                FirebaseModelInterpreter.getInstance(options);
        Log.i("SUCCESS", "0.5");

        //Process

        FirebaseModelInputOutputOptions inputOutputOptions =
                new FirebaseModelInputOutputOptions.Builder()
                        .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 224, 224, 3})
                        .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 4})
                        .build();


        Log.i("SUCCESS", "1");


        //Performing Inference on Input data

        bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        int batchNum = 0;
        float[][][][] input = new float[1][224][224][3];
        for (int x = 0; x < 224; x++) {
            for (int y = 0; y < 224; y++) {
                int pixel = bitmap.getPixel(x, y);
                // Normalize channel values to [0,1.0]
                input[batchNum][x][y][0] = Color.red(pixel) / 255.0f;
                input[batchNum][x][y][1] = Color.green(pixel) / 255.0f;
                input[batchNum][x][y][2] = Color.blue(pixel) / 255.0f;
            }
        }
        Log.i("SUCCESS", "2");
        //Creating a input model
        FirebaseModelInputs inputs = new FirebaseModelInputs.Builder()
                .add(input)  // add() as many input arrays as your model requires
                .build();

        firebaseInterpreter.run(inputs, inputOutputOptions)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseModelOutputs>() {
                            @Override
                            public void onSuccess(FirebaseModelOutputs result) {
                                Log.i("SUCCESS", "4");
                                float[][] output = result.getOutput(0);
                                float[] probabilities = output[0];
                                String[] labels = {"Arts", "Cox", "Mcknighnight", "Rainbow"};

                                for (int i = 0; i < probabilities.length; i++) {
                                    try {
                                        BufferedReader reader = new BufferedReader(
                                                new InputStreamReader(getAssets().open("train.txt")));
                                        String label = reader.readLine();

                                        Log.i("MLKit", String.format("%s: %1.4f", labels[i], probabilities[i]));
                                    } catch (Exception e) {
                                        Log.i("MLKIT", "FAIL");
                                    }

                                }

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.i("ERROR", " FAILURE AT THE END");
                            }
                        });
        Log.i("SUCCESS", "3");


    }

    private void goToGallery() {
        Intent galleryIntent;
        galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, ACTIVITY_SELECT_PICTURE);
    }


}
