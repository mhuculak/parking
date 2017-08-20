package parking.parkingapp;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import parking.map.Address;
import parking.map.Position;
import parking.map.Place;
import parking.map.Trajectory;
import parking.map.CommonLibInit;
import parking.util.Utils;
import parking.schedule.ParkingSchedule;
import parking.schedule.SignSchedule;
import parking.security.User;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;

import android.media.MediaScannerConnection;
import android.os.Environment;
import android.location.LocationListener;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.FrameLayout;
import android.view.View;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
// import android.support.v4.app.Fragment;
import android.app.Fragment;

import android.location.LocationManager;
import android.location.Location;
import android.util.Log;
import android.content.Context;
import android.location.Criteria;
import android.graphics.Color;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.Manifest;
import android.content.pm.PackageManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import java.io.File;

enum UImethod { getFrom, getUntil, getDestination, getCameraPermission, actionButton0, actionButton1, actionButton2}

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback, LocationListener, Register.CustomerRegistered {

    private GoogleMap mMap;
    private Marker phoneMarker;
    private ParkingAppMode appMode;
    private EditText destinationText;
    private LinearLayout destLayout;
    private Button pictureButton;
    private TextView errorMessage;
    private Map<UImethod, String> errorMap;
    private LinearLayout startLayout;
    private ParkingPeriod parkingPeriod;
    private LinearLayout endLayout;
    private LinearLayout webLayout;
    private RelativeLayout mapLayout;
    private FrameLayout registerLayout;
    private Place destinationPlace;
    private Place signPlace;
    private Place currentPlace;
    private int destClickCount = 0;
    private Position currentPosition;
    private double cameraStartTime;
    private double cameraEndTime;
    private double bestAccuracy;
    private ParkingSchedule displaySchedule;
    private SignSchedule verifiedSchedule;
    private Bitmap signImage;
    private LocationManager locationManager;
    private String provider;
    private int picCount;
    private int cameraPermission;
    private int locationPermission;
    private int networkPermission;
    private int internetPermission;
    private int storagePermission;
    private int phonePermission;
    private Uri signPictureUri;
    private UserCredentials credentials;
    private String phoneNumber;
    private Register register;
    private Trajectory trajectory;
    private List<Position> picturePoints;
    private final int defaultZoomLevel = 17;
    private final int defaultHoursToPark = 3;
    private final double parkingDistanceThresholdKm = 0.1;
    private final double maxDistanceKm = 100; // max distance dest can be
    private final int TAKE_PHOTO_CODE = 53;
    private final int PERMISSIONS_REQUEST_CAMERA = 49;
    private final int PERMISSIONS_REQUEST_LOCATION = 48;
    private final int PERMISSIONS_REQUEST_INTERNET = 47;
    private final int PERMISSIONS_REQUEST_NETWORK = 46;
    private final int PERMISSIONS_REQUEST_STORAGE = 45;
    private final int PERMISSIONS_PHONE = 44;
    private final int maxTrajectorySize = 20;
    private final boolean useInternet = false;  // for data collection without an unlimited data plan

    private final String signUploadUrl = "http://parking.allowed.org:8082/parking/upload";
    private final String signUpdateUrl = "http://parking.allowed.org:8082/parking/upload/verify";
    private final String trajectoryUploadUrl = "http://parking.allowed.org:8082/parking/trajectory";
    private final String registrationUrl = "http://parking.allowed.org:8082/parking/register";
    private final String testUrl = "http://parking.allowed.org:8082/parking/test";
/*
    private final String signUploadUrl = "http://parking.allowed.org:8081/parking/upload";
    private final String signUpdateUrl = "http://parking.allowed.org:8081/parking/upload/verify";
    private final String trajectoryUploadUrl = "http://parking.allowed.org:8081/parking/trajectory";
    private final String registrationUrl = "http://parking.allowed.org:8081/parking/register";
*/
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meters
    private static final long MIN_TIME_BW_UPDATES = 1000; // 1 second

    private CommonLibInit commonInit = new CommonLibInit(this, Locale.getDefault());
    protected static final String TAG = "parking";
    protected static final String PU_TAG = "position-updates";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "enter");

        setContentView(R.layout.activity_maps);
        destinationText = (EditText)findViewById(R.id.destination_text);
        destLayout = (LinearLayout)findViewById(R.id.dest_layout);
        startLayout = (LinearLayout)findViewById(R.id.start_layout);
        endLayout = (LinearLayout)findViewById(R.id.end_layout);
        pictureButton = (Button)findViewById(R.id.picture_button);
        errorMessage = (TextView)findViewById(R.id.error_message);
        mapLayout = (RelativeLayout)findViewById(R.id.map_layout);
        registerLayout = (FrameLayout)findViewById(R.id.register);
        appMode = ParkingAppMode.MOVING;
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        displaySchedule = null;
        errorMap = new HashMap<UImethod, String>();
        trajectory = new Trajectory(maxTrajectorySize);

        webLayout = (LinearLayout)findViewById(R.id.web_layout);
        GifView busy = new GifView(this, "file:///android_asset/ani-busy.gif");
        busy.setLayoutParams(new LinearLayout.LayoutParams(100,100));
        busy.setBackgroundColor(0x00000000);
        webLayout.addView(busy);
        bestAccuracy = -1.0;
        picturePoints = null;
        credentials = UserCredentials.getCredentials(this);
        requestPermissions();
        if (!useInternet) {
            String extStorageState = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
                Log.i(TAG, "external storage is available");
            }
            else {
                String message = "Cannot save data to external storage";
                Log.e(TAG, message);
                setError(message);
            }
        }
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                try {
                    Log.i(TAG, "get schedule...");
                    verifiedSchedule = (SignSchedule) extras.getSerializable("verified");
                } catch (Exception ex) {
                    Log.e(TAG, "exception", ex);
                }
                if (verifiedSchedule != null && useInternet) {
                    uploadSchedule(verifiedSchedule);
                }
            }
        }
        Log.i(TAG, "done create");
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.i(TAG, "map ready enter");
        mMap = map;
        if (locationPermission == PackageManager.PERMISSION_GRANTED) {
            setupLocation();
        }
        if (phonePermission == PackageManager.PERMISSION_GRANTED) {
            if (useInternet) {
                TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
                phoneNumber = telephonyManager.getLine1Number();
                Log.i(TAG, "phone number = "+phoneNumber);

                if (credentials == null) {
                    doRegister(credentials);
                } else {

                    credentials.getUser().setPhone(phoneNumber);
                    checkCredentials(credentials);
                }
            }
        }
        if (storagePermission == PackageManager.PERMISSION_GRANTED) {
            picCount = SignPictures.readPictureCount(getFilesDir());
            Log.i(TAG, "picture count is "+picCount);
        }
    }

    private void doRegister(UserCredentials credentials) {
        if (credentials == null) {
            Log.i(TAG, "user must register because there are no credentials");
        }
        else {
            Log.i(TAG, "user "+credentials.getUser().getUserName()+" must register because authorization failed");
        }
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.register,  Register.newInstance(phoneNumber, registrationUrl));
        ft.commit();
        mapLayout.setVisibility(View.INVISIBLE);
    }

    public void customerRegistered(User user) {
        UserCredentials.setCredentials(this, user);
        mapLayout.setVisibility(View.VISIBLE);
        registerLayout.setVisibility(View.INVISIBLE);
    }

    private void requestPermissions() {

        cameraPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        locationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        storagePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (useInternet) {
            networkPermission = checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE);
            internetPermission = checkSelfPermission(Manifest.permission.INTERNET);
            phonePermission = checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
        }
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "request permission for location");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
        }
        if (networkPermission != PackageManager.PERMISSION_GRANTED && useInternet) {
            Log.i(TAG, "request permission for network");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, PERMISSIONS_REQUEST_NETWORK);
        }
        if (internetPermission != PackageManager.PERMISSION_GRANTED && useInternet) {
            Log.i(TAG, "request permission for internet");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, PERMISSIONS_REQUEST_INTERNET);
        }
        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "request permission for storage");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_STORAGE);
        }
        if (phonePermission != PackageManager.PERMISSION_GRANTED && useInternet) {
            Log.i(TAG, "request permission for phone number");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSIONS_PHONE);
        }
    }

    private void setupLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.i(TAG, "request location updates");
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
        Criteria criteria = new Criteria();
        Log.i(TAG, "get best provider");
        provider = locationManager.getBestProvider(criteria, false);
        Log.i(TAG, "getLastKnownLocation using " + provider);
        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            Log.i(TAG, "Provider " + provider + " has been selected.");
            onLocationChanged(location);
        }
        else {
            Log.i(TAG, "unable to get location");
            if (verifiedSchedule != null && verifiedSchedule.place != null) {
                currentPosition = Position.getLocation(verifiedSchedule.place, null, 0.0);
                currentPlace = new Place(null, currentPosition);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentPosition.getLatitude(), currentPosition.getLongitude()), defaultZoomLevel));
            }
        }
    }
    //
    // Used to clear error messages after corrective action has been taken
    //
    private void handleErrorMessage(UImethod method) {
        String msgToRemove = errorMap.get(method);
        if (msgToRemove != null){
            for (Iterator<UImethod> it = errorMap.keySet().iterator() ; it.hasNext() ; ) { // may have multiple instances
                UImethod m = it.next();
                String msg = errorMap.get(m);
                if (msg.equals(msgToRemove)) {
                    Log.i(TAG, "Removed error message "+msg+" from "+method);
                    it.remove();
                }
            }
            errorMessage.setVisibility(View.INVISIBLE);
        }
    }

    public void getFrom(View view) {
        handleErrorMessage(UImethod.getFrom);
        Log.i(TAG, "get start time");
        parkingPeriod.getFrom().show();
        startLayout.setVisibility(View.INVISIBLE);
    }

    public void getUntil(View view) {
        handleErrorMessage(UImethod.getUntil);
        Log.i(TAG, "get end time");
        parkingPeriod.getUntil().show();
        endLayout.setVisibility(View.INVISIBLE);
    }

    private void setDestination(Place dest) {
        destinationPlace = dest;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(destinationPlace.getLatitude(), destinationPlace.getLongitude()), defaultZoomLevel));
        //
        // TO DO: start HTTP fetch of parking schedule
        //
        displaySchedule = null;
    }

    public void getDestination(View view) {
        handleErrorMessage(UImethod.getDestination);
        destClickCount++;
        String dest = destinationText.getText().toString();
        if (dest != null && Utils.isNotBlank(dest)) {
            Log.i(TAG, "Find destination "+dest);
            Place destination = findDestination(dest);
            if (destination == null) {
                destinationText.setTextColor(Color.RED);
            }
            else {
                setDestination(destination);
                destLayout.setVisibility(View.INVISIBLE);
            }
        }
        else if (destClickCount > 1) { // if user clicks twice with empty destination then use current position
            if (currentPosition != null) {
                Log.i(TAG, "set current position " + currentPosition + " as desiination");
                setDestination(new Place(null, currentPosition));
                Log.i(TAG, "destination is current postion " + destinationPlace.shortAddress());
                if (changeStateToParking()) {
                    Log.i(TAG, "switched to parking mode");
                }
                else {
                    Log.i(TAG, "still in prep mode");
                }
                destLayout.setVisibility(View.INVISIBLE);
            }
            Log.i(TAG, "no destination selected, no current position found count = "+destClickCount);
        }
        else {
            Log.i(TAG, "no destination selected count = "+destClickCount);
        }
    }

    public void getCameraPermission(View view) {
        try {
            handleErrorMessage(UImethod.getCameraPermission);
        }
        catch (Exception ex) {
            Log.e(TAG, "excpetion", ex);
        }
        Log.i(TAG, "get camera permission ");
        pictureButton.setVisibility(View.INVISIBLE);
        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            //
            //  will invoke onRequestPermissionsResult when user grants permission
            //
            try {
                Log.i(TAG, "requsting permission... ");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            }
            catch (Exception ex)
            {
                Log.e(TAG, "Caught exception "+ex+" while requesting permission for camera");
            }
        }
        else {
            Log.i(TAG, "permission already granted...");
            takePicture();
        }
    }

    public void uploadSchedule(SignSchedule schedule) {
        try {
            UploadClient uploadClient = new UploadClient(schedule, this, credentials.getUser());
            uploadClient.execute(signUpdateUrl);
            webLayout.setVisibility(View.VISIBLE);
        }
        catch(Exception ex) {
            Log.e(TAG, "exception", ex);
        }
    }

    public void uploadPicture() {
        if (!useInternet) {
            Log.i(TAG, "save picture count "+picCount);
            MediaScannerConnection.scanFile(this, new String[] {signPictureUri.getPath()}, null, null);
            SignPictures.saveLocationData(this, Environment.getExternalStorageDirectory(), picCount, picturePoints);
            return;
        }
        Log.i(TAG, "upload file "+signPictureUri.getPath());
        File imgFile = new File(signPictureUri.getPath());
        Log.i(TAG, "image size is " + imgFile.length() + " bytes");

        cameraEndTime = trajectory.getElapsedTime();
        if (currentPlace != null) {
            signPlace = currentPlace;
            trajectory.add(currentPlace.getPosition());
            Log.i(TAG, "sign place is " + signPlace.shortAddress());
        }
        else {
            Log.i(TAG, "sign position not available ");
        }

        try {
            UploadClient imageUpload = new UploadClient(imgFile, this, credentials.getUser());
            imageUpload.execute(signUploadUrl);
            webLayout.setVisibility(View.VISIBLE);
        }
        catch(Exception ex) {
            Log.e(TAG, "Caught exception "+ex+" attempting to upload.");
        }
    }

    public void uploadResultAvailable(String resultData) {
        webLayout.setVisibility(View.INVISIBLE);
        if (resultData == null || resultData.length() == 0) {
            Log.i(TAG, "No upload result to verify...");
            return;
        }
        Log.i(TAG, "Process picture upload result "+resultData);
        SignSchedule autoSchedule = null;
        try {
            JSONObject jObj = new JSONObject(resultData);
            autoSchedule = new SignSchedule(jObj);

            if (trajectory.size() > 0) {
                UploadClient trajectoryUpload = new UploadClient(trajectory, autoSchedule.id, cameraStartTime, cameraEndTime, this, credentials.getUser());
                trajectoryUpload.execute(trajectoryUploadUrl);
                trajectory.allowLimit();
            }

//            Log.i(TAG, "got schedule " + schedule.displayText());

            if (autoSchedule != null) {

                Intent intent = new Intent(this, SignVerifier.class);
                intent.putExtra("image", signPictureUri);
                intent.putExtra("schedule", autoSchedule);
                intent.putExtra("bestAccuracy", bestAccuracy);
               if (signPlace != null) {
                    Log.i(TAG, "pass sign place " + signPlace.shortAddress()+" to verifier");
                    intent.putExtra("place", signPlace);
                }
                startActivity(intent);
            }
        }
        catch (Exception ex) {
            Log.e(TAG, "Exception "+ex+" while parsing schedule");
            Log.e(TAG, "exception", ex);
        }

    }

    public void uploadConnectionFailure(String message, int httpResponseCode) {
        setError(message);
        if (httpResponseCode == 401) {
            doRegister(credentials);
        }
    }


    public void takePicture() {
        Log.i(TAG, "take picture ");
        cameraStartTime = trajectory.getElapsedTime();
        trajectory.blockLimit();
        bestAccuracy = -1.0;
        int perm = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (perm != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "can't take picture unless permission is granted...");
            setError("Permission not granted to use camera");
            return;
        }
        picCount++;
        picturePoints = new ArrayList<Position>();
        picturePoints.add(currentPosition);
        SignPictures.writePictureCount(getFilesDir(), picCount);
        File filesDir = null;
        if (useInternet) {
            filesDir = this.getApplicationContext().getExternalCacheDir();
        }
        else {
            final String locationtDir = "parking/images";
            File rootDir = Environment.getExternalStorageDirectory();
            filesDir = new File(rootDir+File.separator+locationtDir);
            if (!filesDir.exists()) {
                if (filesDir.mkdirs()) {
                    Log.i(TAG, filesDir.getAbsolutePath() + " was created");
                }
                else {
                    Log.e(TAG, "failed to create "+filesDir.getAbsolutePath());
                    return;
                }
            }
            Log.i(TAG,"Scan file "+filesDir.toString());

        }
        String signPicFilename = "sign-pic"+picCount+".jpg";
        File newfile = new File(filesDir+File.separator+signPicFilename);

        try {
            signPictureUri = Uri.fromFile(newfile);
            Log.i(TAG, "created uri " + signPictureUri.getPath());
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, signPictureUri);
            Log.i(TAG, "start camera");
            //
            //  will invoke onActivityResult after user takes picture
            //
            startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);
            Log.i(TAG, "wait for result...");
        }
        catch (SecurityException se)
        {
            Log.e(TAG, "Caught security exception "+se+" while creating "+signPicFilename);
            setError("Permission not granted to use camera");
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Caught exception "+ex+" while creating "+signPicFilename);
        }
    }

    public void actionButton0(View view) {
        handleErrorMessage(UImethod.actionButton0);
        if (appMode == ParkingAppMode.MOVING || appMode == ParkingAppMode.PARKING) {
            Log.i(TAG, "Display Schedule...");
            displaySchedule();
        }
        else {
            Log.i(TAG, "Unsupported app mode " + appMode + " for button 0");
        }
    }

    public void actionButton1(View view) {
        handleErrorMessage(UImethod.actionButton1);
        if (appMode == ParkingAppMode.MOVING) {
            if ( destLayout.getVisibility() == View.INVISIBLE) {
                destLayout.setVisibility(View.VISIBLE);
                destinationText.setTextColor(Color.BLACK);
                if (destinationPlace != null) {
                    destinationText.setText(destinationPlace.shortAddress());
                }
            }
            else {
                destLayout.setVisibility(View.INVISIBLE);
            }
            destClickCount = 0;
        }
        else if (appMode == ParkingAppMode.PARKING) {
            Log.i(TAG, "Verify...");
        }
        else {
            Log.i(TAG, "Unsupported app mode "+appMode+" for button 0");
        }
    }

    public void actionButton2(View view) {
        try {
            handleErrorMessage(UImethod.actionButton2);
            if (appMode == ParkingAppMode.MOVING) {
                if (parkingPeriod == null) {
                    CalendarSelector endSel = new CalendarSelector(this, R.id.end_time, true, true);
                    endSel.addHour(defaultHoursToPark);
                    parkingPeriod = new ParkingPeriod(new CalendarSelector(this, R.id.start_time, true, true), endSel);
                }
                if (startLayout.getVisibility() == View.INVISIBLE) {
                    startLayout.setVisibility(View.VISIBLE);
                } else {
                    startLayout.setVisibility(View.INVISIBLE);
                }
                if (endLayout.getVisibility() == View.INVISIBLE) {
                    endLayout.setVisibility(View.VISIBLE);
                } else {
                    endLayout.setVisibility(View.INVISIBLE);
                }
                Log.i(TAG, "Set period from " + parkingPeriod.getFrom().toString() + " to " + parkingPeriod.getUntil().toString());
            } else if (appMode == ParkingAppMode.PARKING) {
                Log.i(TAG, "Make Correction...");
            } else {
                Log.i(TAG, "Unsupported app mode " + appMode + " for button 0");
            }
        }
        catch (Exception ex) {
            Log.e(TAG, "exception", ex);
        }
    }

    public Place findDestination(String dest) {
        Log.i(TAG, "Got raw dest "+dest);
        Place p = Place.findPlace(dest, currentPosition, maxDistanceKm);
        if (p != null) {
            p.setStreetNumber(dest);
            Log.i(TAG, "destination is " + p.shortAddress());
            return p;
        }
        else if (currentPlace.getAddress() != null) {
            Address currentAddress = currentPlace.getAddress();
            Log.i(TAG, "Could not find "+dest);
            String retry = null;
            if (currentAddress.getCity() != null) {
                retry = dest + " " + currentAddress.getCity();
            }
            else if (currentAddress.getProvinceState() != null) {
                retry = dest + " " +currentAddress.getProvinceState();
            }
            else if (currentAddress.getCountry() != null) {
                retry = dest + " " +currentAddress.getCountry();
            }
            Log.i(TAG, "Retry with "+retry);
            p = Place.findPlace(retry, currentPosition, maxDistanceKm);
            if (p != null) {
                p.setStreetNumber(dest);
                Log.i(TAG, "destination is " + p.shortAddress());
                return p;
            }
            else {
                Log.i(TAG, "still no luck...");
            }
        }
        else {
            Log.i(TAG, "No current address to use for retry...");
        }
        return null;
    }

    public void displaySchedule() {
        if (displaySchedule == null) {
            pictureButton.setVisibility(View.VISIBLE);
            String err = getResources().getString(R.string.schedule_unavailable);
            setError(err);
            errorMap.put(UImethod.actionButton1, err);
            errorMap.put(UImethod.getCameraPermission, err);
        }
    }

    private void setError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisibility(View.VISIBLE);
    }

    private boolean changeStateToParking() {

        if (destinationPlace != null && parkingPeriod != null) {
            appMode = ParkingAppMode.PARKING;
            final Button actionButton1 = (Button) findViewById(R.id.actionButton1);
            actionButton1.setText(getResources().getString(R.string.verify));
            final Button actionButton2 = (Button) findViewById(R.id.actionButton2);
            actionButton2.setText(getResources().getString(R.string.report_problem));
            return true;
        }
        else {
            String error_message = null;
            if (destinationPlace == null) {
                error_message = "No destination defined";
                errorMap.put(UImethod.actionButton1, error_message);
            }
            else {
                error_message = "No period defined.";
                errorMap.put(UImethod.actionButton2, error_message);
            }
            setError(error_message);
            return false;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(PU_TAG, "location changed");
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        Log.i(PU_TAG,"Location accuracy is "+location.getAccuracy());
        double accuracy = Double.parseDouble(Float.toString(location.getAccuracy())); // radius in meters where actual position is 68% of the time
        if (bestAccuracy < 0 || bestAccuracy > accuracy) {
            bestAccuracy = accuracy;
        }
        LatLng latLng = new LatLng(lat, lng);
        if (phoneMarker == null) {
            Log.i(PU_TAG, "create marker options");
            MarkerOptions options = new MarkerOptions();
            options.position(latLng);
            phoneMarker = mMap.addMarker(options);
            Log.i(TAG, "new marker added");
        }
        else {
            Log.i(PU_TAG, "update marker position");
            phoneMarker.setPosition(latLng);
        }
        currentPosition = new Position( lat, lng, accuracy);
        trajectory.add(currentPosition);
        if (picturePoints != null) {
            picturePoints.add(currentPosition);
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), defaultZoomLevel));
        Address currentAddress = Address.reverseGeocode( currentPosition);
        currentPlace = new Place(currentAddress, currentPosition);
        Log.i(PU_TAG, "current place is " + currentPlace.shortAddress());
        if (currentPosition != null) {
            if (currentAddress != null) {
                Log.i(PU_TAG, "address is " + currentAddress.toString());
            }
            if (destinationPlace != null) {
                double distance = Position.getDistanceKm(destinationPlace.getPosition(), currentPosition);
                if (distance < parkingDistanceThresholdKm) {
                    changeStateToParking();
                }
            }
        }
        else {
            Log.i(PU_TAG, "reverse geocode unavailable");
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.i(TAG,"Enabled new provider " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.i(TAG, "Disabled provider " + provider);
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    public String getDateTime(int addHour) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        String dString = day + "/" + (month + 1) + "/" + year;
        int hour = c.get(Calendar.HOUR_OF_DAY);
        hour += addHour;
        int minute = c.get(Calendar.MINUTE);
        String mString = minute < 10 ? "0"+Integer.toString(minute) : Integer.toString(minute);
        String tString = hour + ":" + mString;
        return dString + " -" + tString;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "got activity result request = "+requestCode+" result = "+resultCode);
        if ( resultCode == RESULT_OK) {
            if (requestCode == TAKE_PHOTO_CODE) {
                Log.i(TAG, "Pic is available");
                uploadPicture();
            }
            else {
                Log.i(TAG, "got success for request "+requestCode);
            }
        }
        else if (resultCode == RESULT_CANCELED) {
            Log.i(TAG, "Result cancelled");
        }
        else if (resultCode == RESULT_FIRST_USER) {
            Log.i(TAG, "Result First User");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
            Log.i(TAG, "Received response for Camera permission request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture();
            }
            else {
                Log.e(TAG, "Permission not granted for camera result = " + grantResults[0]);
            }
        }
        else if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            Log.i(TAG, "Received response for Location permission request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
                if (locationPermission == PackageManager.PERMISSION_GRANTED) {
                    setupLocation();
                }
            }
            else {
                Log.e(TAG, "Permission not granted for location result = " + grantResults[0]);
            }
        }
        else if (requestCode == PERMISSIONS_REQUEST_INTERNET) {
            Log.i(TAG, "Received response for internet permission request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                internetPermission = checkSelfPermission(Manifest.permission.INTERNET);
                if (internetPermission == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG,"Internet permission is granted");
                }
                else {
                    Log.e(TAG,"Failed to get internet access");
                }
            }
            else {
                Log.e(TAG, "Permission not granted for internet result = " + grantResults[0]);
            }
        }
        else if (requestCode == PERMISSIONS_REQUEST_NETWORK) {
            Log.i(TAG, "Received response for network permission request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                networkPermission = checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE);
                if ( networkPermission == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG,"networkt permission is granted");
                }
                else {
                    Log.e(TAG,"Failed to get network access");
                }
            }
            else {
                Log.e(TAG, "Permission not granted for network result = " + grantResults[0]);
            }
        }
        else if (requestCode == PERMISSIONS_REQUEST_STORAGE) {
            Log.i(TAG, "Received response for storage permission request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                storagePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if ( storagePermission == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG,"storage permission is granted");
                }
                else {
                    Log.e(TAG,"Failed to get storage access");
                }
            }
            else {
                Log.e(TAG, "Permission not granted for storage result = " + grantResults[0]);
            }
        }
        else if (requestCode == PERMISSIONS_PHONE) {
            Log.i(TAG, "Received response for phone permission request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                storagePermission = checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
                if ( storagePermission == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG,"phone permission is granted");
                }
                else {
                    Log.e(TAG,"Failed to get phone access");
                }
            }
            else {
                Log.e(TAG, "Permission not granted for phone result = " + grantResults[0]);
            }
        }
        else {
            Log.i(TAG, "Received request for permission "+requestCode);
        }
    }

    private void checkCredentials(UserCredentials credentials) {
        Log.i(TAG, "testing credentials by fetching "+testUrl);
        DownloadClient downloadClient = new DownloadClient(this, credentials.getUser());
        downloadClient.execute(testUrl);
    }

    public void downloadResultAvailable(String downloadResponse) {
        Log.i(TAG, "Credentials are good, nothing to do...");
    }

    public void downloadConnectionFailure(String errorMessage, int httpResponseCode) {
        if (httpResponseCode == 401) {
            Log.i(TAG, "server returned not authorized...user must register");
            doRegister(credentials);
        }
    }
}
