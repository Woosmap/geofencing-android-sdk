package com.webgeoservices.sample;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.urbanairship.UAirship;
import com.urbanairship.analytics.CustomEvent;
import com.webgeoservices.sample.model.PlaceData;
import com.webgeoservices.woosmapgeofencing.DistanceAPIDataModel.DistanceAPI;
import com.webgeoservices.woosmapgeofencing.FigmmForVisitsCreator;
import com.webgeoservices.woosmapgeofencing.PositionsManager;
import com.webgeoservices.woosmapgeofencing.Woosmap;
import com.webgeoservices.woosmapgeofencing.WoosmapSettings;
import com.webgeoservices.woosmapgeofencing.database.MovingPosition;
import com.webgeoservices.woosmapgeofencing.database.POI;
import com.webgeoservices.woosmapgeofencing.database.Region;
import com.webgeoservices.woosmapgeofencing.database.RegionLog;
import com.webgeoservices.woosmapgeofencing.database.Visit;
import com.webgeoservices.woosmapgeofencing.database.WoosmapDb;
import com.webgeoservices.woosmapgeofencing.database.ZOI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlin.Unit;
import se.warting.permissionsui.backgroundlocation.PermissionsUiContracts;


public class MainActivity extends AppCompatActivity {
    public static final boolean AIRSHIP = false;
    static SimpleDateFormat displayDateFormatAirship = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private LocationFragment locationFragment;
    private MapFragment mapFragment;
    private VisitFragment visitFragment;

    private POI[] POIData = new POI[0];

    private boolean isMenuOpen = false;

    private BottomNavigationView bottomNav;

    private Woosmap woosmap;

    public class WoosLocationReadyListener implements Woosmap.LocationReadyListener {
        public void LocationReadyCallback(Location location) {
            onLocationCallback(location);
        }
    }

    private void onLocationCallback(Location currentLocation) {
    }


    public class WoosAirshipSearchAPIReadyListener implements Woosmap.AirshipSearchAPIReadyListener {
        public void AirshipSearchAPIReadyCallback(HashMap<String, Object> dataEvent) {
            sendAirshipEvent( dataEvent );
        }
    }


    public class WoosAirshipVisitReadyListener implements Woosmap.AirshipVisitReadyListener {
        public void AirshipVisitReadyCallback(HashMap<String, Object> dataEvent) {
            sendAirshipEvent( dataEvent );
        }
    }

    public class WoosAirshipRegionLogReadyListener implements Woosmap.AirshipRegionLogReadyListener {
        public void AirshipRegionLogReadyCallback(HashMap<String, Object> dataEvent) {
            sendAirshipEvent( dataEvent );
        }
    }



    void sendAirshipEvent(HashMap<String, Object> dataEvent){
        if(AIRSHIP) {
            // Create and name an event
            CustomEvent.Builder eventBuilder = new CustomEvent.Builder( (String) dataEvent.get( "event" ) );

            // Set custom event properties on the builder
            for (Map.Entry<String, Object> entry : dataEvent.entrySet()) {
                eventBuilder.addProperty(entry.getKey(),  entry.getValue().toString() );
            }

            // Then record it
            CustomEvent event = eventBuilder.build();

            event.track();
        }
    }

    public class WoosMarketingCloudSearchAPIReadyListener implements Woosmap.MarketingCloudSearchAPIReadyListener {
        public void MarketingCloudSearchAPIReadyCallback(HashMap<String, Object> dataEvent) {
            sendMarketingCloudEvent( dataEvent );
        }
    }


    public class WoosMarketingCloudVisitReadyListener implements Woosmap.MarketingCloudVisitReadyListener {
        public void MarketingCloudVisitReadyCallback(HashMap<String, Object> dataEvent) {
            sendMarketingCloudEvent( dataEvent );
        }
    }

    public class WoosMarketingCloudRegionLogReadyListener implements Woosmap.MarketingCloudRegionLogReadyListener {
        public void MarketingCloudRegionLogReadyCallback(HashMap<String, Object> dataEvent) {
            sendMarketingCloudEvent( dataEvent );
        }
    }

    void sendMarketingCloudEvent(HashMap<String, Object> dataEvent){
        // Call Marketing Cloud API here
    }

    public class WoosSearchAPIReadyListener implements Woosmap.SearchAPIReadyListener {
        public void SearchAPIReadyCallback(POI poi) {
            onPOICallback(poi);
        }
    }

    private void onPOICallback(POI poi) {
    }



    public class WoosDistanceAPIReadyListener implements Woosmap.DistanceAPIReadyListener {
        public void DistanceAPIReadyCallback(DistanceAPI distanceAPIData) {
            onDistanceAPICallback(distanceAPIData);
        }
    }

    private void onDistanceAPICallback(DistanceAPI distanceAPIData) {
    }

    public class WoosVisitReadyListener implements Woosmap.VisitReadyListener {
        public void VisitReadyCallback(Visit visit) {
            onVisitCallback(visit);
        }
    }

    private void onVisitCallback(Visit visit) {
    }

    public class WoosRegionReadyListener implements Woosmap.RegionReadyListener {
        public void RegionReadyCallback(Region region) {
            onRegionCallback(region);
        }
    }

    private void onRegionCallback(Region region) {
    }

    public class WoosRegionLogReadyListener implements Woosmap.RegionLogReadyListener {
        public void RegionLogReadyCallback(RegionLog regionLog) {
            onRegionLogCallback(regionLog);
        }
    }

    private void onRegionLogCallback(RegionLog regionLog) {
        createNotification("Region update from geofence detection","Region : " + regionLog.identifier + "\n" + "didenter : " + regionLog.didEnter +
                "\n" + "isCurrentPositionInside : " + regionLog.isCurrentPositionInside +
                "\n" + "Date : " + displayDateFormatAirship.format(regionLog.dateTime));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            Log.d("WoosmapGeofencing", "Permission OK");
            this.woosmap.onResume();
        } else {
            Log.d("WoosmapGeofencing", "Permission NOK");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("WoosmapGeofencing", "BackGround");
        if (checkPermissions()) {
           this.woosmap.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        woosmap.onDestroy();
    };


    public void createNotification(String title, String body) {
        final int NOTIFY_ID = 1002;

        // There are hardcoding only for show it's just strings
        String name = "my_package_channel";
        String id = "my_package_channel_1"; // The user-visible name of the channel.
        String description = "my_package_first_channel"; // The user-visible description of the channel.

        Intent intent;
        PendingIntent pendingIntent;
        NotificationCompat.Builder builder;

        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = notifManager.getNotificationChannel(id);
            if (mChannel == null) {
                mChannel = new NotificationChannel(id, name, importance);
                mChannel.setDescription(description);
                mChannel.enableVibration(true);
                mChannel.setLightColor( Color.GREEN);
                mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                notifManager.createNotificationChannel(mChannel);
            }
            builder = new NotificationCompat.Builder(this, id);

            intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
            }
            pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

            builder.setContentTitle(title)  // required
                    .setSmallIcon(android.R.drawable.ic_popup_reminder) // required
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setContentText(body)
                    .setDefaults( Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setTicker(title)
                    .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        } else {

            builder = new NotificationCompat.Builder(this);

            intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

            builder.setContentTitle(title)                           // required
                    .setSmallIcon(android.R.drawable.ic_popup_reminder) // required
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setContentText(body)  // required
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setTicker(title)
                    .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400})
                    .setPriority(Notification.PRIORITY_HIGH);
        }

        Notification notification = builder.build();
        notifManager.notify(NOTIFY_ID, notification);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationFragment = new LocationFragment();
        mapFragment = new MapFragment();
        visitFragment = new VisitFragment();

        POIData = new POI[0];

        setFragment(mapFragment);
        setFragment(visitFragment);
        setFragment(locationFragment);

        loadData();
        loadPOI();
        loadVisit();
        loadZOI();
        loadRegion();
        loadRegionLogs();

        if(AIRSHIP) {
            String channelId = UAirship.shared().getChannel().getId();
            System.out.println( "My Application Channel ID: " + channelId );
        }

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_map:
                        setFragment(mapFragment);
                        return true;
                    case R.id.navigation_location:
                        setFragment(locationFragment);
                        return true;
                    case R.id.navigation_visit:
                        setFragment(visitFragment);
                        return true;
                    default:
                        return false;
                }
            }

        });

        // TODO this could maybe be remove with new profiles methods
        final SharedPreferences mPrefs = getApplicationContext().getSharedPreferences("WGSGeofencingPref",MODE_PRIVATE);
        boolean trackingEnable = mPrefs.getBoolean("trackingEnable",true);
        boolean searchAPIEnable = mPrefs.getBoolean("searchAPIEnable",true);
        boolean distanceAPIEnable = mPrefs.getBoolean("distanceAPIEnable",true);

        boolean modeHighFrequencyLocationEnable = mPrefs.getBoolean("modeHighFrequencyLocationEnable",false);
        WoosmapSettings.trackingEnable = trackingEnable;
        WoosmapSettings.searchAPIEnable = searchAPIEnable;
        WoosmapSettings.distanceAPIEnable = distanceAPIEnable;
        WoosmapSettings.modeHighFrequencyLocation = modeHighFrequencyLocationEnable;

        // Set Filter on user Location
        //WoosmapSettings.currentLocationTimeFilter = 30;

        // Set Filter on search API
        //WoosmapSettings.searchAPITimeFilter = 30;
        //WoosmapSettings.searchAPIDistanceFilter = 50;

        // Set Filter on Accuracy of the location
        //WoosmapSettings.accuracyFilter = 10;

        // Instanciate woosmap object
        this.woosmap = Woosmap.getInstance().initializeWoosmap(this);

        // Set the Delay of Duration data
        WoosmapSettings.numberOfDayDataDuration = 30;

        // Set Keys
        WoosmapSettings.privateKeyWoosmapAPI = "";
        WoosmapSettings.privateKeyGMPStatic = "";

        WoosmapSettings.foregroundLocationServiceEnable = true;

        this.woosmap.setLocationReadyListener(new WoosLocationReadyListener());
        this.woosmap.setSearchAPIReadyListener(new WoosSearchAPIReadyListener());
        this.woosmap.setDistanceAPIReadyListener(new WoosDistanceAPIReadyListener());
        this.woosmap.setVisitReadyListener(new WoosVisitReadyListener());
        this.woosmap.setRegionReadyListener( new WoosRegionReadyListener() );
        this.woosmap.setRegionLogReadyListener( new WoosRegionLogReadyListener() );

        // Airship Listener
        //this.woosmap.setAirshipSearchAPIReadyListener( new WoosAirshipSearchAPIReadyListener() );
        //this.woosmap.setAirshipVisitReadyListener( new WoosAirshipVisitReadyListener() );
        //this.woosmap.setAirhshipRegionLogReadyListener( new WoosAirshipRegionLogReadyListener() );

        // Marketing CLoud Listener
        //this.woosmap.setMarketingCloudSearchAPIReadyListener( new WoosMarketingCloudSearchAPIReadyListener() );
        //this.woosmap.setMarketingCloudVisitReadyListener( new WoosMarketingCloudVisitReadyListener() );
        //this.woosmap.setMarketingCloudRegionLogReadyListener( new WoosMarketingCloudRegionLogReadyListener() );

        // Search API parameters
        //WoosmapSettings.searchAPIParameters.put("radius","5000");
        //WoosmapSettings.searchAPIParameters.put("stores_by_page","100");

        // User properties filter
        //WoosmapSettings.userPropertiesFilter.add( "creation_year" );

        // Fix the radius of geofence POI
        //WoosmapSettings.poiRadiusNameFromResponse = "creation_year";
        //WoosmapSettings.poiRadius = 500;

        this.woosmap.startTracking( Woosmap.ConfigurationProfile.liveTracking );

        // For android version >= 8 you have to create a channel or use the woosmap's channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.woosmap.createWoosmapNotifChannel();
        }
        if(savedInstanceState== null && !checkPermissions()) {
            requestPermissionsLauncher.launch(null);
        }
        this.InitializeOptionsPanel();
    }

    private void InitializeOptionsPanel() {
        final SharedPreferences mPrefs = getApplicationContext().getSharedPreferences("WGSGeofencingPref",MODE_PRIVATE);
        final SharedPreferences.Editor editor = mPrefs.edit();
        final FloatingActionButton enableLocationUpdateBtn = findViewById(R.id.UpdateLocation);
        enableLocationUpdateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WoosmapSettings.modeHighFrequencyLocation = !WoosmapSettings.modeHighFrequencyLocation;
                String msg = "";
                if(WoosmapSettings.modeHighFrequencyLocation) {
                    msg = "Mode High Frequency Location Enable";
                    editor.putBoolean( "modeHighFrequencyLocationEnable",true);
                    editor.apply();
                    enableLocationUpdateBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
                } else {
                    msg = "Mode High Frequency Location disable";
                    editor.putBoolean( "modeHighFrequencyLocationEnable",false);
                    editor.apply();
                    enableLocationUpdateBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
                }
                woosmap.enableModeHighFrequencyLocation(WoosmapSettings.modeHighFrequencyLocation );
                Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();


            }
        });

        final FloatingActionButton clearDBBtn = findViewById(R.id.clearDB);
        clearDBBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Clear Database", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                new clearDBTask(getApplicationContext(), MainActivity.this).execute();
            }
        });

        final FloatingActionButton testZOIBtn = findViewById(R.id.TestZOI);
        testZOIBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Create ZOI", 8000)
                        .setAction("Action", null).show();
                new testZOITask(getApplicationContext(), MainActivity.this).execute();
                //new testDataImportTask(getApplicationContext(), MainActivity.this).execute();
            }
        });

        final FloatingActionButton enableLocationBtn = findViewById(R.id.EnableLocation);
        enableLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WoosmapSettings.trackingEnable = !WoosmapSettings.trackingEnable;
                String msg = "";
                if(WoosmapSettings.trackingEnable) {
                    msg = "Tracking Enable";
                    editor.putBoolean( "trackingEnable",true);
                    editor.apply();
                    enableLocationBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
                } else {
                    msg = "Tracking Disable";
                    editor.putBoolean( "trackingEnable",false);
                    editor.apply();
                    enableLocationBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
                }
                woosmap.enableTracking(WoosmapSettings.trackingEnable);
                Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        final FloatingActionButton enableSearchAPIBtn = findViewById(R.id.EnableSearchAPI);
        enableSearchAPIBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WoosmapSettings.searchAPIEnable = !WoosmapSettings.searchAPIEnable;
                String msg = "";
                if(WoosmapSettings.searchAPIEnable) {
                    msg = "SearchAPI Enable";
                    editor.putBoolean( "searchAPIEnable",true);
                    editor.apply();
                    enableSearchAPIBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
                }else {
                    msg = "SearchAPI Disable";
                    editor.putBoolean( "searchAPIEnable",false);
                    editor.apply();
                    enableSearchAPIBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
                }
                Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        final FloatingActionButton enableDistanceAPIBtn = findViewById(R.id.EnableDistanceAPI);
        enableDistanceAPIBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WoosmapSettings.distanceAPIEnable = !WoosmapSettings.distanceAPIEnable;
                String msg = "";
                if(WoosmapSettings.distanceAPIEnable) {
                    msg = "DistanceAPI Enable";
                    editor.putBoolean( "distanceAPIEnable",true);
                    editor.apply();
                    enableDistanceAPIBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
                }else {
                    msg = "DistanceAPI Disable";
                    editor.putBoolean( "distanceAPIEnable",false);
                    editor.apply();
                    enableDistanceAPIBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
                }
                Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        if(WoosmapSettings.trackingEnable) {
            enableLocationBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
        } else {
            enableLocationBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
        }

        if(WoosmapSettings.modeHighFrequencyLocation) {
            enableLocationUpdateBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
        } else {
            enableLocationUpdateBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
        }

        if(WoosmapSettings.distanceAPIEnable) {
            enableDistanceAPIBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
        }else {
            enableDistanceAPIBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
        }

        if(WoosmapSettings.searchAPIEnable) {
            enableSearchAPIBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
        }else {
            enableSearchAPIBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
        }

        final FloatingActionButton menuSettings = findViewById(R.id.Menu);
        menuSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isMenuOpen) {
                    isMenuOpen = true;
                    clearDBBtn.animate().translationY(-200);
                    testZOIBtn.animate().translationY(-400);
                    enableDistanceAPIBtn.animate().translationY(-600);
                    enableSearchAPIBtn.animate().translationY(-800);
                    enableLocationBtn.animate().translationY(-1000);
                    enableLocationUpdateBtn.animate().translationY(-1200);
                } else {
                    isMenuOpen = false;
                    clearDBBtn.animate().translationY(0);
                    testZOIBtn.animate().translationY(0);
                    enableDistanceAPIBtn.animate().translationY(0);
                    enableSearchAPIBtn.animate().translationY(0);
                    enableLocationBtn.animate().translationY(0);
                    enableLocationUpdateBtn.animate().translationY(0);
                }
            }
        });

    }

    private void loadPOI() {
        final LiveData<POI[]> POIList = WoosmapDb.getInstance(getApplicationContext()).getPOIsDAO().getAllLivePOIs();
        POIList.observe(this, new Observer<POI[]>() {
            @Override
            public void onChanged(POI[] pois) {
                POIData = pois;
            }
        });
    }

    private void loadVisit() {
        final LiveData<Visit[]> VisitList = WoosmapDb.getInstance(getApplicationContext()).getVisitsDao().getAllLiveStaticPositions();
        VisitList.observe(this, new Observer<Visit[]>() {
            @Override
            public void onChanged(Visit[] visits) {
                final ArrayList<PlaceData> arrayOfPlaceData = new ArrayList<>();
                for (Visit visitToShow : visits) {
                    PlaceData place = new PlaceData();
                    place.setType( PlaceData.dataType.visit );
                    place.setLatitude( visitToShow.lat );
                    place.setLongitude( visitToShow.lng );
                    place.setArrivalDate( visitToShow.startTime );
                    place.setDepartureDate( visitToShow.endTime );
                    place.setDuration( visitToShow.duration );
                    arrayOfPlaceData.add( place );

                    SimpleDateFormat displayDateFormat = new SimpleDateFormat("HH:mm:ss");
                    if(visitToShow.duration >= WoosmapSettings.durationVisitFilter) {
                        LatLng latLng = new LatLng( visitToShow.lat, visitToShow.lng );
                        String startFormatedDate = displayDateFormat.format( visitToShow.startTime );
                        String endFormatedDate = "";
                        if (visitToShow.endTime == 0) {
                            //Visit in progress
                            endFormatedDate = "ongoing";
                        } else {
                            endFormatedDate = displayDateFormat.format( visitToShow.endTime );
                        }
                        String infoVisites = " --> start: " + startFormatedDate + " / end: " + endFormatedDate + " NbPt : " + visitToShow.nbPoint;
                        MarkerOptions markerOptions = new MarkerOptions().position( latLng ).title( infoVisites );
                        boolean markerToUpdate = false;
                        for (MarkerOptions marker : MainActivity.this.mapFragment.markersVisit) {
                            if (marker.getPosition().equals( markerOptions.getPosition() )) {
                                //Update marker
                                markerToUpdate = true;
                                marker.title( markerOptions.getTitle() );
                            }
                        }
                        if (!markerToUpdate) {
                            MainActivity.this.mapFragment.markersVisit.add( markerOptions );
                            if (MainActivity.this.mapFragment.mGoolgeMap != null) {
                                MainActivity.this.mapFragment.visitMarkerList.add( MainActivity.this.mapFragment.mGoolgeMap.addMarker( markerOptions.icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_ORANGE ) ) ) );
                                if (!MainActivity.this.mapFragment.visitEnableCheckbox.isChecked()) {
                                    for (Marker marker : MainActivity.this.mapFragment.visitMarkerList) {
                                        marker.setVisible( false );
                                    }
                                }
                            }
                        }
                    }
                }
                MainActivity.this.visitFragment.loadDataFromVisit( arrayOfPlaceData );
            }
        });
    }

    private void loadRegion() {
        final LiveData<Region[]> regionList = WoosmapDb.getInstance( getApplicationContext() ).getRegionsDAO().getAllLiveRegions();
        regionList.observe( this, new Observer<Region[]>() {
            @Override
            public void onChanged(Region[] regions) {
                MainActivity.this.mapFragment.regions.clear();
                for (Region region : regions) {
                    MainActivity.this.mapFragment.regions.add(region);
                }

                if (MainActivity.this.mapFragment.mGoolgeMap != null) {
                    MainActivity.this.mapFragment.clearCircleGeofence();
                    MainActivity.this.mapFragment.drawCircleGeofence();
                }
            }
        } );
    }

    private void loadZOI() {
        final LiveData<ZOI[]> zoiList = WoosmapDb.getInstance( getApplicationContext() ).getZOIsDAO().getAllLiveZois();
        zoiList.observe( this, new Observer<ZOI[]>() {
            @Override
            public void onChanged(ZOI[] zois) {
                MainActivity.this.mapFragment.zois.clear();
                MainActivity.this.mapFragment.clearPolygon();

                MainActivity.this.mapFragment.zois.addAll(Arrays.asList(zois));

                if (MainActivity.this.mapFragment.mGoolgeMap != null) {
                    MainActivity.this.mapFragment.drawPolygon();
                }
            }
        } );
    }

    private void loadRegionLogs() {
        final LiveData<RegionLog[]> regionLogList = WoosmapDb.getInstance( getApplicationContext() ).getRegionLogsDAO().getAllLiveRegionLogs();
        regionLogList.observe( this, new Observer<RegionLog[]>() {
            @Override
            public void onChanged(RegionLog[] regionLogs) {
                final ArrayList<PlaceData> arrayOfPlaceData = new ArrayList<>();
                for (RegionLog regionLogToShow : regionLogs) {
                    PlaceData place = new PlaceData( regionLogToShow );
                    arrayOfPlaceData.add( place );
                }
                MainActivity.this.visitFragment.loadDataFromRegionLog( arrayOfPlaceData );
            }
        } );
    }

    public void loadData() {
        final LiveData<MovingPosition[]> movingPositionList = WoosmapDb.getInstance(getApplicationContext()).getMovingPositionsDao().getLiveDataMovingPositions(-1);
        movingPositionList.observe(this, new Observer<MovingPosition[]>() {
            @Override
            public void onChanged(MovingPosition[] movingPositions) {
                final ArrayList<PlaceData> arrayOfPlaceData = new ArrayList<>();
                for (MovingPosition locationToShow : movingPositionList.getValue()) {
                    final PlaceData place = new PlaceData();
                    place.setType( PlaceData.dataType.location );
                    place.setLatitude( locationToShow.lat );
                    place.setLongitude( locationToShow.lng );
                    place.setDate(locationToShow.dateTime);
                    place.setLocationId( locationToShow.id );
                    for (POI poi : POIData) {
                        if (poi.locationId == locationToShow.id) {
                            place.setPOILatitude( poi.lat );
                            place.setPOILongitude( poi.lng );
                            place.setZipCode( poi.zipCode );
                            place.setCity( poi.city );
                            place.setDistance( poi.distance );
                            place.setTravelingDistance( poi.travelingDistance );
                            place.setType( PlaceData.dataType.POI );
                            place.setMovingDuration( poi.duration );
                        }
                    }
                    arrayOfPlaceData.add( place );
                    if(place.getType() == PlaceData.dataType.location) {
                        LatLng latLng = new LatLng( place.getLatitude(), place.getLongitude() );
                        boolean markerToAdd = true;
                        MarkerOptions markerOptions = new MarkerOptions().position( latLng ).icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_MAGENTA ) );
                        if (!MainActivity.this.mapFragment.markersLocations.isEmpty()) {
                            for (MarkerOptions marker : MainActivity.this.mapFragment.markersLocations) {
                                if (marker.getPosition().equals( markerOptions.getPosition() )) {
                                    markerToAdd = false;
                                }
                            }
                        }
                        if (markerToAdd) {
                            MainActivity.this.mapFragment.markersLocations.add( markerOptions );
                            if (MainActivity.this.mapFragment.mGoolgeMap != null && MainActivity.this.mapFragment.isVisible()) {
                                MainActivity.this.mapFragment.locationMarkerList.add( MainActivity.this.mapFragment.mGoolgeMap.addMarker( markerOptions ) );
                                if (!MainActivity.this.mapFragment.locationEnableCheckbox.isChecked()) {
                                    for (Marker marker : MainActivity.this.mapFragment.locationMarkerList) {
                                        marker.setVisible( false );
                                    }
                                }
                            }
                        }
                    }

                    if (MainActivity.this.mapFragment.mGoolgeMap != null && MainActivity.this.mapFragment.isVisible()) {

                        if(place.getType() == PlaceData.dataType.POI) {
                            LatLng latLng = new LatLng( place.getPOILatitude(), place.getPOILongitude() );
                            boolean markerToAdd = true;
                            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(place.getCity()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                            if (!MainActivity.this.mapFragment.markersPOI.isEmpty()) {
                                for (MarkerOptions marker : MainActivity.this.mapFragment.markersPOI) {
                                    if (marker.getPosition().equals(markerOptions.getPosition())) {
                                        markerToAdd =false;
                                    }
                                }
                            }
                            if(markerToAdd) {
                                MainActivity.this.mapFragment.markersPOI.add(markerOptions);
                                MainActivity.this.mapFragment.poiMarkerList.add(MainActivity.this.mapFragment.mGoolgeMap.addMarker(markerOptions));
                                if(!MainActivity.this.mapFragment.POIEnableCheckbox.isChecked()){
                                    for (Marker marker : MainActivity.this.mapFragment.poiMarkerList) {
                                        marker.setVisible(false);
                                    }
                                }
                            }
                        }
                    }
                }
                MainActivity.this.locationFragment.loadData( arrayOfPlaceData );
            }
        });
    }

    private void setFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int coarsePermissionState = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        int finePermissionState = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION);
        int backgroundPermissionState = PackageManager.PERMISSION_GRANTED;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            backgroundPermissionState = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        return coarsePermissionState == PackageManager.PERMISSION_GRANTED &&
                finePermissionState == PackageManager.PERMISSION_GRANTED &&
                backgroundPermissionState == PackageManager.PERMISSION_GRANTED;
    }


    private ActivityResultLauncher<Unit> requestPermissionsLauncher =
            registerForActivityResult(new PermissionsUiContracts.RequestBackgroundLocation(), result -> {
                if (!result) {
                    // Permission denied.

                    // Notify the user via a SnackBar that they have rejected a core permission for the
                    // app, which makes the Activity useless. In a real app, core permissions would
                    // typically be best requested during a welcome-screen flow.

                    // Additionally, it is important to remember that a permission might have been
                    // rejected without asking the user for permission (device policy or "Never ask
                    // again" prompts). Therefore, a user interface affordance is typically implemented
                    // when permissions are denied. Otherwise, your app could appear unresponsive to
                    // touches or interactions which have required permissions.
                    showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    // Build intent that displays the App settings screen.
                                    Intent intent = new Intent();
                                    intent.setAction(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                            });
                }
            });

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }


    public static class clearDBTask extends AsyncTask<Void, Void, Void> {
        private final Context mContext;
        public MainActivity mActivity;

        clearDBTask(Context context, MainActivity activity) {
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            WoosmapDb.getInstance(mContext).clearAllTables();
            Woosmap.getInstance().removeGeofence();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mActivity.locationFragment.adapter != null)
                mActivity.locationFragment.clearData();
            if (mActivity.visitFragment.adapter != null)
                mActivity.visitFragment.clearData();
            mActivity.mapFragment.clearMarkers();
        }
    }

    public class testZOITask extends AsyncTask<Void, Void, Void> {
        private final Context mContext;
        public MainActivity mActivity;

        testZOITask(Context context, MainActivity activity) {
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            InputStream in = getResources().openRawResource(R.raw.visit_qualif);
            // if you want less visits and ZOI you can load the file location.csv like that :
            //InputStream in = getResources().openRawResource(R.raw.location);
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SS");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = null;
            FigmmForVisitsCreator figmmForVisitsCreator = new FigmmForVisitsCreator(WoosmapDb.getInstance(mContext));
            try {
                int i = 0;
                while ((line = reader.readLine()) != null) {
                    String[] separated = line.split(",");
                    String id = separated[0];
                    double accuracy = Double.valueOf(separated[1]);
                    String[] valLatLng = separated[2].replace("POINT(","").replace(")","").split(" ");

                    double x = Double.valueOf(valLatLng[0]);
                    double y = Double.valueOf(valLatLng[1]);

                    long startime = formatter.parse(separated[3]).getTime();
                    long endtime = formatter.parse(separated[4]).getTime();

                    Visit visit = new Visit();
                    visit.lat = y;
                    visit.lng = x;
                    visit.startTime = startime;
                    visit.endTime = endtime;
                    visit.accuracy = (float) accuracy;
                    visit.uuid = id;
                    visit.duration = visit.endTime - visit.startTime;

                    WoosmapDb.getInstance(mContext).getVisitsDao().createStaticPosition(visit);

                    figmmForVisitsCreator.figmmForVisitTest(visit);
                    i++;
                }
                figmmForVisitsCreator.update_db();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }

    public class testDataImportTask extends AsyncTask<Void, Void, Void> {
        private final Context mContext;
        public MainActivity mActivity;

        testDataImportTask(Context context, MainActivity activity) {
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            InputStream in = getResources().openRawResource(R.raw.dataimport);
            // if you want less visits and ZOI you can load the file location.csv like that :
            //InputStream in = getResources().openRawResource(R.raw.location);
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SS");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = null;
            PositionsManager mPositionsManager = new PositionsManager(mContext,WoosmapDb.getInstance( mContext ));
            try {
                int i = 0;
                while ((line = reader.readLine()) != null) {
                    String[] separated = line.split(",");
                    String id = separated[0];
                    double accuracy = Double.valueOf(separated[3]);

                    Location loc = new Location("test");
                    loc.setTime( Long.parseLong( separated[5] ) );
                    loc.setAccuracy( (float) accuracy );
                    loc.setLatitude( Double.valueOf(separated[1]));
                    loc.setLongitude(  Double.valueOf(separated[2]));

                    List<Location> listLocations = new ArrayList<Location>();
                    listLocations.add(loc);
                    Thread.sleep(200);
                    mPositionsManager.asyncManageLocation(listLocations);

                    i++;
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }

}
