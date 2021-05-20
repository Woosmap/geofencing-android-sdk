package com.webgeoservices.woosmapgeofencing;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import static android.content.Context.MODE_PRIVATE;

public class WoosmapSettings {

    public static void saveSettings(Context context){
        SharedPreferences mPrefs = context.getSharedPreferences("WGSGeofencingPref",MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        prefsEditor.putBoolean( "modeHighFrequencyLocationEnable", modeHighFrequencyLocation );
        prefsEditor.putBoolean( "trackingEnable",trackingEnable );
        prefsEditor.putInt( "currentLocationTimeFilter",currentLocationTimeFilter );
        prefsEditor.putInt( "currentLocationDistanceFilter",currentLocationDistanceFilter );
        prefsEditor.putBoolean( "searchAPIEnable",searchAPIEnable );
        prefsEditor.putBoolean( "visitEnable",visitEnable );
        prefsEditor.putBoolean( "searchAPICreationRegionEnable",searchAPICreationRegionEnable );
        prefsEditor.putInt( "firstSearchAPIRegionRadius",firstSearchAPIRegionRadius );
        prefsEditor.putInt( "secondSearchAPIRegionRadius",secondSearchAPIRegionRadius );
        prefsEditor.putInt( "thirdSearchAPIRegionRadius",thirdSearchAPIRegionRadius );
        prefsEditor.putInt( "searchAPITimeFilter",searchAPITimeFilter );
        prefsEditor.putInt( "searchAPIDistanceFilter",searchAPIDistanceFilter );
        prefsEditor.putBoolean( "distanceAPIEnable",distanceAPIEnable );
        prefsEditor.putString( "drivingMode",drivingMode );
        prefsEditor.putString( "modeDistance",modeDistance );
        prefsEditor.putInt( "accuracyFilter",accuracyFilter );
        prefsEditor.putInt( "outOfTimeDelay",outOfTimeDelay );
        prefsEditor.putFloat( "distanceDetectionThresholdVisits", (float) distanceDetectionThresholdVisits );
        prefsEditor.putLong("minDurationVisitDisplay",minDurationVisitDisplay);
        prefsEditor.putLong("numberOfDayDataDuration",numberOfDayDataDuration);
        prefsEditor.putBoolean("classificationEnable",classificationEnable );
        prefsEditor.putInt("radiusDetectionClassifiedZOI",radiusDetectionClassifiedZOI );
        prefsEditor.putString( "privateKeyGMPStatic",privateKeyGMPStatic );
        prefsEditor.putString( "privateKeyWoosmapAPI",privateKeyWoosmapAPI );
        prefsEditor.putString( "WoosmapURL",Urls.WoosmapURL);
        prefsEditor.putString( "SearchAPIUrl",Urls.SearchAPIUrl);
        prefsEditor.putString( "DistanceAPIUrl",Urls.DistanceAPIUrl);
        prefsEditor.putString( "GoogleMapStaticUrl",Urls.GoogleMapStaticUrl);
        prefsEditor.putString( "GoogleMapStaticUrl1POI",Urls.GoogleMapStaticUrl1POI);
        prefsEditor.putBoolean( "checkIfPositionIsInsideGeofencingRegionsEnable",checkIfPositionIsInsideGeofencingRegionsEnable );
        prefsEditor.putBoolean( "foregroundLocationServiceEnable",foregroundLocationServiceEnable );
        prefsEditor.putString( "updateServiceNotificationTitle",updateServiceNotificationTitle );
        prefsEditor.putString( "updateServiceNotificationText",updateServiceNotificationText );
        prefsEditor.putString( "WoosmapNotificationChannelID",WoosmapNotificationChannelID );
        prefsEditor.putString( "WoosmapNotificationChannelName",WoosmapNotificationChannelName );
        prefsEditor.putString( "WoosmapNotificationDescriptionChannel",WoosmapNotificationDescriptionChannel );
        prefsEditor.putBoolean( "WoosmapNotificationActive",WoosmapNotificationActive );

        prefsEditor.commit();

    }

    public static void loadSettings(Context context) {
        SharedPreferences mPrefs = context.getSharedPreferences("WGSGeofencingPref", Context.MODE_PRIVATE);

        WoosmapSettings.modeHighFrequencyLocation = mPrefs.getBoolean( "modeHighFrequencyLocationEnable", WoosmapSettings.modeHighFrequencyLocation );
        WoosmapSettings.trackingEnable = mPrefs.getBoolean( "trackingEnable", WoosmapSettings.trackingEnable );
        WoosmapSettings.currentLocationTimeFilter  = mPrefs.getInt( "currentLocationTimeFilter",WoosmapSettings.currentLocationTimeFilter );
        WoosmapSettings.currentLocationDistanceFilter  = mPrefs.getInt( "currentLocationDistanceFilter",WoosmapSettings.currentLocationDistanceFilter );
        WoosmapSettings.searchAPIEnable  = mPrefs.getBoolean( "searchAPIEnable",WoosmapSettings.searchAPIEnable );
        WoosmapSettings.visitEnable  = mPrefs.getBoolean( "visitEnable",WoosmapSettings.visitEnable );
        WoosmapSettings.searchAPICreationRegionEnable  = mPrefs.getBoolean( "searchAPICreationRegionEnable",WoosmapSettings.searchAPICreationRegionEnable );
        WoosmapSettings.firstSearchAPIRegionRadius  = mPrefs.getInt( "firstSearchAPIRegionRadius",WoosmapSettings.firstSearchAPIRegionRadius );
        WoosmapSettings.secondSearchAPIRegionRadius  = mPrefs.getInt( "secondSearchAPIRegionRadius",WoosmapSettings.secondSearchAPIRegionRadius );
        WoosmapSettings.thirdSearchAPIRegionRadius  = mPrefs.getInt( "thirdSearchAPIRegionRadius",WoosmapSettings.thirdSearchAPIRegionRadius );
        WoosmapSettings.searchAPITimeFilter  = mPrefs.getInt( "searchAPITimeFilter",WoosmapSettings.searchAPITimeFilter );
        WoosmapSettings.searchAPIDistanceFilter  = mPrefs.getInt( "searchAPIDistanceFilter",WoosmapSettings.searchAPIDistanceFilter );
        WoosmapSettings.distanceAPIEnable  = mPrefs.getBoolean( "distanceAPIEnable",WoosmapSettings.distanceAPIEnable );
        WoosmapSettings.drivingMode  = mPrefs.getString( "drivingMode",WoosmapSettings.drivingMode );
        WoosmapSettings.modeDistance  = mPrefs.getString( "modeDistance",WoosmapSettings.modeDistance );
        WoosmapSettings.accuracyFilter  = mPrefs.getInt( "accuracyFilter",WoosmapSettings.accuracyFilter );
        WoosmapSettings.outOfTimeDelay  = mPrefs.getInt( "outOfTimeDelay",WoosmapSettings.outOfTimeDelay );
        WoosmapSettings.distanceDetectionThresholdVisits  = mPrefs.getFloat( "distanceDetectionThresholdVisits", (float) WoosmapSettings.distanceDetectionThresholdVisits );
        WoosmapSettings.minDurationVisitDisplay  = mPrefs.getLong("minDurationVisitDisplay",WoosmapSettings.minDurationVisitDisplay);
        WoosmapSettings.numberOfDayDataDuration  = mPrefs.getLong("numberOfDayDataDuration",WoosmapSettings.minDurationVisitDisplay);
        WoosmapSettings.classificationEnable  = mPrefs.getBoolean("classificationEnable",WoosmapSettings.classificationEnable );
        WoosmapSettings.radiusDetectionClassifiedZOI  = mPrefs.getInt( "radiusDetectionClassifiedZOI",WoosmapSettings.radiusDetectionClassifiedZOI );
        WoosmapSettings.privateKeyGMPStatic  = mPrefs.getString( "privateKeyGMPStatic",WoosmapSettings.privateKeyGMPStatic );
        WoosmapSettings.privateKeyWoosmapAPI  = mPrefs.getString( "privateKeyWoosmapAPI",WoosmapSettings.privateKeyWoosmapAPI );
        WoosmapSettings.Urls.WoosmapURL  = mPrefs.getString( "WoosmapURL",WoosmapSettings.Urls.WoosmapURL);
        WoosmapSettings.Urls.SearchAPIUrl = mPrefs.getString( "SearchAPIUrl",WoosmapSettings.Urls.SearchAPIUrl);
        WoosmapSettings.Urls.DistanceAPIUrl  = mPrefs.getString( "DistanceAPIUrl",WoosmapSettings.Urls.DistanceAPIUrl);
        WoosmapSettings.Urls.GoogleMapStaticUrl  = mPrefs.getString( "GoogleMapStaticUrl",WoosmapSettings.Urls.GoogleMapStaticUrl);
        WoosmapSettings.Urls.GoogleMapStaticUrl1POI  = mPrefs.getString( "GoogleMapStaticUrl1POI",WoosmapSettings.Urls.GoogleMapStaticUrl1POI);
        WoosmapSettings.checkIfPositionIsInsideGeofencingRegionsEnable = mPrefs.getBoolean( "checkIfPositionIsInsideGeofencingRegionsEnable", WoosmapSettings.checkIfPositionIsInsideGeofencingRegionsEnable );
        WoosmapSettings.foregroundLocationServiceEnable = mPrefs.getBoolean( "foregroundLocationServiceEnable", WoosmapSettings.foregroundLocationServiceEnable );
        WoosmapSettings.updateServiceNotificationTitle = mPrefs.getString( "updateServiceNotificationTitle", WoosmapSettings.updateServiceNotificationTitle );
        WoosmapSettings.updateServiceNotificationText = mPrefs.getString( "updateServiceNotificationText", WoosmapSettings.updateServiceNotificationText );
        WoosmapSettings.WoosmapNotificationChannelID  = mPrefs.getString( "WoosmapNotificationChannelID",WoosmapSettings.WoosmapNotificationChannelID );
        WoosmapSettings.WoosmapNotificationChannelName  = mPrefs.getString( "WoosmapNotificationChannelName",WoosmapSettings.WoosmapNotificationChannelID );
        WoosmapSettings.WoosmapNotificationDescriptionChannel  = mPrefs.getString( "WoosmapNotificationDescriptionChannel",WoosmapSettings.WoosmapNotificationDescriptionChannel );
        WoosmapSettings.WoosmapNotificationActive = mPrefs.getBoolean( "WoosmapNotificationActive", WoosmapSettings.WoosmapNotificationActive );

    }

    static String AndroidDeviceModel = "android";
    static String PositionDateFormat = "yyyy-MM-dd'T'HH:mm:ss Z";
    static final String WoosmapNotification = "woosmapNotification";
    static public String WoosmapNotificationChannelID = "Location Channel ID";
    static public String WoosmapNotificationChannelName = "Location Channel Name";
    static public String WoosmapNotificationDescriptionChannel = "Location Channel";
    static public boolean WoosmapNotificationActive = false;

    //Enable/disable Location
    static public boolean modeHighFrequencyLocation = false;

    //Enable/disable Location
    static public boolean trackingEnable = true;

    //filter time to refresh user location
    static public int currentLocationTimeFilter = 0;

    //filter distance to refresh user location
    static public int currentLocationDistanceFilter = 0;

    //Enable/disable VisitEnable
    static public boolean visitEnable = true;

    //Enable/disable SearchAPI
    static public boolean searchAPIEnable = true;

    //Enable/disable Creation region on SearchAPI
    static public boolean searchAPICreationRegionEnable = true;

    //Radius region From SearchAPI (m)
    static public int firstSearchAPIRegionRadius = 100;
    static public int secondSearchAPIRegionRadius = 200;
    static public int thirdSearchAPIRegionRadius = 300;

    //filter time to request Search API
    static public int searchAPITimeFilter = 0;

    //filter distance to request Search API
    static public int searchAPIDistanceFilter = 0;

    //Enable/disable DistanceAPI
    static public boolean distanceAPIEnable = true;

    //Mode transportation DistanceAPI
    private static String drivingMode  = "driving";
    private static final String walkingMode  = "walking";
    private static final String cyclingMode  = "cycling";
    static public String modeDistance = drivingMode;

    //Filter Accuracy of the location
    static public int accuracyFilter = 100;

    // delay for outdated notification
    static public int outOfTimeDelay = 300;

    // Distance detection threshold for visits
    static public double distanceDetectionThresholdVisits = 25.0;

    // Distance detection threshold for visits
    static public long minDurationVisitDisplay = 60 * 5;
    static public long durationVisitFilter = 1000 * minDurationVisitDisplay;

    //Delay of Duration data
    static public long numberOfDayDataDuration = 30;// number of day
    static public long dataDurationDelay = numberOfDayDataDuration * 1000 * 86400;

    //Active Classification
    static public boolean classificationEnable = true;

    // Distance detection threshold for a ZOI classified
    static public int radiusDetectionClassifiedZOI = 50;

    // Key for APIs
    static public String privateKeyGMPStatic = "";
    static public String privateKeyWoosmapAPI = "";

    //Checking Position is inside a region
    static public boolean checkIfPositionIsInsideGeofencingRegionsEnable = true;

    //Notification ForegroundService
    static public boolean foregroundLocationServiceEnable = false;
    static public String updateServiceNotificationTitle = "Location updated";
    static public String updateServiceNotificationText = "This app use your location";


    public static class Tags {
        public static final String WoosmapSdkTag = "WoosmapSdk";
        static final String WoosmapBackgroundTag = "WoosmapBackground";
        static String WoosmapVisitsTag = "WoosmapVisit";
        static String NotificationError = "NotificationError";
        static String WoosmapBroadcastTag = "WoosmapBroadcast";
        static String WoosmapGeofenceTag = "WoosmapGeofence";
    }

    static class Urls {
        static String WoosmapURL = "https://api.woosmap.com";
        static String SearchAPIUrl = "%s/stores/search/?private_key=%s&lat=%s&lng=%s&stores_by_page=1";
        static String DistanceAPIUrl ="%s/distance/distancematrix/json?mode=%s&units=metric&origins=%s,%s&destinations=%s&private_key=%s&elements=duration_distance";
        static String GoogleMapStaticUrl = "https://maps.google.com/maps/api/staticmap?markers=color:red%%7C%s,%s&markers=color:blue%%7C%s,%s&zoom=14&size=400x400&sensor=true&key=%s";
        static String GoogleMapStaticUrl1POI = "https://maps.google.com/maps/api/staticmap?markers=color:red%%7C%s,%s&zoom=14&size=400x400&sensor=true&key=%s";
    }


    public static String getNotificationDefaultUri(Context context) {
        String notificationUri = "";
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            notificationUri = bundle.getString("woosmap_notification_defautl_uri");
            Log.d(Tags.WoosmapSdkTag, "notification defautl uri : " + notificationUri);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(Tags.WoosmapSdkTag, "Failed to load project key, NameNotFound: " + e.getMessage());
        } catch (NullPointerException e) {
            Log.e(Tags.WoosmapSdkTag, "Failed to load meta-data, NullPointer: " + e.getMessage());
        }
        return notificationUri;
    }
}
