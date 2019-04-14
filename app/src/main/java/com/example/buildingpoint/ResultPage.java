package com.example.buildingpoint;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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

public class ResultPage extends AppCompatActivity {

    final int GOING_TO_GALLERY = 5;
    ImageView pictureZone ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_page);
        pictureZone= findViewById(R.id.preview_photo);
        try {
            Bitmap selectedPhoto = setImage();
            getTFModel(selectedPhoto);
        } catch (Exception e) {
            Log.i("PIC", "could not be set");
        }


    }


    //Sets the image of the activity
    public Bitmap setImage() throws Exception {
        Bitmap pictureMap;
        pictureZone = findViewById(R.id.preview_photo);

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
        switch (item.getItemId()) {
            case R.id.live_id:
                goHome();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    //Action to bring activity back home
    public void goHome() {
        Intent homeIntent = new Intent();
        homeIntent.setClass(this, MainActivity.class);
        startActivityForResult(homeIntent, 3);

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
                                getInfo(predictionProb(probabilities, labels));

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

    public void getInfo(String label) {
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

                showDialog(ResultPage.this, Name, Department, Address);
            }
        });
    }

    public void showDialog(Activity activity, String Name, String Department, final String Address) {
        Log.i("DialogC", "It is done");
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.layout_dialog);

        //Setting the textView up
        TextView name = dialog.findViewById(R.id.name_holder);
        TextView dep = dialog.findViewById(R.id.department_holder);
        TextView address = dialog.findViewById(R.id.address_holder);
        Button exit = dialog.findViewById(R.id.exit_button);
        exit.setText("Back to Gallery");


        name.setText(Name);
        name.setTextColor(Color.WHITE);
        dep.setText(Department);
        dep.setTextColor(Color.WHITE);
        address.setText(Address);
        address.setClickable(true);
        address.setTextColor(Color.WHITE);

        address.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(Address));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });


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
                goToGallery();


            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri selectedURI;
        Bitmap newSelectedPhoto;
        switch (requestCode) {
            case GOING_TO_GALLERY:
                if (resultCode == Activity.RESULT_OK) {
                    selectedURI = data.getData();

                    try {
                        newSelectedPhoto = MediaStore.Images.Media.getBitmap(
                                this.getContentResolver(), selectedURI);
                        pictureZone.setImageBitmap(newSelectedPhoto);
                        getTFModel(newSelectedPhoto);
                    } catch (Exception e) {
                        Log.i("settingPhoto", "not working");
                    }
                }
                break;
            default:
                break;

        }
    }

    private void goToGallery() {
        Intent galleryIntent;
        galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GOING_TO_GALLERY);
    }

}
