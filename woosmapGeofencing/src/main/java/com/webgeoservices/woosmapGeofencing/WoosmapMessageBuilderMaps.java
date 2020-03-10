package com.webgeoservices.woosmapGeofencing;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.webgeoservices.woosmapGeofencing.SearchAPIDataModel.Feature;
import com.webgeoservices.woosmapGeofencing.SearchAPIDataModel.SearchAPI;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import static com.webgeoservices.woosmapGeofencing.WoosmapSettings.Tags.NotificationError;
import static com.webgeoservices.woosmapGeofencing.WoosmapSettings.getNotificationDefaultUri;

public class WoosmapMessageBuilderMaps {

    private Context context;
    private Class<?> cls = null;
    ApplicationInfo mApplicationInfo;
    private int message_icon;
    private NotificationCompat.Builder mBuilder;
    private PendingIntent mPendingIntent;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Style[] mStyle = new NotificationCompat.Style[1];




    public WoosmapMessageBuilderMaps(Context context){
        this.context = context;
        this.setIconFromManifestVariable();
    }

    public WoosmapMessageBuilderMaps(Context context, Class<?> cls){
        this.context = context;
        this.cls = cls;
        this.setIconFromManifestVariable();


    }

    private void setIconFromManifestVariable(){
        try {
            mApplicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = mApplicationInfo.metaData;
            this.message_icon = bundle.getInt("woosmap.messaging.default_notification_icon", R.drawable.ic_local_grocery_store_black_24dp);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            this.message_icon = R.drawable.ic_local_grocery_store_black_24dp;
        }
    }

    /**
     * Set the notification's small icon
     * @param icon
     */
    public void setSmallIcon(int icon){
        this.message_icon = icon;
    }

    /**
     * Create and show a notification
     *
     * @param datas FCM message body received.
     */
    public void sendWoosmapNotification(final WoosmapMessageDatas datas) {
        Log.d(WoosmapSettings.Tags.WoosmapSdkTag, "Search API");

        /**
         * Compare Timestamp between Server and Mobile to know if the notification is outdated
         */
        if (datas.timestamp != null) {
            Long tsMobile = System.currentTimeMillis()/1000;

            try{
                Long tsServer = Long.parseLong (datas.timestamp);
                if (tsServer + WoosmapSettings.outOfTimeDelay < tsMobile) {
                    Log.d(WoosmapSettings.Tags.WoosmapSdkTag, "Timestamp is outdated");
                    return;
                }
            } catch(NumberFormatException ex){ // handle your exception
                Log.d(WoosmapSettings.Tags.WoosmapSdkTag, "invalid timestamp ");
                return;
            }

        } else {
            Log.d(WoosmapSettings.Tags.WoosmapSdkTag, "No timestamp is define in the payload");
            return;
        }

        mNotificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);

        String CHANNEL_ID = WoosmapSettings.WoosmapNotificationChannel;
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        mBuilder = new NotificationCompat.Builder(this.context, CHANNEL_ID)
                .setSmallIcon(this.message_icon)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        if (datas.icon_url != null){
            mBuilder.setLargeIcon(getBitmapFromURL(datas.icon_url));
        }

        /**
         * Get Position on received FCM notification
         */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Intent resultIntent;
        if (this.cls != null) {
            resultIntent = new Intent(this.context, this.cls);
        }
        else{
            resultIntent = new Intent(Intent.ACTION_VIEW);
            if (datas.open_uri != null){
                resultIntent.setData(Uri.parse(datas.open_uri));
            }else {
                Log.d(WoosmapSettings.Tags.WoosmapSdkTag, "Try to open empty URI");
                resultIntent.setData(Uri.parse(getNotificationDefaultUri(this.context)));
            }
        }
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        resultIntent.putExtra(WoosmapSettings.WoosmapNotification, datas.notificationId);
        Log.d(WoosmapSettings.Tags.WoosmapSdkTag, "notif: "+datas.notificationId);
        mPendingIntent = PendingIntent.getActivity(this.context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        if (!WoosmapSettings.privateKeyGMPStatic.isEmpty () && !WoosmapSettings.privateKeySearchAPI.isEmpty ()){
            sendNotificationWithSearchAPIAndStaticMap ();
        } else if (WoosmapSettings.privateKeyGMPStatic.isEmpty () && !WoosmapSettings.privateKeySearchAPI.isEmpty ()) {
            sendNotificationWithSearchAPI ();
        } else if (!WoosmapSettings.privateKeyGMPStatic.isEmpty () && WoosmapSettings.privateKeySearchAPI.isEmpty ()) {
            sendNotificationWithGMPStatic ();
        } else {
            sendNotificationWithLocation();
        }
    }

    /**
     * Create and show a notification with the result of Search API and a show a google Map Static with the user location
     * and the nearest POI
     *
     */
    private void sendNotificationWithSearchAPIAndStaticMap() {
        getLatestLocation(this.context, new OnSuccessListener<Location> () {
            @Override
            public void onSuccess(final Location location) {
                if (location == null) {
                    Log.d(WoosmapSettings.Tags.WoosmapSdkTag, "Can't get user Location");
                    return;
                }
                //Request Search API with Google map Static
                searchAPIRequest(location,true);
            }
        });
    }

    /**
     * Create and show a notification with the result of Search API
     */
    private void sendNotificationWithSearchAPI() {
        getLatestLocation (this.context, new OnSuccessListener<Location> () {
            @Override
            public void onSuccess(final Location location) {
                if (location == null) {
                    Log.d(WoosmapSettings.Tags.WoosmapSdkTag, "Can't get user Location");
                    return;
                }
                //Request Search API
                searchAPIRequest(location,false);
            }
        });
    }

    /**
     * Create and show a notification with a google map Static
     */
    private void sendNotificationWithGMPStatic() {
        getLatestLocation(this.context, new OnSuccessListener<Location> () {
            @Override
            public void onSuccess(final Location location) {
                if (location == null) {
                    Log.d(WoosmapSettings.Tags.WoosmapSdkTag, "Can't get user Location");
                    return;
                }
                // Fill body message with informations from API
                final String messageBody = "User Location Longitude = " + location.getLongitude () + "\n" + "User Location Latitude = " + location.getLatitude ();
                mBuilder.setContentText (messageBody);
                mBuilder.setContentTitle ("Location Notification");
                // call Google API static map
                googleMapStaticAPIRequest(location, null, null, messageBody);
            }
        });
    }

    /**
     * Create and show a notification with the result of the user location
     */
    private void sendNotificationWithLocation() {
        getLatestLocation(this.context, new OnSuccessListener<Location> () {
            @Override
            public void onSuccess(final Location location) {
                if (location == null) {
                    Log.d(WoosmapSettings.Tags.WoosmapSdkTag, "Can't get user Location");
                    return;
                }
                // Fill body message with informations from API
                final String messageBody = "User Location Longitude = " + location.getLongitude () + "\n" + "User Location Latitude = " + location.getLatitude ();
                mBuilder.setContentTitle ("Location Notification");
                mBuilder.setStyle (new NotificationCompat.BigTextStyle()
                        .bigText(messageBody));
                mBuilder.setContentIntent (mPendingIntent);

                Notification notification = mBuilder.build ();
                mNotificationManager.notify (new Random ().nextInt (20), notification);
            }
        });
    }

    /**
     * Request a Google Map Static
     */
    private void googleMapStaticAPIRequest (Location location, Double longitudePOI, Double latitudePOI, final String messageBody){
        final RequestQueue requestQueue = Volley.newRequestQueue (context);
        // Request Google Maps Static
        String urlGMPStatic = "";
        if (longitudePOI == null) {
            urlGMPStatic = String.format (WoosmapSettings.Urls.GoogleMapStaticUrl1POI, String.valueOf (location.getLatitude ()), String.valueOf (location.getLongitude ()), WoosmapSettings.privateKeyGMPStatic);
        } else {
            urlGMPStatic = String.format (WoosmapSettings.Urls.GoogleMapStaticUrl,String.valueOf (location.getLatitude ()),String.valueOf (location.getLongitude ()),String.valueOf (latitudePOI),String.valueOf (longitudePOI),WoosmapSettings.privateKeyGMPStatic);
        }
        // Retrieves an image specified by the URL, displays it in the UI.
        ImageRequest request = new ImageRequest (urlGMPStatic,
                new Response.Listener<Bitmap> () {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        mStyle[0] = new NotificationCompat.BigPictureStyle ().bigPicture (bitmap).setSummaryText (messageBody);
                        mBuilder.setStyle (mStyle[0]);
                        mBuilder.setContentIntent (mPendingIntent);

                        Notification notification = mBuilder.build ();
                        mNotificationManager.notify (new Random ().nextInt (20), notification);

                    }
                }, 0, 0, null, null,
                new Response.ErrorListener () {
                    public void onErrorResponse(VolleyError error) {
                        Log.e (WoosmapSettings.Tags.WoosmapSdkTag, error.toString () + " maps.google.com");
                        sendErrorNotification(context, "Google API : " + error.toString ());
                    }
                });
        // Add ImageRequest to the RequestQueue
        requestQueue.add (request);
    }

    /**
     * Request SearchAPI nearest the user location
     */
    private void searchAPIRequest(final Location location, final boolean withGoogleMapStatic) {
        final RequestQueue requestQueue = Volley.newRequestQueue (context);
        String urlAPI = String.format (WoosmapSettings.Urls.SearchAPIUrl, WoosmapSettings.privateKeySearchAPI, location.getLatitude (), location.getLongitude ());
        StringRequest stringRequest = new StringRequest (urlAPI, new Response.Listener<String> () {
            @Override
            public void onResponse(String response) {
                Gson gson = new Gson ();
                SearchAPI data = gson.fromJson (response, SearchAPI.class);
                Feature featureSearch = data.getFeatures ()[0];
                String city = featureSearch.getProperties ().getAddress ().getCity ();
                String zipcode = featureSearch.getProperties ().getAddress ().getZipcode ();
                String distance = String.valueOf (featureSearch.getProperties ().getDistance ());
                double longitudePOI = featureSearch.getGeometry ().getCoordinates ()[0];
                double latitudePOI = featureSearch.getGeometry ().getCoordinates ()[1];
                // Fill body message with informations from API
                String messageBody = "city = " + city + "\nzipcode =" + zipcode + "\ndistance = " + distance;

                // With Google Map Static in the notification
                if (withGoogleMapStatic) {
                    googleMapStaticAPIRequest(location,longitudePOI,latitudePOI,messageBody);
                } else {

                    mBuilder.setContentTitle ("Location Notification");
                    mBuilder.setContentIntent (mPendingIntent);
                    mBuilder.setStyle (new NotificationCompat.BigTextStyle ()
                            .bigText (messageBody));
                    mBuilder.setContentIntent (mPendingIntent);
                    Notification notification = mBuilder.build ();
                    mNotificationManager.notify (new Random ().nextInt (20), notification);
                }

            }
        }, new Response.ErrorListener () {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Error request
                Log.e (WoosmapSettings.Tags.WoosmapSdkTag, error.toString () + " search API");
                sendErrorNotification(context, "Search API : " + error.toString ());
            }
        });
        requestQueue.add (stringRequest);
    }

    private static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void getLatestLocation(Context context, OnSuccessListener<Location> successListener) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(WoosmapSettings.Tags.WoosmapSdkTag, "No permission");
        } else {
            FusedLocationProviderClient locationProvider = new FusedLocationProviderClient(context);
            locationProvider.getLastLocation().addOnSuccessListener(successListener);
        }
    }

    private void sendErrorNotification(Context context, String errorMsg) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, WoosmapSettings.WoosmapNotificationChannel)
                .setSmallIcon(R.drawable.ic_shopping_cart_black_24dp)
                .setContentTitle(NotificationError)
                .setContentText(errorMsg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        mNotificationManager.notify(new Random().nextInt(20), builder.build());
    }
}
