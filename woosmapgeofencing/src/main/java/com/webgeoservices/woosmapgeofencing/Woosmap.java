package com.webgeoservices.woosmapgeofencing;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;

import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;

import androidx.annotation.RequiresApi;

import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;


import com.google.android.gms.maps.model.LatLng;
import com.webgeoservices.woosmapgeofencing.DistanceAPIDataModel.DistanceAPI;
import com.webgeoservices.woosmapgeofencing.database.POI;
import com.webgeoservices.woosmapgeofencing.database.Region;
import com.webgeoservices.woosmapgeofencing.database.RegionLog;
import com.webgeoservices.woosmapgeofencing.database.Visit;
import com.webgeoservices.woosmapgeofencing.database.WoosmapDb;

import java.util.Objects;

import static com.webgeoservices.woosmapgeofencing.WoosmapSettings.Tags.WoosmapSdkTag;


/**
 * Helper to monitor user's positions. This is a singleton use getInstance() method instead of constructors.
 */
public class Woosmap {

    private static volatile Woosmap woosmapInstance;
    private Context context;
    private String fcmToken = "";

    private LocationManager locationManager;
    private Boolean vistingEnable = false;
    private Boolean isForegroundEnabled = false;
    private String asyncTrackNotifOpened = null;
    LocationReadyListener locationReadyListener = null;
    SearchAPIReadyListener searchAPIReadyListener = null;
    VisitReadyListener visitReadyListener = null;
    DistanceAPIReadyListener distanceAPIReadyListener = null;
    RegionReadyListener regionReadyListener = null;
    RegionLogReadyListener regionLogReadyListener = null;

    // A reference to the service used to get location updates.
    private LocationUpdatesService mLocationUpdateService = null;

    // Tracks the bound state of the service.
    private boolean mBound = false;

    /**
     * An interface to add callback on location retrieving
     */
    public interface LocationReadyListener {
        /**
         * When Woosmap get a new location it calls this method
         *
         * @param location an user's location
         */
        void LocationReadyCallback(Location location);
    }

    /**
     * An interface to add callback on Search API retrieving
     */
    public interface SearchAPIReadyListener {
        /**
         * When Woosmap get a new POI it calls this method
         *
         * @param poi an user's location
         */
        void SearchAPIReadyCallback(POI poi);
    }

    /**
     * An interface to add callback on Distance API retrieving
     */
    public interface DistanceAPIReadyListener {
        /**
         * When Woosmap get a new distance it calls this method
         *
         * @param distanceAPIData an user's location
         */
        void DistanceAPIReadyCallback(DistanceAPI distanceAPIData);
    }

    /**
     * An interface to add callback on Visit retrieving
     */
    public interface VisitReadyListener {
        /**
         * When Woosmap get a new Visit it calls this method
         *
         * @param visit an user's location
         */
        void VisitReadyCallback(Visit visit);
    }

    /**
     * An interface to add callback on Region retrieving
     */
    public interface RegionReadyListener {
        /**
         * When Woosmap get a region when is create it calls this method
         *
         * @param region an user's location
         */
        void RegionReadyCallback(Region region);
    }

    /**
     * An interface to add callback on RegionLog retrieving
     */
    public interface RegionLogReadyListener {
        /**
         * When Woosmap get a region when event (enter,exit) it calls this method
         *
         * @param regionLog an user's location
         */
        void RegionLogReadyCallback(RegionLog regionLog);
    }

    private Woosmap() {
        // Prevent form the reflection api
        if (woosmapInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    /**
     * Initialize Woosmap singleton (use automatically FCM to notify)
     *
     * @param context Your application context
     * @see <a href="https://firebase.google.com/docs/cloud-messaging/android/client#sample-register">Firebase documentation</a>
     */
    public Woosmap initializeWoosmap(final Context context) {
        Woosmap woosmapInstance = initializeWoosmap(context, null);
        // Asynchronous loading of fcm token
        WoosmapInstanceIDService.Companion.initializedFCMToken();
        return woosmapInstance;
    }

    /**
     * Initialize Woosmap singleton (notify manual could use GCM or FCM). Use this method only to initialized
     *
     * @param context       Your application context
     * @param messageToken, token give by notification service (GCM or FCM)
     * @return the Woosmap instance which has been initialized
     * @see <a href="https://firebase.google.com/docs/cloud-messaging/android/client#sample-register">Firebase documentation</a>
     */
    private Woosmap initializeWoosmap(Context context, String messageToken) {
        this.setupWoosmap(context);
        if (messageToken != null) {
            this.fcmToken = messageToken;
        }
        this.initWoosmap();

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        context.bindService(new Intent(context, LocationUpdatesService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);


        return woosmapInstance;
    }

    /**
     * Initialize Woosmap singleton in Background. Use this method only to initialized
     *
     * @param context Your application context
     */
    Woosmap initializeWoosmapInBackground(Context context) {
        this.setupWoosmap(context);
        return woosmapInstance;
    }


    /**
     * Add a listener to get callback on new locations
     *
     * @param locationReadyListener
     * @see LocationReadyListener
     */
    public void setLocationReadyListener(LocationReadyListener locationReadyListener) {
        this.locationReadyListener = locationReadyListener;
    }

    /**
     * Add a listener to get callback on new POI
     *
     * @param searchAPIReadyListener
     * @see SearchAPIReadyListener
     */
    public void setSearchAPIReadyListener(SearchAPIReadyListener searchAPIReadyListener) {
        this.searchAPIReadyListener = searchAPIReadyListener;
    }

    /**
     * Add a listener to get callback on new Distance
     *
     * @param distanceAPIReadyListener
     * @see DistanceAPIReadyListener
     */
    public void setDistanceAPIReadyListener(DistanceAPIReadyListener distanceAPIReadyListener) {
        this.distanceAPIReadyListener = distanceAPIReadyListener;
    }

    /**
     * Add a listener to get callback on new Visit
     *
     * @param visitReadyListener
     * @see VisitReadyListener
     */
    public void setVisitReadyListener(VisitReadyListener visitReadyListener) {
        this.visitReadyListener = visitReadyListener;
    }

    /**
     * Add a listener to get callback on create region
     *
     * @param regionReadyListener
     * @see RegionReadyListener
     */
    public void setRegionReadyListener(RegionReadyListener regionReadyListener) {
        this.regionReadyListener = regionReadyListener;
    }

    /**
     * Add a listener to get callback on event region
     *
     * @param regionLogReadyListener
     * @see RegionLogReadyListener
     */
    public void setRegionLogReadyListener(RegionLogReadyListener regionLogReadyListener) {
        this.regionLogReadyListener = regionLogReadyListener;
    }

    private void setupWoosmap(Context context) {
        this.context = context;
        this.locationManager = new LocationManager(context, this);
    }

    /**
     * Return Woosmap singleton instance (if you get it for the first time you should initialized it after)
     *
     * @return Woosmap
     */
    public static Woosmap getInstance() {
        if (woosmapInstance == null) {
            //if there is no instance available... create new one
            woosmapInstance = new Woosmap();
        }
        return woosmapInstance;
    }

    /**
     * Should be call on your mainActivity onResume method
     */
    public void onResume() {
        if(!WoosmapSettings.trackingEnable) {
            return;
        }
        this.isForegroundEnabled = true;
        if (this.shouldTrackUser()) {
            this.locationManager.updateLocationForeground();
        } else {
            Log.d(WoosmapSdkTag, "Get Location permissions error");
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                WoosmapDb.getInstance(context).cleanOldGeographicData(context);
            }
        });
        if(mLocationUpdateService != null && WoosmapSettings.foregroundLocationServiceEnable) {
            mLocationUpdateService.removeLocationUpdates();
        }

    }

    /**
     * Should be call on your mainActivity onPause method
     */
    public void onPause() {
        if(!WoosmapSettings.trackingEnable) {
            return;
        }

        WoosmapSettings.saveSettings(context);

        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            context.unbindService(mServiceConnection);
            mBound = false;
        }

        if(mLocationUpdateService != null && WoosmapSettings.foregroundLocationServiceEnable) {
            mLocationUpdateService.enableLocationBackground( true );
        }


        try {
            if (this.shouldTrackUser()) {
                this.isForegroundEnabled = false;
                this.locationManager.updateLocationBackground();
            } else {
                Log.d(WoosmapSdkTag, "Get Location permissions error");
            }
        } catch (NullPointerException e) {
            Log.d("WoosmapGeofencing", "Foreground inactive");
        }
    }


    void onReboot() {
        this.isForegroundEnabled = false;
        if(!WoosmapSettings.trackingEnable) {
            return;
        }
        if (this.shouldTrackUser()) {
            this.locationManager.setmLocationRequest();
            this.locationManager.updateLocationBackground();
            this.locationManager.setMonitoringRegions();
        } else {
            Log.d(WoosmapSdkTag, "Get Location permissions error");
        }
    }




    private Boolean shouldTrackUser() {
        return this.locationManager.checkPermissions();
    }

    public Boolean enableTracking(boolean trackingEnable) {
        WoosmapSettings.trackingEnable = trackingEnable;
        if(WoosmapSettings.trackingEnable) {
            onResume();
            return true;
        } else {
            this.locationManager.removeLocationUpdates();
            return false;
        }
    }

    public void enableModeHighfrequencyLocation(boolean modeHighfrequencyLocationEnable) {
        WoosmapSettings.modeHighfrequencyLocation = modeHighfrequencyLocationEnable;
        if(WoosmapSettings.modeHighfrequencyLocation) {
            WoosmapSettings.searchAPIEnable = false;
            WoosmapSettings.visitEnable = false;
            WoosmapSettings.classificationEnable = false;
        } else {

        }
        onResume();
    }


    private void initWoosmap() {
        if (fcmToken == null) {
            Log.i(WoosmapSdkTag, "Message Token is null");
        }

        /* Send notifications is opened async if the app was killed */
        if (asyncTrackNotifOpened != null) {
            asyncTrackNotifOpened = null;
        }
        if (isForegroundEnabled) {
            onResume();
        }

    }

    /**
     * For API >= 26 Only
     * Create a notification channel
     */
    @RequiresApi(26)
    public void createWoosmapNotifChannel() {
        NotificationManager mNotificationManager =
                (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        String id = WoosmapSettings.WoosmapNotificationChannel;
        CharSequence name = "WoosmapGeofencing";
        String description = "WoosmapGeofencing Notifs";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        mChannel.setDescription(description);
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.RED);
        mChannel.enableVibration(true);
        mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        Objects.requireNonNull(mNotificationManager).createNotificationChannel(mChannel);
    }

    /**
     * Static function which set the Notification Token
     *
     * @param messageToken
     */
    public static void setMessageToken(String messageToken) {
        Woosmap.getInstance().fcmToken = messageToken;
    }

    public void addGeofence(String id, LatLng latLng, float radius, String idStore) {
        locationManager.addGeofence( id,latLng,radius, idStore );
    }

    public void removeGeofence(String id) {
        locationManager.removeGeofences(id);
    }
    public void removeGeofence() { locationManager.removeGeofences();}

    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mLocationUpdateService = binder.getService();
            mBound = true;
            mLocationUpdateService.requestLocationUpdates();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLocationUpdateService = null;
            mBound = false;
        }
    };
}
