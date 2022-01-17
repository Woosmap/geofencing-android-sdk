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

import android.os.IBinder;
import android.util.Log;


import com.google.android.gms.maps.model.LatLng;
import com.webgeoservices.woosmapgeofencing.database.Distance;
import com.webgeoservices.woosmapgeofencing.database.POI;
import com.webgeoservices.woosmapgeofencing.database.Region;
import com.webgeoservices.woosmapgeofencing.database.RegionLog;
import com.webgeoservices.woosmapgeofencing.database.Visit;
import com.webgeoservices.woosmapgeofencing.database.WoosmapDb;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Executors;

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

    AirshipSearchAPIReadyListener airshipSearchAPIReadyListener = null;
    AirshipVisitReadyListener airshipVisitReadyListener = null;
    AirshipRegionLogReadyListener airshipRegionLogReadyListener = null;

    MarketingCloudSearchAPIReadyListener marketingCloudSearchAPIReadyListener = null;
    MarketingCloudVisitReadyListener marketingCloudVisitReadyListener = null;
    MarketingCloudRegionLogReadyListener marketingCloudRegionLogReadyListener = null;

    LocationReadyListener locationReadyListener = null;
    SearchAPIReadyListener searchAPIReadyListener = null;
    VisitReadyListener visitReadyListener = null;
    DistanceReadyListener distanceReadyListener = null;
    RegionReadyListener regionReadyListener = null;
    RegionLogReadyListener regionLogReadyListener = null;

    // A reference to the service used to get location updates.
    private LocationUpdatesService mLocationUpdateService = null;

    public interface AirshipSearchAPIReadyListener {

        /**
         * When Woosmap get a new POI it calls this method to create a event for airship
         *
         * @param dataEvent an dictonnary of dataPOI
         */
        void AirshipSearchAPIReadyCallback(HashMap<String, Object> dataEvent);
    }

    /**
     * An interface to add callback on Visit retrieving
     */
    public interface AirshipVisitReadyListener {
        /**
         * When Woosmap get a new Visit it calls this method to create a event for airship
         *
         * @param dataEvent an dictonnary of a Visit
         */
        void AirshipVisitReadyCallback(HashMap<String, Object> dataEvent);
    }

    /**
     * An interface to add callback on RegionLog retrieving
     */
    public interface AirshipRegionLogReadyListener {
        /**
         * When Woosmap get a region when event (enter,exit) it calls this method to create a event for airship
         *
         * @param dataEvent an dictonnary of a Region Log
         */
        void AirshipRegionLogReadyCallback(HashMap<String, Object> dataEvent);
    }

    public interface MarketingCloudSearchAPIReadyListener {

        /**
         * When Woosmap get a new POI it calls this method to create a event for Marketing Cloud
         *
         * @param dataEvent an dictonnary of dataPOI
         */
        void MarketingCloudSearchAPIReadyCallback(HashMap<String, Object> dataEvent);
    }

    /**
     * An interface to add callback on Visit retrieving
     */
    public interface MarketingCloudVisitReadyListener {
        /**
         * When Woosmap get a new Visit it calls this method to create a event for Marketing Cloud
         *
         * @param dataEvent an dictonnary of a Visit
         */
        void MarketingCloudVisitReadyCallback(HashMap<String, Object> dataEvent);
    }

    /**
     * An interface to add callback on RegionLog retrieving
     */
    public interface MarketingCloudRegionLogReadyListener {
        /**
         * When Woosmap get a region when event (enter,exit) it calls this method to create a event for Marketing Cloud
         *
         * @param dataEvent an dictonnary of a Region Log
         */
        void MarketingCloudRegionLogReadyCallback(HashMap<String, Object> dataEvent);
    }
  
    public final class ConfigurationProfile {

        public static final String liveTracking = "liveTracking";
        public static final String passiveTracking = "passiveTracking";
        public static final String stopsTracking = "visitsTracking";

        private ConfigurationProfile() { }
    }

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
    public interface DistanceReadyListener {
        /**
         * When Woosmap get a new distance it calls this method
         *
         * @param distances array of distance reponse API
         */
        void DistanceReadyCallback(Distance[] distances);
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
     * Add a listener to get callback on new POI for Airship
     *
     * @param airshipSearchAPIReadyListener
     * @see AirshipSearchAPIReadyListener
     */
    public void setAirshipSearchAPIReadyListener(AirshipSearchAPIReadyListener airshipSearchAPIReadyListener) {
        this.airshipSearchAPIReadyListener = airshipSearchAPIReadyListener;
    }

    /**
     * Add a listener to get callback on new Visit for airship
     *
     * @param airshipVisitReadyListener
     * @see AirshipVisitReadyListener
     */
    public void setAirshipVisitReadyListener(AirshipVisitReadyListener airshipVisitReadyListener) {
        this.airshipVisitReadyListener = airshipVisitReadyListener;
    }

    /**
     * Add a listener to get callback on event region for airship
     *
     * @param airshipRegionLogReadyListener
     * @see AirshipRegionLogReadyListener
     */
    public void setAirhshipRegionLogReadyListener(AirshipRegionLogReadyListener airshipRegionLogReadyListener) {
        this.airshipRegionLogReadyListener = airshipRegionLogReadyListener;
    }

    /**
     * Add a listener to get callback on new POI for MarketingCloud
     *
     * @param marketingCloudSearchAPIReadyListener
     * @see MarketingCloudSearchAPIReadyListener
     */
    public void setMarketingCloudSearchAPIReadyListener(MarketingCloudSearchAPIReadyListener marketingCloudSearchAPIReadyListener) {
        this.marketingCloudSearchAPIReadyListener = marketingCloudSearchAPIReadyListener;
    }

    /**
     * Add a listener to get callback on new Visit for MarketingCloud
     *
     * @param marketingCloudVisitReadyListener
     * @see MarketingCloudVisitReadyListener
     */
    public void setMarketingCloudVisitReadyListener(MarketingCloudVisitReadyListener marketingCloudVisitReadyListener) {
        this.marketingCloudVisitReadyListener = marketingCloudVisitReadyListener;
    }

    /**
     * Add a listener to get callback on event region for MarketingCloud
     *
     * @param marketingCloudRegionLogReadyListener
     * @see MarketingCloudRegionLogReadyListener
     */
    public void setMarketingCloudRegionLogReadyListener(MarketingCloudRegionLogReadyListener marketingCloudRegionLogReadyListener) {
        this.marketingCloudRegionLogReadyListener = marketingCloudRegionLogReadyListener;
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
     * @param distanceReadyListener
     * @see DistanceReadyListener
     */
    public void setDistanceReadyListener(DistanceReadyListener distanceReadyListener) {
        this.distanceReadyListener = distanceReadyListener;
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
        setRegionLogReadyListener(regionLogReadyListener,false);
    }

    /**
     * Add a listener to get callback on event region
     *
     * @param regionLogReadyListener
     * @see RegionLogReadyListener
     */
    public void setRegionLogReadyListener(RegionLogReadyListener regionLogReadyListener, Boolean sendCurrentState) {
        this.regionLogReadyListener = regionLogReadyListener;
        if(sendCurrentState) {
            getLastRegionState();
        }
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
        if  (woosmapInstance == null) {
            synchronized (Woosmap.class) {
                if (woosmapInstance == null) {
                    woosmapInstance = new Woosmap();
                }
            }
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

        Executors.newSingleThreadExecutor().execute(new Runnable() {
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

        if(WoosmapSettings.foregroundLocationServiceEnable){
            if(mLocationUpdateService != null ) {
                mLocationUpdateService.enableLocationBackground( true );
            }else {
                // Bind to the service. If the service is in foreground mode, this signals to the service
                // that since this activity is in the foreground, the service can exit foreground mode.
                getInstance().context.getApplicationContext().bindService(new Intent(context.getApplicationContext(), LocationUpdatesService.class), mServiceConnection,Context.BIND_AUTO_CREATE);
            }
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

    public void onDestroy() {

        if(mLocationUpdateService != null && WoosmapSettings.foregroundLocationServiceEnable) {
            mLocationUpdateService.removeLocationUpdates();
        }
        mLocationUpdateService = null;
        if(WoosmapSettings.trackingEnable) {
            getInstance().context.unbindService( mServiceConnection );
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

    public void enableModeHighFrequencyLocation(boolean modeHighFrequencyLocationEnable) {
        WoosmapSettings.modeHighFrequencyLocation = modeHighFrequencyLocationEnable;
        if(WoosmapSettings.modeHighFrequencyLocation) {
            WoosmapSettings.searchAPIEnable = false;
            WoosmapSettings.visitEnable = false;
            WoosmapSettings.classificationEnable = false;
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
        String id = WoosmapSettings.WoosmapNotificationChannelID;
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

    public void addGeofence(String id, LatLng latLng, float radius, String type) {
        addGeofence( id,latLng,radius, "", type);
    }

    public void addGeofence(String id, LatLng latLng, float radius) {
        addGeofence( id,latLng,radius, "", "circle" );
    }

    public void addGeofence(String id, LatLng latLng, float radius, String idStore, String type) {
        locationManager.addGeofence( id,latLng,radius, idStore, type );
    }

    public void removeGeofence(String id) {
        locationManager.removeGeofences(id);
    }
    public void removeGeofence() { locationManager.removeGeofences();}

    // Monitors the state of the connection to the service.
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("LocationUpdatesService", "onServiceConnected");
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mLocationUpdateService = binder.getService();
            mLocationUpdateService.enableLocationBackground( true );
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("LocationUpdatesService", "onServiceDisconnected");
            mLocationUpdateService = null;
        }
    };

    public void getLastRegionState() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                RegionLog rLog = WoosmapDb.getInstance(context).getRegionLogsDAO().getLastRegionLog();
                if (Woosmap.getInstance().regionLogReadyListener != null && rLog != null) {
                    Woosmap.getInstance().regionLogReadyListener.RegionLogReadyCallback(rLog);
                }
            }
        });
    }

    public void stopTracking() {
        WoosmapSettings.trackingEnable = false;
        this.locationManager.removeLocationUpdates();
    }

    public String loadJSONFromAsset(String fileName) {
        String json = null;
        try {
            InputStream is = this.context.getAssets().open(fileName +".json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public void startTracking(String profile) {

        try {

            JSONObject obj = new JSONObject(loadJSONFromAsset(profile));
            WoosmapSettings.trackingEnable = obj.getBoolean( "trackingEnable" );
            WoosmapSettings.foregroundLocationServiceEnable = obj.getBoolean( "foregroundLocationServiceEnable" );
            WoosmapSettings.modeHighFrequencyLocation = obj.getBoolean( "modeHighFrequencyLocation" );

            WoosmapSettings.visitEnable = obj.getBoolean( "visitEnable" );
            WoosmapSettings.classificationEnable = obj.getBoolean( "classificationEnable" );
            if(!obj.isNull( "minDurationVisitDisplay" )) {
                WoosmapSettings.minDurationVisitDisplay = obj.getLong( "minDurationVisitDisplay" );
            }
            if(!obj.isNull( "radiusDetectionClassifiedZOI" )) {
                WoosmapSettings.radiusDetectionClassifiedZOI = obj.getInt( "radiusDetectionClassifiedZOI" );
            }
            if(!obj.isNull( "distanceDetectionThresholdVisits" )) {
                WoosmapSettings.distanceDetectionThresholdVisits = obj.getDouble( "distanceDetectionThresholdVisits" );
            }
            WoosmapSettings.creationOfZOIEnable = obj.getBoolean( "creationOfZOIEnable" );

            WoosmapSettings.currentLocationTimeFilter = obj.getInt( "currentLocationTimeFilter" );
            WoosmapSettings.currentLocationDistanceFilter = obj.getInt( "currentLocationTimeFilter" );
            WoosmapSettings.accuracyFilter = obj.getInt( "accuracyFilter" );

            WoosmapSettings.searchAPIEnable = obj.getBoolean( "searchAPIEnable" );
            WoosmapSettings.searchAPICreationRegionEnable = obj.getBoolean( "searchAPICreationRegionEnable" );
            WoosmapSettings.searchAPITimeFilter = obj.getInt( "searchAPITimeFilter" );
            WoosmapSettings.searchAPIDistanceFilter = obj.getInt( "searchAPIDistanceFilter" );
            WoosmapSettings.searchAPIRefreshDelayDay = obj.getInt( "searchAPIRefreshDelayDay" );


            WoosmapSettings.distanceAPIEnable = obj.getBoolean( "distanceAPIEnable" );
            WoosmapSettings.outOfTimeDelay = obj.getInt( "outOfTimeDelay" );
            WoosmapSettings.numberOfDayDataDuration = obj.getLong( "dataDurationDelay" );

            WoosmapSettings.setDistanceProvider( obj.getJSONObject( "distance" ).getString( "distanceProvider" ) );
            WoosmapSettings.setModeDistance( obj.getJSONObject( "distance" ).getString( "distanceMode" ) );
            WoosmapSettings.setDistanceUnits( obj.getJSONObject( "distance" ).getString( "distanceUnits" ) );
            WoosmapSettings.setTrafficDistanceRouting( obj.getJSONObject( "distance" ).getString( "distanceRouting" ) );
            WoosmapSettings.setDistanceLanguage( obj.getJSONObject( "distance" ).getString( "distanceLanguage" ) );
            WoosmapSettings.setDistanceMaxAirDistanceFilter( obj.getJSONObject( "distance" ).getInt( "distanceMaxAirDistanceFilter" ) );
            WoosmapSettings.setDistanceTimeFilter( obj.getJSONObject( "distance" ).getInt( "distanceTimeFilter" ) );

            enableTracking(WoosmapSettings.trackingEnable);

            WoosmapSettings.saveSettings(context);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



}
