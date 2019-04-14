package com.example.buildingpoint;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, Camera.PictureCallback {

    final int ACTIVITY_SELECT_PICTURE = 1;
    final int ACTIVITY_RESULT = 2;
    //Camera Stuff
    private Camera mCamera;
    private TextureView mTextureView;
    private boolean cameraInUse;
    private boolean canTakePhoto;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    String counter;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        cameraInUse = false;
        canTakePhoto = false;
        checkPermission();
        if (counter==null){
            WelcomeMessage(mTextureView);
        }

    }

    public void createTexture() {
        //Let us prepare the Texture View
        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);
        setContentView(mTextureView);

        mTextureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Tap", "U r clicking");
                if (canTakePhoto) {
                    mCamera.takePicture(null, null, null, MainActivity.this);
                    mTextureView.setClickable(false);
                    canTakePhoto = false;
                }
            }
        });
    }

    private void checkPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            canTakePhoto = true;
            //openCamera();

        } else {
            /*
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                Toast.makeText(this, "Camera permission is needed to show the camera preview.", Toast.LENGTH_SHORT).show();
            }
            */
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_IMAGE_CAPTURE);


            checkPermission();


        }
    }

    public static int getId() {
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                Log.d("ID", "Camera found");
                cameraId = i;
                break;
            }
        }
        Log.i("ID", " " + cameraId);
        return cameraId;
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }


    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Bitmap myPhoto;
        myPhoto = BitmapFactory.decodeByteArray(data, 0, data.length);
        mCamera.startPreview();

        try {
            Log.i("PIC", "RED");
            getTFModel(myPhoto);
            canTakePhoto = true;
        } catch (Exception e) {
            Log.i("FireBaseFail", "Failed");
        }


    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
            openCamera();
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
        }

    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        //mCamera.stopPreview();
        //mCamera.release();
        return true;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!cameraInUse) {
            createTexture();
            openCamera();
            Log.i("RESUME", "ENTERED");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (cameraInUse) {
            closeCamera();
            releaseCameraAndPreview();
            Log.i("RESUME", "Entered onPause");
        }

    }

    private void releaseCameraAndPreview() {

        if (mTextureView != null) {
            mTextureView.destroyDrawingCache();
        }
    }



    private void openCamera() {
        int cameraID = getId();

        if ((mCamera = Camera.open(cameraID)) == null) {
            Toast.makeText(getApplicationContext(), "Camera not available!",
                    Toast.LENGTH_LONG).show();
            return;
        }


        setCameraDisplayOrientation(this, cameraID, mCamera);
        cameraInUse = true;
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
            case 3:
                if (resultCode == Activity.RESULT_OK) {
                    counter="HELLO";
                    Log.i("RETURN","HI");
                }


            default:
                Log.i("ERROR", "U DONE MESSED UP");
                break;
        }
    }

    public void getTFModel(Bitmap bitmap) throws FirebaseMLException {
        final TextView myResult = findViewById(R.id.result);
        Log.i("SUCCESS", "-1");

        //Getting the TensorFlowModel from assets folder

        FirebaseLocalModel localSource =
                new FirebaseLocalModel.Builder("my_local_model")  // Assign a name to this model
                        .setAssetFilePath("model.tflite")
                        .build();
        FirebaseModelManager.getInstance().registerLocalModel(localSource);


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
                .setLocalModelName("my_local_model").build();
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
                                String[] labels = {"Arts", "Cox", "McKnight", "Rainbow"};

                                for (int i = 0; i < probabilities.length; i++) {
                                    try {
                                        BufferedReader reader = new BufferedReader(
                                                new InputStreamReader(getAssets().open("train.txt")));
                                        String label = reader.readLine();


                                        Log.i("MLKit", String.format("%s: %1.4f", labels[i], probabilities[i]));


                                    } catch (Exception e) {
                                        Log.i("MLKIT", "FAIL");
                                    }
                                    //String resultForDisplay=predictionProb(probabilities,labels);
                                    //Toast.makeText(getApplicationContext(), resultForDisplay, Toast.LENGTH_SHORT).show();


                                }
                                getInfo(predictionProb(probabilities, labels),getApplicationContext());

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

    private String predictionProb(float[] probabilities, String[] labels) {
        float max = 0;
        int index = 100;
        int length = probabilities.length;
        for (int i = 0; i < length; i++) {
            if (probabilities[i] > max) {
                max = probabilities[i];
                index = i;
            }
        }
        //String resultForAndroid=String.format("%s: %1.4f",labels[index] ,max);
        //return resultForAndroid;
        return labels[index];
    }

    private void goToGallery() {
        Intent galleryIntent;
        galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, ACTIVITY_SELECT_PICTURE);
    }

    public void getInfo(String label, final Context myContext) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        //Task<DocumentSnapshot> tds = db.collection("buildings").document(label).get();
        Task<DocumentSnapshot> tds = db.collection("building").document(label).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot ds = task.getResult();
                Object bName = ds.get("name");
                Object bDepartment = ds.get("department");
                Object bAddress = ds.get("address");
                String resultForDisplay = bName.toString() + "\n" + bDepartment.toString() + "\n" + bAddress.toString();
                //Toast.makeText(myContext, resultForDisplay, Toast.LENGTH_SHORT).show();
                String Name = bName.toString();
                String Department = bDepartment.toString();
                String Address = bAddress.toString();


                //openDialog(Name, Department, Address);

                showDialog(MainActivity.this, Name,Department,Address);
            }
        });
    }

    public void openDialog (String Name, String Department, String Address){
        BuildingDialog buildingDialog = new BuildingDialog();

        Bundle args = new Bundle();
        args.putString("Name", Name);
        args.putString("Department", Department);
        args.putString("Address", Address);
        buildingDialog.setArguments(args);
        buildingDialog.show(getSupportFragmentManager(), "exampledialog");

    }




    public void WelcomeMessage(View view) {
        String welcomemessage = "Please click on the screen to identify a building and see its information \n";
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Welcome to BuildingPoint!");
        alertDialogBuilder.setMessage(welcomemessage);
        alertDialogBuilder.setPositiveButton("continue",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        //  Toast.makeText(MainActivity.this,"Let's go!",Toast.LENGTH_LONG).show();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater;

        inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return (true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home_id:
                goToGallery();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void showDialog(Activity activity, String Name, String Department, String Address) {
        Log.i("DialogC","It is done");
        mTextureView.setClickable(false);
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.layout_dialog);

        //Setting the textView up
        TextView name = dialog.findViewById(R.id.name_holder);
        TextView dep = dialog.findViewById(R.id.department_holder);
        TextView address = dialog.findViewById(R.id.address_holder);
        Button exit = dialog.findViewById(R.id.exit_button);


        name.setText(Name);
        name.setTextColor(Color.WHITE);
        dep.setText(Department);
        dep.setTextColor(Color.WHITE);
        address.setText(Address);
        address.setTextColor(Color.WHITE);


        //Stick it on the bottem and resume the darkened screen
        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.BOTTOM;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);

        //Setting the dialog to be the entire horiztal screen

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;


        //Showing the dialog
        dialog.show();

        //Setting the backgroundcolor
        //int color=ContextCompat.getColor(ViewDialog.this,R.color.myOrange);


        // dialog.getWindow().setBackgroundDrawableResoure(color);
        //setting the horizontal portion
        dialog.getWindow().setAttributes(lp);
        //Click handler for exiting


        //Setting up the button to exit the dilog
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();
                mTextureView.setClickable(true);

            }
        });

    }




}