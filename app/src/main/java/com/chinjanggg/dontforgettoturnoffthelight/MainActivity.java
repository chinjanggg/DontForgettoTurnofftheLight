package com.chinjanggg.dontforgettoturnoffthelight;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

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
    final private double soFarAway = 0.0003000;
    private View view;
    private int seekBarProgressValue=100;


    //---------Main---------//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_check);

        view = this.getWindow().getDecorView();
        view.setBackgroundResource(R.color.yellow);
        ((SeekBar)findViewById(R.id.seekBar)).setProgress(seekBarProgressValue);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

        seekBarSet();
        checkLocation();
        checkDistance();
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

    public void checkDistance() {
        latDif = homeLat-currentLat;
        longDif = homeLong-currentLong;
        if((latDif > soFarAway || latDif < -soFarAway || longDif > soFarAway || longDif < -soFarAway) && isLightOn) {
            sendNotification();
        }
    }

    //Implement anything as you want, you can delete and write a new one with different function name
    public void sendNotification() {
        /*
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.ic_launcher, )
        */

    }


    //---------End Location---------//



    //---------Set SeekBar---------//
    public void seekBarSet() {
        ((SeekBar)findViewById(R.id.seekBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarProgressValue = progress;
                ((TextView)findViewById(R.id.seek_textview)).setText("(Brightness: " + seekBarProgressValue + "%)");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(seekBarProgressValue == 0) {
                    isLightOn = false;
                    turnOffTheLight();
                } else {
                    isLightOn = true;
                    turnOnTheLight();
                }
            }
        });
    }


    //---------Light---------//
    public void switchLight(View v) {
        isLightOn = !isLightOn;
        if(isLightOn) {
            turnOnTheLight();
            ((SeekBar)findViewById(R.id.seekBar)).setProgress(100);
        } else {
            turnOffTheLight();
            ((SeekBar)findViewById(R.id.seekBar)).setProgress(0);
        }
    }

    public void turnOffTheLight() {
        view.setBackgroundColor(getResources().getColor(R.color.gray));
        ((TextView)findViewById(R.id.status_textview)).setText("STATUS: OFF");
        findViewById(R.id.btSwitch).setBackgroundResource(R.drawable.idea2);
    }

    public void turnOnTheLight() {
        view.setBackgroundColor(getResources().getColor(R.color.yellow));
        ((TextView)findViewById(R.id.status_textview)).setText("STATUS: ON");
        findViewById(R.id.btSwitch).setBackgroundResource(R.drawable.idea);
    }

}
