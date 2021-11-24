package com.webgeoservices.woosmapgeofencing;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.webgeoservices.woosmapgeofencing.database.Region;
import com.webgeoservices.woosmapgeofencing.database.WoosmapDb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.webgeoservices.woosmapgeofencing.WoosmapSettings.Tags.WoosmapSdkTag;

class LocationManager {

    private LocationRequest mLocationRequest;
    private final FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private PendingIntent mLocationIntent;

    private final GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent;
    private final GeofenceHelper geofenceHelper;

    private final Woosmap woos;
    private final Context context;
    private WoosmapDb db = null;
    private PositionsManager positionsManager = null;

    public LocationManager(Context context, Woosmap woos) {
        this.woos = woos;
        this.context = context;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        db = WoosmapDb.getInstance(context);
        positionsManager = new PositionsManager(context, db);

        mGeofencingClient = LocationServices.getGeofencingClient(context);
        geofenceHelper = new GeofenceHelper(context);

        createLocationCallback();
        createLocationPendingIntent();
    }

    public void removeGeofences() {
        mGeofencingClient.removeGeofences(getGeofencePendingIntent());
        db.getRegionsDAO().deleteAllRegions();
    }

    public void removeGeofences(String id) {
        mGeofencingClient.removeGeofences( Collections.singletonList( id ) );
        positionsManager.removeGeofence(id);
    }


    public void addGeofence(final String id, final LatLng latLng, final float radius, final String idStore, Boolean isCircle) {
        if(isCircle) {
            Geofence geofence = geofenceHelper.getGeofence( id, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT );
            GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest( geofence );
            positionsManager.addGeofence( geofenceHelper, geofencingRequest, getGeofencePendingIntent(), mGeofencingClient, id, radius, latLng.latitude, latLng.longitude, idStore );
        } else {
            positionsManager.createRegion( id,radius,latLng.latitude,latLng.longitude,idStore,false );
        }
    }


    private PendingIntent getGeofencePendingIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
            mGeofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.
                    FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
            mGeofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.
                    FLAG_UPDATE_CURRENT);
        } else {
            Intent intent = new Intent(context, GeofenceTransitionsIntentService.class);
            mGeofencePendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mGeofencePendingIntent;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(this.context, LocationUpdatesBroadcastReceiver.class);
            intent.setAction(LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
            mLocationIntent = PendingIntent.getBroadcast(this.context, 0, intent, flags);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        if(WoosmapSettings.modeHighFrequencyLocation) {
            mLocationRequest.setInterval( 1000 );
            mLocationRequest.setFastestInterval( 1000 );
        } else {
            mLocationRequest.setInterval( 10000 );
            mLocationRequest.setFastestInterval( 5000 );
        }
        mLocationRequest.setPriority( LocationRequest.PRIORITY_HIGH_ACCURACY );
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
        if(WoosmapSettings.foregroundLocationServiceEnable) {
            mFusedLocationClient.removeLocationUpdates(mLocationIntent);
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            return;
        }

        if(WoosmapSettings.modeHighFrequencyLocation) {
            mLocationRequest.setInterval( 1000 );
            mLocationRequest.setFastestInterval( 1000 );
            mLocationRequest.setMaxWaitTime( 5000 );
            mLocationRequest.setPriority( LocationRequest.PRIORITY_HIGH_ACCURACY );
        } else {
            mLocationRequest.setInterval( 240000 );
            mLocationRequest.setFastestInterval( 60000 );
            mLocationRequest.setMaxWaitTime( 480000 );
            mLocationRequest.setPriority( LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY );
        }
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationIntent);
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationIntent);
        } catch (SecurityException e) {
            Log.e(WoosmapSdkTag, "security exception");
        }
    }

    void removeLocationUpdates() {
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationIntent);
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
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

    @SuppressLint("MissingPermission")
    public void setMonitoringRegions() {
        Log.d(WoosmapSdkTag,"Geofence Add on Reboot");
        mGeofencingClient.removeGeofences(getGeofencePendingIntent());
        Region[] regions = db.getRegionsDAO().getAllRegions();
        for (Region regionToAdd : regions) {
            Geofence geofence = geofenceHelper.getGeofence(regionToAdd.identifier, new LatLng( regionToAdd.lng, regionToAdd.lat ), (float) regionToAdd.radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);
            GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
            mGeofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(WoosmapSdkTag,"onSuccess: Geofence Added...");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            String errorMessage=geofenceHelper.getErrorString(e);
                            Log.d(WoosmapSdkTag,"onFailure "+errorMessage);
                        }
                    });
        }
    }
}
