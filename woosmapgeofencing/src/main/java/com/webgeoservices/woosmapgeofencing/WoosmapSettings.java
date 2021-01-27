package com.webgeoservices.woosmapgeofencing;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;


public class WoosmapSettings {

    static String AndroidDeviceModel = "android";
    static String PositionDateFormat = "yyyy-MM-dd'T'HH:mm:ss Z";
    static final String WoosmapNotification = "woosmapNotification";
    static public final String WoosmapNotificationChannel = "woosmap_01";

    //Enable/disable Location
    static public boolean trackingEnable = true;

    //filter time to refresh user location
    static public int currentLocationTimeFilter = 0;

    //filter distance to refresh user location
    static public int currentLocationDistanceFilter = 0;

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
    private static final String drivingMode  = "driving";
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
    static public long minDurationVisitDisplay = 60;
    static public long durationVisitFilter = 1000 * minDurationVisitDisplay;

    //Delay of Duration data
    static public long numberOfDayDataDuration = 30;// number of day
    static public long dataDurationDelay = numberOfDayDataDuration * 1000 * 86400;

    //Active Classification
    static public boolean classificationEnable = true;

    // Key for APIs
    static public String privateKeyGMPStatic = "";
    static public String privateKeyWoosmapAPI = "";

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
