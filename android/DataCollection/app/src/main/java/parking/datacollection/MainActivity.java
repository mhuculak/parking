package parking.datacollection;

import parking.map.Position;

import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Context;
import android.location.Criteria;
import android.os.Environment;
import android.content.Intent;
import android.provider.MediaStore;
import android.media.MediaScannerConnection;

import android.view.View;
import android.widget.TextView;

import android.location.LocationManager;
import android.location.Location;
import android.location.LocationListener;
import com.google.android.gms.maps.model.LatLng;

import android.util.Log;
import android.net.Uri;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private int cameraPermission;
    private int locationPermission;
    private int storagePermission;
    private LocationManager locationManager;
    private String provider;
    private double bestAccuracy;
    private Position currentPosition;
    private List<Position> picturePoints;
    private int picCount;
    private Uri signPictureUri;
    private boolean pictureRequested;

    private TextView errorMessage;

    protected static final String TAG = "parking";
    protected static final String PU_TAG = "parking";

    private final String appDir = "dc";
    private final String imageDir = "images";
    private final int TAKE_PHOTO_CODE = 53;
    private final int PERMISSIONS_REQUEST_CAMERA = 49;
    private final int PERMISSIONS_REQUEST_LOCATION = 48;
    private final int PERMISSIONS_REQUEST_STORAGE = 45;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meters
    private static final long MIN_TIME_BW_UPDATES = 1000; // 1 second

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        errorMessage = (TextView)findViewById(R.id.error_message);
        picturePoints = null;
        currentPosition = null;
        picCount = -1;
        if (!checkPermission()) {
            requestPermission();
        }
    }

    private boolean checkPermission() {
        cameraPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        locationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        storagePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return cameraPermission == PackageManager.PERMISSION_GRANTED && locationPermission == PackageManager.PERMISSION_GRANTED
                && storagePermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "request permission for camera "+PERMISSIONS_REQUEST_CAMERA);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
        }
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "request permission for location "+PERMISSIONS_REQUEST_LOCATION);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
        }
        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "request permission for storage "+PERMISSIONS_REQUEST_STORAGE);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_STORAGE);
        }
    }

    private boolean setupPicture() {
        if (checkPermission()) {
            setupLocation();
            if (picCount < 0) {
                picCount = SignPictures.readPictureCount(getFilesDir());
                Log.i(TAG, "picture count is " + picCount);
            }
            return true;
        }
        else {
            return false;
        }
    }

    public void doPicture(View view) {
        pictureRequested = true;
        if (setupPicture()) {
            takePicture();
        }
        else {
            requestPermission();
        }
    }

    public void takePicture() {
        if (!pictureRequested) {
            Log.i(TAG, "no picture requested ");
            return;
        }
        pictureRequested = false;
        Log.i(TAG, "take picture ");
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

        File rootDir = Environment.getExternalStorageDirectory();
        filesDir = new File(rootDir+File.separator+appDir+File.separator+imageDir);
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
            Log.e(TAG, "Caught exception ", ex);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "got activity result request = "+requestCode+" result = "+resultCode);
        if ( resultCode == RESULT_OK) {
            if (requestCode == TAKE_PHOTO_CODE) {
                Log.i(TAG, "Pic is available");
                savePicture();
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

    private void  savePicture() {
        Log.i(TAG, "save picture count "+picCount);
        MediaScannerConnection.scanFile(this, new String[] {signPictureUri.getPath()}, null, null);
        SignPictures.saveLocationData(this, Environment.getExternalStorageDirectory(), appDir, picCount, picturePoints);
        return;
    }

    private void setError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisibility(View.VISIBLE);
    }

    private void setupLocation() {
        if (currentPosition != null) {
            return; // already done
        }
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
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(PU_TAG, "location changed");
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        Log.i(PU_TAG,"Location is "+lat+" "+lng+" accuracy is "+location.getAccuracy());
        double accuracy = Double.parseDouble(Float.toString(location.getAccuracy())); // radius in meters where actual position is 68% of the time
        if (bestAccuracy < 0 || bestAccuracy > accuracy) {
            bestAccuracy = accuracy;
        }
        LatLng latLng = new LatLng(lat, lng);

        currentPosition = new Position( lat, lng, accuracy);
        if (picturePoints != null) {
            picturePoints.add(currentPosition);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "response for permission request "+requestCode);
        for ( int i=0 ; i<grantResults.length ; i++) {
            Log.i(TAG, "result["+i+"] = "+grantResults[i]);
        }
        for ( int i=0 ; i<permissions.length ; i++) {
            Log.i(TAG, "permissions["+i+"] = "+permissions[i]);
        }
        if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
            Log.i(TAG, "Received response for Camera permission request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG,"camera permission is granted");
                if (setupPicture()) {
                    takePicture();
                }
            }
            else {
                Log.e(TAG, "Permission not granted for camera result");
            }
        }
        else if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            Log.i(TAG, "Received response for Location permission request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
                if (locationPermission == PackageManager.PERMISSION_GRANTED) {
                    if (setupPicture()) {
                        takePicture();
                    }
                }
            }
            else {
                Log.e(TAG, "Permission not granted for location result");
            }
        }
        else if (requestCode == PERMISSIONS_REQUEST_STORAGE) {
            Log.i(TAG, "Received response for storage permission request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                storagePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if ( storagePermission == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG,"storage permission is granted");
                    if (setupPicture()) {
                        takePicture();
                    }
                }
                else {
                    Log.e(TAG,"Failed to get storage access");
                }
            }
            else {
                Log.e(TAG, "Permission not granted for storage result");
            }
        }
        else {
            Log.i(TAG, "Received request for permission "+requestCode);
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
}
