package com.chinjanggg.dontforgettoturnoffthelight;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    //---------Variables---------//
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationManager mLocationManager;
    private LocationRequest mLocationRequest;
    private long UPDATE_INTERVAL = 2 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */
    private double currentLat;
    private double currentLong;
    private double homeLat;
    private double homeLong;
    private boolean isLightOn = true;
    private double latDif;
    private double longDif;
    final private double soFarAway = 0.0000100;
    private View view;
    private boolean isSetLocation = false;
    private static final String PATH_TO_SERVER = "http://192.168.43.167/project/newfile.txt";
    private static final String PATH_TO_ON = "http://192.168.43.167/project/sample.php?value=ON";
    private static final String PATH_TO_OFF = "http://192.168.43.167/project/sample.php?value=OFF";
    DownloadFilesTask downloadFilesTask = new DownloadFilesTask();
    CheckDistance checkDistance = new CheckDistance();


    //---------Main---------//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_check);

        view = this.getWindow().getDecorView();
        view.setBackgroundResource(R.color.yellow);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        checkLocation();

        //Declare the timer
        Timer myTimer = new Timer();
        //Set the schedule function and rate
        myTimer.scheduleAtFixedRate(new TimerTask() {
                                        @Override
                                        public void run() {
                                            //Called at every 1000 milliseconds (1 second)
                                            downloadFilesTask = new DownloadFilesTask();
                                            checkDistance = new CheckDistance();
                                            checkDistance.execute();
                                            downloadFilesTask.execute();
                                            Log.i("MainActivity", "Repeated task");
                                        }
                                    },
                //set the amount of time in milliseconds before first execution
                0,
                //Set the amount of time between each execution (in milliseconds)
                300);



    }

    private class DownloadFilesTask extends AsyncTask<URL, Void, String> {
        protected String doInBackground(URL... urls) {
            return downloadRemoteTextFileContent();
        }
        protected void onPostExecute(String result) {
            if(!TextUtils.isEmpty(result)){
                System.out.println(result);
                ((TextView)findViewById(R.id.status_textview)).setText("STATUS: "+result);
                if (result.trim().equals("ON")){
                    isLightOn = true;
                    view.setBackgroundColor(getResources().getColor(R.color.yellow));
                    findViewById(R.id.btSwitch).setBackgroundResource(R.drawable.idea);
                }
                else if (result.trim().equals("OFF")){
                    isLightOn = false;
                    view.setBackgroundColor(getResources().getColor(R.color.gray));
                    findViewById(R.id.btSwitch).setBackgroundResource(R.drawable.idea2);
                }
//
//
//                    view.setBackgroundColor(getResources().getColor(R.color.gray));
//                    findViewById(R.id.btSwitch).setBackgroundResource(R.drawable.idea2);

            }
        }
    }




    //---------Location---------//
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        startLocationUpdates();

        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(mLocation == null){
            startLocationUpdates();
        }
        if (mLocation != null) {

            // mLatitudeTextView.setText(String.valueOf(mLocation.getLatitude()));
            //mLongitudeTextView.setText(String.valueOf(mLocation.getLongitude()));
        } else {
            Toast.makeText(this, "Location not Detected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
        Log.d("reque", "--->>>>");
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLat = location.getLatitude();
        currentLong = location.getLongitude();
        ((TextView)findViewById(R.id.latitude_textview)).setText("CURRENT Latitude: " + currentLat);
        ((TextView)findViewById(R.id.longitude_textview)).setText("CURRENT Longitude: " + currentLong);
        // You can now create a LatLng Object for use with maps
        //LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

    }

    private boolean checkLocation() {
        if(!isLocationEnabled())
            showAlert();
        return isLocationEnabled();
    }

    private String downloadRemoteTextFileContent(){
        URL mUrl = null;
        String content = "";
        try {
            mUrl = new URL(PATH_TO_SERVER);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            assert mUrl != null;
            URLConnection connection = mUrl.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = "";
            while((line = br.readLine()) != null){
                content += line;
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content;
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                    }
                });
        dialog.show();
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    //---------Set Home---------//
    public void setHome(View view) {
        homeLat = currentLat;
        homeLong = currentLong;
        isSetLocation=true;
        ((TextView)findViewById(R.id.homeLat_textview)).setText("Home Latitude: " + Double.toString(homeLat));
        ((TextView)findViewById(R.id.homeLong_textview)).setText("Home Longitude: " + Double.toString(homeLong));
        String msg = "Updated Home Location: " +
                Double.toString(homeLat) + "," +
                Double.toString(homeLong);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    //---------Distance and Alert---------//

    //We will use only sendNotif. function
    //We don't need to use check distance function, we will use checkInHouse value from nodeMCU, I just write to test alert
    //I didn't create a variable for checkInHouse yet, do anything you want


    private class CheckDistance extends AsyncTask<URL, Void, String> {
        protected String doInBackground(URL... urls) {
            if (isSetLocation) {
                latDif = homeLat - currentLat;
                longDif = homeLong - currentLong;
                if ((latDif > soFarAway || latDif < -soFarAway || longDif > soFarAway || longDif < -soFarAway) && isLightOn) {
                    turnOffTheLight();
                }
            }
            return "";
        }
        protected void onPostExecute(String result) {
            if(!TextUtils.isEmpty(result)){
            }
        }
    }


    //---------End Location---------//





    //---------Light---------//
    public void switchLight(View v) {
        isLightOn = !isLightOn;
        if(isLightOn) {
            turnOnTheLight();
        } else {
            turnOffTheLight();
        }
    }

    public void turnOffTheLight()  {
        view.setBackgroundColor(getResources().getColor(R.color.gray));
        findViewById(R.id.btSwitch).setBackgroundResource(R.drawable.idea2);
        TurnOff turnOff = new TurnOff();
        turnOff.execute();

    }
    private class TurnOff extends AsyncTask<URL, Void, String> {
        protected String doInBackground(URL... urls) {
            URL mUrl = null;
            try {
                mUrl = new URL(PATH_TO_OFF);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                assert mUrl != null;
                URLConnection connection = mUrl.openConnection();
                connection.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }
        protected void onPostExecute(String result) {
            if(!TextUtils.isEmpty(result)){
            }
        }
    }


    public void turnOnTheLight() {
        view.setBackgroundColor(getResources().getColor(R.color.yellow));
        findViewById(R.id.btSwitch).setBackgroundResource(R.drawable.idea);
        TurnOn turnOn = new TurnOn();
        turnOn.execute();
    }

    private class TurnOn extends AsyncTask<URL, Void, String> {
        protected String doInBackground(URL... urls) {
            URL mUrl = null;
            try {
                mUrl = new URL(PATH_TO_ON);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                assert mUrl != null;
                URLConnection connection = mUrl.openConnection();
                connection.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }
        protected void onPostExecute(String result) {
            if(!TextUtils.isEmpty(result)){
            }
        }
    }

}
