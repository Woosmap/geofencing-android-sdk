package com.webgeoservices.woosmapgeofencing;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.webgeoservices.woosmapgeofencing.database.WoosmapDb;

import java.util.Collections;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;

public class LocationUpdatesService extends Service {
    private static final String PACKAGE_NAME =
            "com.webgeoservices.woosmapgeofencing";

    private static final String TAG = LocationUpdatesService.class.getSimpleName();

    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
            ".started_from_notification";

    private final IBinder mBinder = new LocalBinder();

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 4000;

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * The identifier for the notification displayed for the foreground service.
     */
    private static final int NOTIFICATION_ID = 20200520;

    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */
    private boolean mChangingConfiguration = false;
    private NotificationManager mNotificationManager;
    private LocationRequest mLocationRequest;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Callback for changes in location.
     */
    private LocationCallback mLocationCallback;


    /**
     * The current location.
     */
    private Location mLocation;

    ApplicationInfo mApplicationInfo;
    private int message_icon;

    public LocationUpdatesService() {
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            boolean startedFromNotification = intent.getBooleanExtra( EXTRA_STARTED_FROM_NOTIFICATION,
                    false );
            Log.i( TAG, "onStartCommand" );
            // We got here because the user decided to remove location updates from the notification.
            if (startedFromNotification) {
                removeLocationUpdates();
                stopSelf();
            }
        }
        // Tells the system to not try to recreate the service after it has been killed.
        return START_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "in onBind()");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "in onRebind()");
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind");
        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        if (!mChangingConfiguration && WoosmapSettings.foregroundLocationServiceEnable) {
            Log.i(TAG, "Starting foreground service");
        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        removeLocationUpdates();
        stopForeground(true);
        stopSelf();

        super.onDestroy();
    }

    public void enableLocationBackground(boolean enable) {
        Log.i(TAG, "enableLocationBackground");
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
            if(WoosmapSettings.foregroundLocationServiceEnable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService( new Intent( getApplicationContext(), LocationUpdatesService.class ) );
                } else {
                    startService( new Intent( getApplicationContext(), LocationUpdatesService.class ) );
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                startForeground( NOTIFICATION_ID, getNotification(), FOREGROUND_SERVICE_TYPE_LOCATION );
            } else {
                startForeground( NOTIFICATION_ID,getNotification() );
            }
            mNotificationManager.notify(NOTIFICATION_ID, getNotification());
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    /**
     * Removes location updates.
     * {@link SecurityException}.
     */
    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            stopForeground(true);
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification() {
        Intent intent = new Intent(this, LocationUpdatesService.class);

        //create channel
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;

        PackageManager pm = this.getPackageManager();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            String CHANNEL_ID = WoosmapSettings.WoosmapNotificationChannelID;
            int active = NotificationManager.IMPORTANCE_NONE;
            if(WoosmapSettings.WoosmapNotificationActive) {
                active = NotificationManager.IMPORTANCE_HIGH;
            }
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, WoosmapSettings.WoosmapNotificationChannelName, active);
            channel.setDescription(WoosmapSettings.WoosmapNotificationDescriptionChannel);
            manager.createNotificationChannel(channel);
            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }

        CharSequence text = getLocationText(mLocation);

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
        }

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent, flags);

        Intent newIntent = pm.getLaunchIntentForPackage(this.getPackageName());
        PendingIntent mPendingIntent = PendingIntent.getActivity(this,0, newIntent,  flags);
        setIconFromManifestVariable();
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        return builder
                .setContentText(text)
                .setContentTitle(WoosmapSettings.updateServiceNotificationTitle)
                .setOngoing(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(Notification.PRIORITY_MIN)
                .setSmallIcon( message_icon )
                .setSound( defaultSoundUri )
                .setTicker(text)
                .setContentIntent(mPendingIntent)
                .setWhen(System.currentTimeMillis()).build();

    }


    private void onNewLocation(Location location) {
        Log.i(TAG, "New location: " + location);

        mLocation = location;

        WoosmapDb db = WoosmapDb.getInstance(getApplicationContext());

        PositionsManager positionsManager = new PositionsManager(getApplicationContext(), db);
        positionsManager.asyncManageLocation( Collections.singletonList( mLocation ) );

    }

    /**
     * Sets the location request parameters.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        LocationUpdatesService getService() {
            return LocationUpdatesService.this;
        }
    }


    private void setIconFromManifestVariable() {
        try {
            mApplicationInfo = getApplication().getPackageManager().getApplicationInfo(getApplication().getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = mApplicationInfo.metaData;
            this.message_icon = bundle.getInt("woosmap.messaging.default_notification_icon", R.drawable.ic_local_grocery_store_black_24dp);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            this.message_icon = R.drawable.ic_local_grocery_store_black_24dp;
        }
    }

    static String getLocationText(Location location) {
        if(WoosmapSettings.updateServiceNotificationText.isEmpty()) {
            return location == null ? "Unknown location" :
                    "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
        } else {
            return WoosmapSettings.updateServiceNotificationText;
        }
    }
}