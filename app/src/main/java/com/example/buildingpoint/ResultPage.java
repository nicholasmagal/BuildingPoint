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
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
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

    //Integers for switching activities
    final int GOING_TO_GALLERY = 1;

    //Preview of the selected photo
    ImageView pictureZone;

    //Reference to Firestore database
    FirebaseFirestore db;

    //Building labels
    final String[] labels = {"Arts", "Cox", "McKnight", "Rainbow"};


    /**
     * First method called in Android lifecycle.  This method initializes our global variables, and prompts a user to grant permissions
     * for camera and location services.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance(); //getting an instance of the Firestore database
        setContentView(R.layout.activity_result_page); //setting the layout
        pictureZone = findViewById(R.id.preview_photo); //grabbing ImageView from xml
        try {
            Bitmap selectedPhoto = setImage(); //convert selected photo to bitmap
            getTFModel(selectedPhoto); //pass photo to ML model
        } catch (Exception e) {

        }
    }

    /**
     * Sets the image of the activity chosen by user in gallery activity.
     *
     * @return the image in a bitmap for the getTFModel
     * @throws Exception if the picture is unable to be set
     */
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

    /**
     * The method that contains the CNN machine learning model. Sets up the model.  This method creates a list of probabilities
     *  of different classes. These probabilities are passed into chooseDialog, which will decide what dialog to display to the user
     *  given if the classification was successful or not.
     *
     * @param bitmap the photo to be classified
     * @throws FirebaseMLException if the classifier fails, will throw exception
     */
    public void getTFModel(Bitmap bitmap) throws FirebaseMLException {
        //Getting the TensorFlow Model from assets folder
        FirebaseLocalModel localSource =
                new FirebaseLocalModel.Builder("my_local_model")  // Assign a name to this model in the program
                        .setAssetFilePath("model.tflite") //this is the name of the model in the assets folder
                        .build(); //building the model
        FirebaseModelManager.getInstance().registerLocalModel(localSource); //registering the local model with the FirebaseModelManager

        FirebaseModelOptions options = new FirebaseModelOptions.Builder()
                .setLocalModelName("my_local_model").build(); //options associated with the model
        FirebaseModelInterpreter firebaseInterpreter =
                FirebaseModelInterpreter.getInstance(options); //creating an interpreter for the model, given the options

        //Specifying the inputs and outputs of model
        FirebaseModelInputOutputOptions inputOutputOptions =
                new FirebaseModelInputOutputOptions.Builder()
                        .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 224, 224, 3}) //input is 1 image, 224x224 pixels, with 3 color channels (RBG)
                        .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 4}) //output is 1 array of 4 probabilities
                        .build(); //building inputs/outputs

        //Performing inference on the input data
        bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true); //this is the bitmap of the photo we took
        int batchNum = 0; //not feeding our model in batches but rather one image at a time
        float[][][][] input = new float[1][224][224][3]; //creating input for the photo we took
        for (int x = 0; x < 224; x++) {
            for (int y = 0; y < 224; y++) {
                int pixel = bitmap.getPixel(x, y);
                // Normalize channel values to [0,1.0]
                input[batchNum][x][y][0] = Color.red(pixel) / 255.0f;
                input[batchNum][x][y][1] = Color.green(pixel) / 255.0f;
                input[batchNum][x][y][2] = Color.blue(pixel) / 255.0f;
            } //the for loop is normalizing all the pixel values of our photo between 0 and 1 (as we did when training our model)
        }

        //Creating the input part of model
        FirebaseModelInputs inputs = new FirebaseModelInputs.Builder()
                .add(input)  // add() as many input arrays as your model requires
                .build(); //putting our input image in place for our model

        //Running the model
        firebaseInterpreter.run(inputs, inputOutputOptions) //given the input & options, our model will now run and give us an output
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseModelOutputs>() {
                            @Override
                            public void onSuccess(FirebaseModelOutputs result) { //on the successful running of our model ...
                                float[][] output = result.getOutput(0); //we get our output from the model
                                float[] probabilities = output[0]; //store our output in an array
                                //Log message to give us probabilities in the terminal
                                for (int i = 0; i < probabilities.length; i++) {
                                    Log.i("MLKit", String.format("%s: %1.4f", labels[i], probabilities[i]));
                                }
                                //We will now pass the probabilities from the ML model to a new function which will display either a background dialog if confidence is too low, or dialog of most probable building
                                chooseDialog(probabilities);

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() { //on our model failing to run, we log a failure message (should never happen given that the model is local)
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.i("ERROR", " FAILURE AT THE END");
                            }
                        });
    }

    /**
     * choose which dialog to display depending on returned probabilities from ML model. If probability of most probable building is <0.7,
     * a background noise dialog will be displayed, else, the dialog of the most probable building is displayed.
     *
     * @param probabilities these are the probabilities of classes returned from the CNN machine learning model from getTFModel
     */
    private void chooseDialog(float[] probabilities) {
        int maxProbBuildingIndex = predictionProb(probabilities); //get index of most probable building
        float maxProb = probabilities[maxProbBuildingIndex]; //get probability of most probable building

        if (maxProb < 0.7) { //if probability(confidence) is less than 0.7, show background noise dialog and return from execution
            backgroundDialogue(this);
            return;
        } else { //else, show dialog of most probable building
            getInfo(labels[maxProbBuildingIndex]);
        }
    }

    /**
     * Returns index of building with highest probability.
     *
     * @param probabilities these are the probabilities of classes returned from the CNN machine learning model from getTFModel
     * @return the index of the building with the highest probability
     */
    private int predictionProb(float[] probabilities) {
        float max = 0;
        int index = 100;
        int length = probabilities.length;
        for (int i = 0; i < length; i++) {
            if (probabilities[i] > max) {
                max = probabilities[i];
                index = i;
            }
        }
        return index;
    }

    /**
     * Given string label of building, a query for the particular building's document in FireStore database is initiated,
     * and all the building's info (name,dept,address,etc.) is passed to a function which will create dialog.
     *
     * @param label string label which refers to which building to construct dialog for
     */
    //given document of building, gets all the building's info (name,dept,address,etc.) and pass info to a function which will create dialog
    public void getInfo(String label) {
        Task<DocumentSnapshot> tds = db.collection("building").document(label).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot ds = task.getResult();

                Object bName = ds.get("name");
                Object bDepartment = ds.get("department");
                Object bAddress = ds.get("address");

                String Name = bName.toString();
                String Department = bDepartment.toString();
                String Address = bAddress.toString();

                showDialog(ResultPage.this, Name, Department, Address);
            }
        });
    }

    /**
     * Creating dialog given information of classified building. This will be called upon successful classification of image.
     *
     * @param activity   Which class we want the dialog to show in
     * @param Name       Name of building to display
     * @param Department Department name of building to display
     * @param Address    Address of building to display
     */
    public void showDialog(Activity activity, String Name, String Department, final String Address) {
        final Dialog dialog = new Dialog(activity); //create a new dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //no title
        dialog.setContentView(R.layout.layout_dialog2); //set the layout of the dialog (grabbing from xml)

        //Setting the TextView up
        TextView name = dialog.findViewById(R.id.name_holder); //grabbing name element from xml
        TextView dep = dialog.findViewById(R.id.department_holder); //grabbing department element from xml
        TextView address = dialog.findViewById(R.id.address_holder); //grabbing address element from xml
        Button exit = dialog.findViewById(R.id.exit_button); //grabbing exit element from xml
        Button home = dialog.findViewById(R.id.home_button); //grabbing home element from xml

        name.setText(Name); //set the name of the building
        name.setTextColor(Color.WHITE); //set color to white

        dep.setText(Department); //set the department of the building
        dep.setTextColor(Color.WHITE); //set color to white

        address.setText(Address); //set the address of the building
        address.setClickable(true); //allow to click (will open google maps and query using address)
        address.setTextColor(Color.YELLOW); //set color to yellow

        //underline the address to emphasize that it is clickable (like a link)
        SpannableString content = new SpannableString(Address);
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        address.setText(content);

        //when you click on the address, a new GoogleMap intent is started, which will open Google Maps and query maps with the address
        address.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(Address));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });

        Window window = dialog.getWindow(); //get window of dialog
        WindowManager.LayoutParams wlp = window.getAttributes(); //grab current attributes of window

        //overwrite some attributes ...
        wlp.gravity = Gravity.BOTTOM; //stick dialog on the bottom
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND; //remove darkened screen
        wlp.windowAnimations = R.style.DialogAnimation_2; //add animation to the dialog window (slide up upon arrival & slide down upon exit)
        wlp.width = WindowManager.LayoutParams.MATCH_PARENT; //setup width of the dialog to match parent
        window.setAttributes(wlp); //write back all the attributes to the dialog window

        dialog.show(); //showing the dialog

        //setting up the button to exit the dialog
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //upon exiting the dialog ...
                dialog.dismiss(); //dismiss the dialog
                goToGallery(); //go to gallery
            }
        });

        //setting up button to go back to camera
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goHome(); //go back to camera
            }
        });
    }

    /**
     * Creating dialog alerting user of background noise. This will be called upon unsuccessful classification of image.
     *
     * @param activity Which class we want the dialog to show in
     */
    public void backgroundDialogue(Activity activity) {
        final Dialog dialog2 = new Dialog(activity); //create a new dialog
        dialog2.requestWindowFeature(Window.FEATURE_NO_TITLE); //no title
        dialog2.setContentView(R.layout.background_dialogue2); //set the layout of the dialog (grabbing from xml)

        Button exit2 = dialog2.findViewById(R.id.exit_button2); //grabbing exit element from xml
        Button home2 = dialog2.findViewById(R.id.home_button2); //grabbing home element from xml

        Window window = dialog2.getWindow(); //get window of dialog
        WindowManager.LayoutParams wlp = window.getAttributes(); //grab current attributes of window

        //overwrite some attributes ...
        wlp.gravity = Gravity.BOTTOM; //stick dialog on the bottom
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND; //remove darkened screen
        wlp.windowAnimations = R.style.DialogAnimation_2; //add animation to the dialog window (slide up upon arrival & slide down upon exit)
        wlp.width = WindowManager.LayoutParams.MATCH_PARENT; //setup width of the dialog to match parent
        window.setAttributes(wlp); //write back all the attributes to the dialog window

        dialog2.show(); //showing the dialog

        //setting up the button to exit the dialog
        exit2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //upon exiting the dialog ...
                dialog2.dismiss(); //dismiss the dialog
                goToGallery(); //go to gallery
            }
        });

        //setting up button to go back to camera
        home2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goHome(); //go back to camera
            }
        });
    }

    /**
     * Creates a intent that will take user to gallery page.
     */
    private void goToGallery() {
        Intent galleryIntent;
        galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GOING_TO_GALLERY);
    }

    /**
     * Called when returning from different activity. Done when picture is selected after going back to gallery multiple times (first time is handled by onCreate).
     *
     * @param requestCode The activity code we are returning from
     * @param resultCode  The indication if we returned successfully from activity
     * @param data        The data passed from previous activity
     */
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

                    }
                } else {
                    goHome();
                }
                break;
            default:
                break;

        }
    }

    /**
     * Action to bring activity back home (camera).
     */
    public void goHome() {
        Intent homeIntent = new Intent();
        homeIntent.setClass(this, MainActivity.class);
        startActivityForResult(homeIntent, 3);
    }
}
