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
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;
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
import com.google.type.LatLng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, Camera.PictureCallback {

    //Integers for switching activities
    final int ACTIVITY_SELECT_PICTURE = 1;
    final int ACTIVITY_RESULT = 2;

    //Integer for requesting permissions
    final int REQUEST_PERMISSIONS = 3;

    //Camera components
    private Camera mCamera;
    private TextureView mTextureView;
    private boolean cameraInUse;
    private boolean canTakePhoto;

    //Boolean which controls when we are allowed to switch to gallery page
    boolean activitySwitchGuard;

    //Boolean which allows us to prompt the user with a welcome message ONLY when the app first opens
    static boolean checkFirstTime = true;

    //Reference to Firestore database
    FirebaseFirestore db;

    //User geolocation provider
    private FusedLocationProviderClient fusedLocationClient;

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this); //getting an instance of FusedLocationProvider
        setContentView(R.layout.activity_main); //setting the layout
        activitySwitchGuard = true; //initial value for activity switch guard (will allow us to switch to gallery for now)
        cameraInUse = false; //initially camera not in use
        canTakePhoto = false; //initially cannot take photo (need permission)
        checkPermission(); //ask user for Camera & Location permission

        //if this is user's first time opening app, display welcome message
        if (checkFirstTime == true) {
            WelcomeMessage(mTextureView);
            checkFirstTime = false;
        }
    }


    /**
     * This method will display the welcome message to the user.
     *
     * @param view The view where this will be displayed
     */
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


    /**
     * Asks for user permission regarding camera usage and location services.
     */
    private void checkPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            canTakePhoto = true; //if user already has camera & location permission, set canTakePhoto to true and exit function (continue app execution)
        } else { //else, if permission is not granted, ask for camera & location permission, and recursively call function which will check once again if user has allowed permission
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
            checkPermission();
        }
    }

    /**
     * done after onCreate in Android LifeCycle.  Calls createTexture() and openCamera() which sets up camera and previewing of camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (!cameraInUse) { //camera should not yet be in use, else we do not need to do this stuff again
            createTexture(); //create the texture which the camera will be setup upon
            openCamera(); //open the camera
        }
    }

    /**
     * Method for creating the TextureView.  TextureView is what we are using to host our camera. We also set up the click listener for the TextureView
     * that allows us to capture a photo on click.
     */
    public void createTexture() {
        //Let us prepare the Texture View
        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);
        setContentView(mTextureView);

        //when you click on the screen ...
        mTextureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (canTakePhoto) {
                    mCamera.takePicture(null, null, null, MainActivity.this); // take a picture
                    mTextureView.setClickable(false); // make the texture un-clickable now while picture is being processed (sent to ML model), prevents app from being overloaded/crashing
                    canTakePhoto = false; // set canTakePhoto to false so no new pictures can be processed as well
                }
            }
        });
    }

    /**
     * This method opens up the camera. This method opens up a camera and sets the orientation by calling setCameraDisplayOrientation
     * which makes our camera upright.
     */
    private void openCamera() {
        int cameraID = getId(); //first need to get cameraID

        //if cannot open camera, let user know camera is not available and return
        if ((mCamera = Camera.open(cameraID)) == null) {
            Toast.makeText(getApplicationContext(), "Camera not available!",
                    Toast.LENGTH_LONG).show();
            return;
        }

        setCameraDisplayOrientation(this, cameraID, mCamera); //set camera orientation to be upright
        cameraInUse = true; //now that camera is open, it is now in use
    }


    /**
     * cycles through cameras until it finds ID of available back-facing camera, which we need.  This method is called by the openCamera method.
     *
     * @return the integer id of the back available camera
     */
    public static int getId() {
        int cameraId = -1; //initial camera ID
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) { //loop until you find cameraID of back-facing camera
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        return cameraId; //return ID of back-facing camera
    }

    /**
     * This method sets the camera orientation. Depending on the current rotation of the camera, the method will rotate the camera
     * until it is in the upright position.
     *
     * @param activity This is the context where the camera is being opened up in
     * @param cameraId This is the camera Id of the camera that we are fixing the orientation of
     * @param camera   This is the instance of the camera that we are modifying
     */
    //given the back-facing camera we want to use, this function will check the display and rotate it if necessary to make sure it is facing upright.
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

    /**
     * Invoked when a TextureView's SurfaceTexture is ready for use.
     *
     * @param surface The surface instance
     * @param width   The width of the surface
     * @param height  The height of the surface
     */
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
            openCamera(); //when surface texture is available, open camera if not already open
            mCamera.setPreviewTexture(surface); //set the texture for the preview (what we will be seeing)
            mCamera.startPreview(); //start the preview (let us see)
        } catch (IOException ioe) {
            // Something bad happened
        }
    }

    /**
     * Invoked when the SurfaceTexture's buffers size changed.
     *
     * @param surface The surface instance
     * @param width   The width of the surface
     * @param height  The height of the surface
     */
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    /**
     * Invoked when the specified SurfaceTexture is about to be destroyed.
     *
     * @param surface The surface instance
     * @return a boolean indicating successful destroyed surface
     */
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    /**
     * Invoked when the specified SurfaceTexture is updated through SurfaceTexture#updateTexImage().
     *
     * @param surface The surface instance
     */
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
    }

    /**
     * Called as part of the activity lifecycle when an activity is going into the background, but has not (yet) been killed. The counterpart to onResume.
     * This method ensures that the camera has been closed so it is not open while the user is not in the app.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (cameraInUse) { //when paused, only if the camera is in use (else there is nothing to do to the camera if it is not in use)
            closeCamera(); //close the camera (which is created again in onResume)
            releasePreview(); //release preview (which is created again in onResume, and when the texture becomes available again in onSurfaceTextureAvailable, the preview will start)
        }
    }

    /**
     * Stops the camera from previewing and releases the camera.  Updates boolean value of cameraInUse.
     */
    private void closeCamera() {
        mCamera.stopPreview(); //stop preview (dont need to see anymore)
        mCamera.release(); //release the camera
        cameraInUse = false; //camera is no longer in use
    }

    /**
     * Destroys the TextureView.
     */
    private void releasePreview() {
        if (mTextureView != null) { //while there is a TextureView to destroy ...
            mTextureView.destroyDrawingCache(); //destroy it
        }
    }

    /**
     * Called when user takes a photo. Once user has taken photo, convert photo into a bitmap where it is then passed into
     * the getTFModel, which is the TensorFlow machine learning model for image classification. This is the first method
     * of our classification chain. The classification chain takes probabilities measured by CNN and refines them based on geolocation.
     *
     * @param data   this is photo data stored into a Byte array
     * @param camera the current camera instance
     */
    @Override
    public void onPictureTaken(byte[] data, Camera camera) { //when a picture is taken ...
        activitySwitchGuard = false; //cannot switch to gallery now because we will begin processing the image
        Bitmap myPhoto; //create a bitmap for the photo taken, which is required for the ML model
        myPhoto = BitmapFactory.decodeByteArray(data, 0, data.length); //change the photo taken into a bitmap, and store the result in the bitmap created on the previous line
        try {
            getTFModel(myPhoto); //send the photo to the ML model to kickoff the inference chain
            canTakePhoto = true; //once done, we can now begin to take more photos
        } catch (Exception e) {

        }
    }

    /**
     * The method that contains the CNN machine learning model. Sets up the model.  This method creates a list of probabilities
     * of different classes.  These probabilities are passed into getLocation, the second method in the classification chain.
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
                                //We will now pass the probabilities from the ML model to a new sequence of functions which will modify the probabilities given the user's geolocation
                                getLocation(probabilities);

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
     * Gets the current location of the user.  This is then passed into getBuildings, the third method in the classification
     * chain.
     *
     * @param probabilities these are the probabilities of classes returned from the CNN machine learning model from getTFModel
     */
    private void getLocation(final float[] probabilities) {
        try {
            fusedLocationClient.getLastLocation() //attempt to get the location of the user
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(final Location location) {
                            //Got last known location. In some rare situations this can be null.

                            if (location != null) { //if the location does not return back as null
                                getBuildings(probabilities, location); //call the next function which will build upon the user's location by
                                //querying the database for the locations of the different buildings
                            }
                        }
                    });
        } catch (SecurityException e) {

        }
    }

    /**
     * Gets a QuerySnapshot of all buildings in FireStore database. Passes a list of documents which refer to each building to getDistanceToBuildings, the fourth
     * classification chain method.
     *
     * @param probabilities these are the probabilities of classes returned from the CNN machine learning model from getTFModel
     * @param uLoc User location captured from getLocation Method.
     */
    private void getBuildings(final float[] probabilities, final Location uLoc) {
        Task<QuerySnapshot> qs = db.collection("building").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            //query database for the collection holding all the building documents(the collection holds each document, and each document holds the info of a particular building)
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) { //when the query completes ...
                QuerySnapshot q = task.getResult(); //store the query snapshot
                getDistanceToBuildings(q.getDocuments(), probabilities, uLoc); //pass to the next function all the building documents in a list, the probabilities from the ML model, and the user location
            }
        });
    }

    /**
     * Calculates User's distance to all buildings stored in FireStore database. Stores these distances into a double array.  These distances are then passed into getNearestBuilding, the fifth
     * classification chain method.
     *
     * @param documents     A list of document snapshots, each document refers to a particular building in the FireStore database
     * @param probabilities These are the probabilities of classes returned from the CNN machine learning model from getTFModel
     * @param uLoc          User location captured from getLocation Method.
     */
    private void getDistanceToBuildings(List<DocumentSnapshot> documents, float[] probabilities, Location uLoc) {
        double[] distances = new double[documents.size()]; //create an array which will store the distance between the user and each building
        for (int index = 0; index < documents.size(); index++) { //for each building ...
            GeoPoint bPoint = documents.get(index).getGeoPoint("location"); //get the building's location from the database, stored in a GeoPoint

            Location bLoc = new Location(""); //convert the building's GeoPoint into a Location (better variable to use for finding distances between two locations)
            bLoc.setLatitude(bPoint.getLatitude()); //pass building's latitude
            bLoc.setLongitude(bPoint.getLongitude()); //pass building's longitude

            distances[index] = uLoc.distanceTo(bLoc); //store in the distance array, the distance in meters between the user and the particular building
        }
        getNearestBuilding(probabilities, distances, documents); //pass to the next function, the probabilities from the ML model, distances between the user and each building, and list of documents for each building
    }

    /**
     * The last method in the classification chain. Finds the nearest building to User.  Enhances the probability by adding .2 to nearest building's probability. Chooses the max probability. If this probability
     * is below .8 or if nearest building is more then 100 meters away, show background noise dialog, else display dialog of most probable building.
     *
     * @param probabilities These are the probabilities of classes returned from the CNN machine learning model from getTFModel
     * @param distances     List of distances between the user and each building stored in FireStore
     * @param documents     A list of document snapshots, each document refers to a particular building in the FireStore database
     */
    private void getNearestBuilding(float[] probabilities, double[] distances, List<DocumentSnapshot> documents) {
        //will attempt to find closest building ...
        double minDistance = distances[0]; //closest building in terms of distance
        int minIndex = 0; //closest building in terms of index
        for (int index = 1; index < distances.length; index++) {
            if (distances[index] < minDistance) {
                minDistance = distances[index];
                minIndex = index;
            }
        } //for loop will iterate over each building's distance and update the variables holding the distance to nearest building & index of nearest building

        probabilities[minIndex] += 0.2; //increase the probability(confidence) of nearest building by 0.2
        int maxProbBuildingIndex = predictionProb(probabilities); //get the index of the building with the highest probability

        if (minDistance > 100.0 || (probabilities[maxProbBuildingIndex] < 0.8)) {
            //if the closest building is farther than 100 meters OR if the probability of the most probable building is less than 80% ...
            backgroundDialogue(this); //bring up "background noise" dialog
            return; //return from function
        } else {
            getInfo(documents.get(maxProbBuildingIndex)); //else, bring up dialog of most probable building
        }
    }

    /**
     * Returns index of building with highest probability
     *
     * @param probabilities These are the probabilities of classes returned from the CNN machine learning model from getTFModel
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
     * Given document of building, gets all the building's info (name,dept,address,etc.) and pass info to a
     * function which will create dialog
     *
     * @param ds Document snapshot referring to a particular building from FireStore database
     */
    public void getInfo(DocumentSnapshot ds) {
        Object bName = ds.get("name");
        Object bDepartment = ds.get("department");
        Object bAddress = ds.get("address");
        String resultForDisplay = bName.toString() + "\n" + bDepartment.toString() + "\n" + bAddress.toString();

        String Name = bName.toString();
        String Department = bDepartment.toString();
        String Address = bAddress.toString();

        showDialog(MainActivity.this, Name, Department, Address);
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
        mTextureView.setClickable(false); //cannot click on TextureView (limited to clicking in dialog area)

        final Dialog dialog = new Dialog(activity); //create a new dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //no title
        dialog.setContentView(R.layout.layout_dialog); //set the layout of the dialog (grabbing from xml)

        //setting the TextView up
        TextView name = dialog.findViewById(R.id.name_holder);  //grabbing name element from xml
        TextView dep = dialog.findViewById(R.id.department_holder); //grabbing department element from xml
        TextView address = dialog.findViewById(R.id.address_holder); //grabbing address element from xml
        Button exit = dialog.findViewById(R.id.exit_button); //grabbing exit element from xml

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
        activitySwitchGuard = true; //now that we are done processing (reached the end since taking photo and passing photo to ML model), we can allow user to go to gallery now by switching activitySwitchGuard

        //setting up the button to exit the dialog
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //upon exiting the dialog ...
                dialog.dismiss(); //dismiss the dialog
                mCamera.startPreview(); //start the preview again (let us see)
                mTextureView.setClickable(true); //allow us to click on the preview again (take more photos)
            }
        });
    }

    /**
     * Creating dialog alerting user of background noise. This will be called upon unsuccessful classification of image.
     *
     * @param activity Which class we want the dialog to show in
     */
    public void backgroundDialogue(Activity activity) {
        mTextureView.setClickable(false); //cannot click on TextureView (limited to clicking in dialog area)

        final Dialog dialog2 = new Dialog(activity); //create a new dialog
        dialog2.requestWindowFeature(Window.FEATURE_NO_TITLE); //no title
        dialog2.setContentView(R.layout.background_dialogue); //set the layout of the dialog (grabbing from xml)

        Button exit2 = dialog2.findViewById(R.id.exit_button2); //grabbing exit element from xml

        Window window = dialog2.getWindow(); //get window of dialog
        WindowManager.LayoutParams wlp = window.getAttributes(); //grab current attributes of window

        //overwrite some attributes ...
        wlp.gravity = Gravity.BOTTOM; //stick dialog on the bottom
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND; //remove darkened screen
        wlp.windowAnimations = R.style.DialogAnimation_2; //add animation to the dialog window (slide up upon arrival & slide down upon exit)
        wlp.width = WindowManager.LayoutParams.MATCH_PARENT; //setup width of the dialog to match parent
        window.setAttributes(wlp); //write back all the attributes to the dialog window

        dialog2.show(); //showing the dialog
        activitySwitchGuard = true; //now that we are done processing (reached the end since taking photo and passing photo to ML model), we can allow user to go to gallery now by switching activitySwitchGuard

        //setting up the button to exit the dialog
        exit2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //upon exiting the dialog ...
                dialog2.dismiss(); //dismiss the dialog
                mCamera.startPreview(); //start the preview again (let us see)
                mTextureView.setClickable(true); //allow us to click on the preview again (take more photos)
            }
        });
    }

    /**
     * Creates a intent that will take user to gallery page.
     */
    private void goToGallery() {
        Intent galleryIntent; //creating a new intent to go to gallery
        galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, ACTIVITY_SELECT_PICTURE);
    }

    /**
     * Creates the gallery icon in the action bar.
     *
     * @param menu The menu of the current activity
     * @return a boolean to indicate success
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater;
        inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Sets up menu to call goToGallery on click.
     *
     * @param item Menu item selected
     * @return a boolean to indicate success
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home_id:
                if (activitySwitchGuard == true) {
                    goToGallery();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Called when returning from different activity.
     *
     * @param requestCode The activity code we are returning from
     * @param resultCode  The indication if we returned successfully from activity
     * @param data        The data passed from previous activity
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri selectedURI;
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ACTIVITY_SELECT_PICTURE:
                if (resultCode == Activity.RESULT_OK) {
                    selectedURI = data.getData();
                    Intent resultIntent = new Intent();
                    resultIntent.setClassName("com.example.buildingpoint",
                            "com.example.buildingpoint.ResultPage");
                    resultIntent.putExtra("theURI", selectedURI.toString());
                    startActivityForResult(resultIntent, ACTIVITY_RESULT);
                } else {
                    canTakePhoto = true;
                }
                break;
            default:
                break;
        }
    }

    /**
     * Perform any final cleanup before an activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

