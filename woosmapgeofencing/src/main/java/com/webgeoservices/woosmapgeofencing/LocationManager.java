package com.webgeoservices.woosmapgeofencing;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.webgeoservices.woosmapgeofencing.database.WoosmapDb;

import java.util.ArrayList;
import java.util.List;

import static com.webgeoservices.woosmapgeofencing.WoosmapSettings.Tags.WoosmapSdkTag;

class LocationManager {

    private LocationRequest mLocationRequest;
    private final FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private PendingIntent mLocationIntent;

    private final Woosmap woos;
    private final Context context;

    public LocationManager(Context context, Woosmap woos) {
        this.woos = woos;
        this.context = context;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        createLocationCallback();
        createLocationPendingIntent();
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location currentLocation = locationResult.getLastLocation();
                Log.d("WoosmapSdk", currentLocation.toString());

                WoosmapDb db = WoosmapDb.getInstance(context, true);
                PositionsManager positionsManager = new PositionsManager(context, db);
                List<Location> listLocations = new ArrayList<Location>();
                listLocations.add(currentLocation);
                if (woos.locationReadyListener != null) {
                    woos.locationReadyListener.LocationReadyCallback(currentLocation);
                }
                positionsManager.asyncManageLocation(listLocations);
            }
        };
    }

    private void createLocationPendingIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(this.context, LocationUpdatesBroadcastReceiver.class);
            intent.setAction(LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES);
            mLocationIntent = PendingIntent.getBroadcast(this.context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            Intent intent = new Intent(this.context, LocationUpdatesIntentService.class);
            intent.setAction(LocationUpdatesIntentService.ACTION_PROCESS_UPDATES);
            mLocationIntent = PendingIntent.getService(this.context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    void setmLocationRequest() {
        mLocationRequest = new LocationRequest();
    }

    void updateLocationForeground() {
        this.setmLocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        //mLocationRequest.setSmallestDisplacement(10);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationIntent);
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, null);//Looper.myLooper());
        } catch (SecurityException e) {
            Log.e(WoosmapSdkTag, "security exception");
        }
    }

    void updateLocationBackground() {
        mLocationRequest.setInterval(240000);
        mLocationRequest.setFastestInterval(60000);
        mLocationRequest.setMaxWaitTime(480000);
        //mLocationRequest.setSmallestDisplacement(50);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationIntent);
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationIntent);
        } catch (SecurityException e) {
            Log.e(WoosmapSdkTag, "security exception");
        }
    }

    void removeLocationCallback() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }


    boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this.context,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }
}
