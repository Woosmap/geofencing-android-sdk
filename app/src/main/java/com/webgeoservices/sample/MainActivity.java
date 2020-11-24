package com.webgeoservices.sample;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.webgeoservices.sample.model.PlaceData;
import com.webgeoservices.woosmapgeofencing.DistanceAPIDataModel.DistanceAPI;
import com.webgeoservices.woosmapgeofencing.FigmmForVisitsCreator;
import com.webgeoservices.woosmapgeofencing.Woosmap;
import com.webgeoservices.woosmapgeofencing.WoosmapSettings;
import com.webgeoservices.woosmapgeofencing.database.MovingPosition;
import com.webgeoservices.woosmapgeofencing.database.POI;
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


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private LocationFragment locationFragment;
    private MapFragment mapFragment;
    private VisitFragment visitFragment;

    private boolean isMenuOpen = false;

    private BottomNavigationView bottomNav;

    private Woosmap woosmap;

    public class WoosLocationReadyListener implements Woosmap.LocationReadyListener {
        public void LocationReadyCallback(Location location) {
            onLocationCallback(location);
        }
    }

    private void onLocationCallback(Location currentLocation) {
        new refreshDataTask(getApplicationContext(), MainActivity.this).execute();
        new LocationTask(getApplicationContext(), this, currentLocation).execute();
    }


    public class WoosSearchAPIReadyListener implements Woosmap.SearchAPIReadyListener {
        public void SearchAPIReadyCallback(POI poi) {
            onPOICallback(poi);
        }
    }

    private void onPOICallback(POI poi) {
        new refreshDataTask(getApplicationContext(), MainActivity.this).execute();
        new POITask(getApplicationContext(), this,poi).execute();
    }

    public class WoosDistanceAPIReadyListener implements Woosmap.DistanceAPIReadyListener {
        public void DistanceAPIReadyCallback(DistanceAPI distanceAPIData) {
            onDistanceAPICallback(distanceAPIData);
        }
    }

    private void onDistanceAPICallback(DistanceAPI distanceAPIData) {
        new refreshDataTask(getApplicationContext(), MainActivity.this).execute();
    }

    public class WoosVisitReadyListener implements Woosmap.VisitReadyListener {
        public void VisitReadyCallback(Visit visit) {
            onVisitCallback(visit);
        }
    }

    private void onVisitCallback(Visit visit) {
        new refreshVisitTask(getApplicationContext(), this).execute();
        new VisitTask(getApplicationContext(), this).execute();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!checkPermissions()) {
            requestPermissions();
        }
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationFragment = new LocationFragment();
        mapFragment = new MapFragment();
        visitFragment = new VisitFragment();

        setFragment(locationFragment);
        new refreshDataTask(getApplicationContext(), MainActivity.this).execute();

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_map:
                        new AllLocationTask(getApplicationContext(), MainActivity.this).execute();
                        new AllPOITask(getApplicationContext(), MainActivity.this).execute();
                        new AllZOITask(getApplicationContext(), MainActivity.this).execute();
                        setFragment(mapFragment);
                        return true;
                    case R.id.navigation_location:
                        new refreshDataTask(getApplicationContext(), MainActivity.this).execute();
                        setFragment(locationFragment);
                        return true;
                    case R.id.navigation_visit:
                        new refreshVisitTask(getApplicationContext(), MainActivity.this).execute();
                        new VisitTask(getApplicationContext(), MainActivity.this).execute();
                        setFragment(visitFragment);
                        return true;
                    default:
                        return false;
                }
            }

        });

        final SharedPreferences mPrefs = getApplicationContext().getSharedPreferences("WGSGeofencingPref",MODE_PRIVATE);
        final SharedPreferences.Editor editor = mPrefs.edit();
        boolean trackingEnable = mPrefs.getBoolean("trackingEnable",true);
        boolean searchAPIEnable = mPrefs.getBoolean("searchAPIEnable",true);
        boolean distanceAPIEnable = mPrefs.getBoolean("distanceAPIEnable",true);
        WoosmapSettings.trackingEnable = trackingEnable;
        WoosmapSettings.searchAPIEnable = searchAPIEnable;
        WoosmapSettings.distanceAPIEnable = distanceAPIEnable;

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
                } else {
                    isMenuOpen = false;
                    clearDBBtn.animate().translationY(0);
                    testZOIBtn.animate().translationY(0);
                    enableDistanceAPIBtn.animate().translationY(0);
                    enableSearchAPIBtn.animate().translationY(0);
                    enableLocationBtn.animate().translationY(0);
                }
            }
        });

        // Set Filter on user Location
        //WoosmapSettings.currentLocationTimeFilter = 30;

        // Set Filter on search API
        //WoosmapSettings.searchAPITimeFilter = 30;
        //WoosmapSettings.searchAPIDistanceFilter = 50;

        // Set Filter on Accuracy of the location
        //WoosmapSettings.accuracyFilter = 10;

        // Set classification of zoi enable
        WoosmapSettings.classificationEnable = false;

        // Instanciate woosmap object
        this.woosmap = Woosmap.getInstance().initializeWoosmap(this);

        // Set the Delay of Duration data
        WoosmapSettings.numberOfDayDataDuration = 30;

        // Set Keys
        WoosmapSettings.privateKeyWoosmapAPI = "";
        WoosmapSettings.privateKeyGMPStatic = "";

        WoosmapSettings.modeDistance = "driving";

        this.woosmap.setLocationReadyListener(new WoosLocationReadyListener());
        this.woosmap.setSearchAPIReadyListener(new WoosSearchAPIReadyListener());
        this.woosmap.setDistanceAPIReadyListener(new WoosDistanceAPIReadyListener());
        this.woosmap.setVisitReadyListener(new WoosVisitReadyListener());

        // Visit Detection Enable
        this.woosmap.setVisitEnable(true);

        // For android version >= 8 you have to create a channel or use the woosmap's channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.woosmap.createWoosmapNotifChannel();
        }
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
        int permissionState = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i("WoosmapGeofencing", "Displaying permission rationale to provide additional context.");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        } else {
            Log.i("WoosmapGeofencing", "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i("WoosmapGeofencing", "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i("WoosmapGeofencing", "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("WoosmapGeofencing", "Permission granted, updates requested, starting location updates");
            } else {
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
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

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


    private static class POITask extends AsyncTask<Void, Void, POI> {
        private final Context mContext;
        public MainActivity mActivity;
        private final POI mPOI;

        POITask(Context context, MainActivity activity, POI poi) {
            mContext = context;
            mActivity = activity;
            mPOI = poi;
        }

        @Override
        protected POI doInBackground(Void... voids) {
            POI newPOI = mPOI;
            return newPOI;
        }

        @Override
        protected void onPostExecute(POI poiToShow) {
            if (mPOI == null)
                return;

            if (mActivity.mapFragment.mGoolgeMap != null && mActivity.mapFragment.isVisible()) {

                LatLng latLng = new LatLng(poiToShow.lat, poiToShow.lng);
                MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(poiToShow.city).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                if (!mActivity.mapFragment.markersPOI.isEmpty()) {
                    for (MarkerOptions marker : mActivity.mapFragment.markersPOI) {
                        if (marker.getPosition().equals(markerOptions.getPosition())) {
                            return;
                        }
                    }
                }
                mActivity.mapFragment.markersPOI.add(markerOptions);
                mActivity.mapFragment.poiMarkerList.add(mActivity.mapFragment.mGoolgeMap.addMarker(markerOptions));
                if(!mActivity.mapFragment.POIEnableCheckbox.isChecked()){
                    for (Marker marker : mActivity.mapFragment.poiMarkerList) {
                        marker.setVisible(false);
                    }
                }
            }
        }
    }

    public static class AllZOITask extends AsyncTask<Void, Void, ZOI[]> {
        private final Context mContext;
        public MainActivity mActivity;

        AllZOITask(Context context, MainActivity activity) {
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected ZOI[] doInBackground(Void... voids) {
            ZOI[] ZOIList = WoosmapDb.getInstance(mContext, true).getZOIsDAO().getAllZois();
            return ZOIList;
        }

        @Override
        protected void onPostExecute(ZOI[] ZOIList) {
            mActivity.mapFragment.zois.clear();
            mActivity.mapFragment.clearPolygon();

            if(ZOIList.length == 0)
                return;

            mActivity.mapFragment.zois.addAll(Arrays.asList(ZOIList));

            if (mActivity.mapFragment.mGoolgeMap != null) {
                mActivity.mapFragment.drawPolygon();
            }
        }
    }

    public static class LocationTask extends AsyncTask<Void, Void, MovingPosition> {
        private final Context mContext;
        private final Location mCurrentLocation;
        public MainActivity mActivity;

        LocationTask(Context context, MainActivity activity, Location currentLocation) {
            mContext = context;
            mActivity = activity;
            mCurrentLocation = currentLocation;
        }

        @Override
        protected MovingPosition doInBackground(Void... voids) {
            MovingPosition movingPosition = new MovingPosition();
            movingPosition.lat = mCurrentLocation.getLatitude();
            movingPosition.lng = mCurrentLocation.getLongitude();
            movingPosition.accuracy = mCurrentLocation.getAccuracy();
            movingPosition.dateTime = mCurrentLocation.getTime();

            return movingPosition;
        }

        @Override
        protected void onPostExecute(MovingPosition movingPosition) {

            if (movingPosition == null)
                return;

            if (mActivity.mapFragment.mGoolgeMap != null && mActivity.mapFragment.isVisible()) {
                LatLng latLng = new LatLng(movingPosition.lat, movingPosition.lng);
                MarkerOptions markerOptions = new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                if (!mActivity.mapFragment.markersLocations.isEmpty()) {
                    for (MarkerOptions marker : mActivity.mapFragment.markersLocations) {
                        if (marker.getPosition().equals(markerOptions.getPosition())) {
                            return;
                        }
                    }
                }
                mActivity.mapFragment.markersLocations.add(markerOptions);
                mActivity.mapFragment.locationMarkerList.add(mActivity.mapFragment.mGoolgeMap.addMarker(markerOptions));
                if (!mActivity.mapFragment.locationEnableCheckbox.isChecked()){
                    for (Marker marker : mActivity.mapFragment.locationMarkerList) {
                        marker.setVisible(false);
                    }
                }
            }
        }
    }

    public static class refreshDataTask extends AsyncTask<Void, Void, ArrayList<PlaceData>> {
        private final Context mContext;
        public MainActivity mActivity;

        refreshDataTask(Context context, MainActivity activity) {
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected ArrayList<PlaceData> doInBackground(Void... voids) {
            MovingPosition[] movingPositionList = WoosmapDb.getInstance(mContext, true).getMovingPositionsDao().getMovingPositions(-1);
            ArrayList<PlaceData> arrayOfPlaceData = new ArrayList<>();
            for (MovingPosition locationToShow : movingPositionList) {
                PlaceData place = new PlaceData();
                place.setType( PlaceData.dataType.location );
                place.setLatitude( locationToShow.lat );
                place.setLongitude( locationToShow.lng );
                place.setDate(locationToShow.dateTime);
                place.setLocationId( locationToShow.id );
                POI poibyLocationID =  WoosmapDb.getInstance(mContext, true).getPOIsDAO().getPOIbyLocationID( locationToShow.id );
                if(poibyLocationID != null) {
                    place.setPOILatitude( poibyLocationID.lat );
                    place.setPOILongitude( poibyLocationID.lng );
                    place.setZipCode( poibyLocationID.zipCode );
                    place.setCity( poibyLocationID.city );
                    place.setDistance( poibyLocationID.distance );
                    place.setTravelingDistance( poibyLocationID.travelingDistance );
                    place.setType( PlaceData.dataType.POI );
                    place.setMovingDuration( poibyLocationID.duration );
                }

                arrayOfPlaceData.add( place );
            }
            return arrayOfPlaceData;
        }

        @Override
        protected void onPostExecute(ArrayList<PlaceData> placeDataArrayList) {
            if(mActivity.bottomNav.getMenu().getItem( 0 ).isChecked())
                mActivity.locationFragment.loadData( placeDataArrayList );
        }

    }

    public static class AllLocationTask extends AsyncTask<Void, Void, MovingPosition[]> {
        private final Context mContext;
        public MainActivity mActivity;

        AllLocationTask(Context context, MainActivity activity) {
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected MovingPosition[] doInBackground(Void... voids) {
            MovingPosition[] MovingPositionList = WoosmapDb.getInstance(mContext, true).getMovingPositionsDao().getMovingPositions(50);
            return MovingPositionList;
        }

        @Override
        protected void onPostExecute(MovingPosition[] movingPositionList) {
            if(movingPositionList.length == 0)
                return;

            for (MovingPosition locationToShow : movingPositionList) {
                LatLng latLng = new LatLng( locationToShow.lat, locationToShow.lng );
                boolean markerToAdd = true;
                MarkerOptions markerOptions = new MarkerOptions().position( latLng );
                for (MarkerOptions marker : mActivity.mapFragment.markersLocations) {
                    if (marker.getPosition().equals( markerOptions.getPosition() )) {
                        markerToAdd = false;
                    }
                }
                if (markerToAdd) {
                    mActivity.mapFragment.markersLocations.add( markerOptions );
                }
            }
        }
    }

    public static class AllPOITask extends AsyncTask<Void, Void, POI[]> {
        private final Context mContext;
        public MainActivity mActivity;

        AllPOITask(Context context, MainActivity activity) {
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected POI[] doInBackground(Void... voids) {
            POI[] poiList = WoosmapDb.getInstance(mContext, true).getPOIsDAO().getAllPOIs();
            return poiList;
        }

        @Override
        protected void onPostExecute(POI[] poiList) {
            if(poiList.length == 0)
                return;

            SimpleDateFormat displayDateFormat = new SimpleDateFormat("HH:mm:ss");

            for (POI poiToShow : poiList) {
                LatLng latLng = new LatLng(poiToShow.lat, poiToShow.lng);
                boolean markerToAdd = true;
                MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(poiToShow.city);
                for (MarkerOptions marker : mActivity.mapFragment.markersPOI) {
                    if (marker.getPosition().equals(markerOptions.getPosition())) {
                        markerToAdd = false;
                    }
                }
                if (markerToAdd) {
                    mActivity.mapFragment.markersPOI.add(markerOptions);
                }
            }
        }
    }

    public static class VisitTask extends AsyncTask<Void, Void, Visit[]> {
        private final Context mContext;
        public MainActivity mActivity;

        VisitTask(Context context, MainActivity activity) {
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected Visit[] doInBackground(Void... voids) {

            Visit[] staticList = WoosmapDb.getInstance(mContext, true).getVisitsDao().getAllStaticPositions();
            return staticList;
        }

        @Override
        protected void onPostExecute(Visit[] staticList) {
            SimpleDateFormat displayDateFormat = new SimpleDateFormat("HH:mm:ss");
            for (Visit visitToShow : staticList) {
                if(visitToShow.duration >= WoosmapSettings.durationVisitFilter) {
                    LatLng latLng = new LatLng(visitToShow.lat, visitToShow.lng);
                    String startFormatedDate = displayDateFormat.format(visitToShow.startTime);
                    String endFormatedDate = "";
                    if (visitToShow.endTime == 0) {
                        //Visit in progress
                        endFormatedDate = "ongoing";
                    } else {
                        endFormatedDate = displayDateFormat.format(visitToShow.endTime);
                    }
                    String infoVisites = " --> start: " + startFormatedDate + " / end: " + endFormatedDate + " NbPt : " + visitToShow.nbPoint;
                    MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(infoVisites);
                    boolean markerToUpdate = false;
                    for (MarkerOptions marker : mActivity.mapFragment.markersVisit) {
                        if (marker.getPosition().equals(markerOptions.getPosition())) {
                            //Update marker
                            markerToUpdate = true;
                            marker.title(markerOptions.getTitle());
                        }
                    }
                    if (!markerToUpdate) {
                        mActivity.mapFragment.markersVisit.add(markerOptions);
                        if (mActivity.mapFragment.mGoolgeMap != null) {
                            mActivity.mapFragment.visitMarkerList.add(mActivity.mapFragment.mGoolgeMap.addMarker(markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))));
                            if(!mActivity.mapFragment.visitEnableCheckbox.isChecked()){
                                for (Marker marker : mActivity.mapFragment.visitMarkerList) {
                                    marker.setVisible(false);
                                }
                            }
                        }
                    }
                }
            }
            //Refresh zoi on map on visit
            new AllZOITask(mContext, mActivity).execute();
        }
    }

    public static class refreshVisitTask extends AsyncTask<Void, Void, ArrayList<PlaceData>> {
        private final Context mContext;
        public MainActivity mActivity;

        refreshVisitTask(Context context, MainActivity activity) {
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected ArrayList<PlaceData> doInBackground(Void... voids) {

            Visit[] visitList = WoosmapDb.getInstance(mContext, true).getVisitsDao().getAllStaticPositions();
            ArrayList<PlaceData> arrayOfPlaceData = new ArrayList<>();

            for (Visit visitToShow : visitList) {
                PlaceData place = new PlaceData();
                place.setType( PlaceData.dataType.visit );
                place.setLatitude( visitToShow.lat );
                place.setLongitude( visitToShow.lng );
                place.setArrivalDate( visitToShow.startTime );
                place.setDepartureDate( visitToShow.endTime );
                place.setDuration( visitToShow.duration );
                arrayOfPlaceData.add( place );
            }
            return arrayOfPlaceData;
        }

        @Override
        protected void onPostExecute(ArrayList<PlaceData> placeDataArrayList) {
            if (mActivity.visitFragment.isVisible()) {
                mActivity.visitFragment.loadData( placeDataArrayList );
            }
        }
    }

    public class clearDBTask extends AsyncTask<Void, Void, Void> {
        private final Context mContext;
        public MainActivity mActivity;

        clearDBTask(Context context, MainActivity activity) {
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            WoosmapDb.getInstance(mContext, true).clearAllTables();
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
            FigmmForVisitsCreator figmmForVisitsCreator = new FigmmForVisitsCreator(WoosmapDb.getInstance(mContext, true));
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

                    WoosmapDb.getInstance(mContext, true).getVisitsDao().createStaticPosition(visit);

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

            //Refresh zoi on map on visit
            new VisitTask(mContext, mActivity).execute();
        }
    }

}
