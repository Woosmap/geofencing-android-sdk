package com.webgeoservices.sample;


import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.webgeoservices.woosmapGeofencing.Woosmap;
import com.webgeoservices.woosmapGeofencing.WoosmapSettings;
import com.webgeoservices.woosmapGeofencing.database.MovingPosition;
import com.webgeoservices.woosmapGeofencing.database.POI;
import com.webgeoservices.woosmapGeofencing.database.Visit;
import com.webgeoservices.woosmapGeofencing.database.WoosmapDb;

import java.text.SimpleDateFormat;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private LocationFragment locationFragment;
    private MapFragment mapFragment;
    private VisitFragment visitFragment;

    private Woosmap woosmap;

    public class WoosLocationReadyListener implements Woosmap.LocationReadyListener
    {
        public void LocationReadyCallback(Location location)
        {
            onLocationCallback(location);
        }
    }

    private void onLocationCallback(Location currentLocation) {
        new LocationTask(getApplicationContext (),this).execute();
    }


    public class WoosSearchAPIReadyListener implements Woosmap.SearchAPIReadyListener
    {
        public void SearchAPIReadyCallback(POI poi)
        {
            onPOICallback(poi);
        }
    }

    private void onPOICallback(POI poi) {
        new POITask(getApplicationContext (),this).execute();
    }

    public class WoosVisitReadyListener implements Woosmap.VisitReadyListener
    {
        public void VisitReadyCallback(Visit visit)
        {
            onVisitCallback(visit);
        }
    }

    private void onVisitCallback(Visit visit) {
        new visitTask (getApplicationContext (),this).execute();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            Log.d("WoosmapGeofencing", "Permission OK");
            this.woosmap.onResume();
        } else {
            Log.d("WoosmapGeofencing", "Permission NOK");
            requestPermissions();
        }
    }

    @Override
    public void onPause(){
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

        locationFragment = new LocationFragment ();
        mapFragment = new MapFragment ();
        visitFragment = new VisitFragment ();


        new AllPOITask (getApplicationContext (),MainActivity.this).execute();
        new visitTask (getApplicationContext (),MainActivity.this).execute();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        setFragment(locationFragment);
        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_map:
                        setFragment(mapFragment);
                        return true;
                    case R.id.navigation_location:
                        new AllPOITask (getApplicationContext (),MainActivity.this).execute();
                        new visitTask (getApplicationContext (),MainActivity.this).execute();
                        setFragment(locationFragment);
                        return true;
                    case R.id.navigation_visit:
                        new visitTask (getApplicationContext (),MainActivity.this).execute();
                        setFragment(visitFragment);
                        return true;
                    default:
                        return false;
                }
            }

        });

        FloatingActionButton clearDBBtn = findViewById(R.id.clearDB);
        clearDBBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Clear Database", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                new clearDBTask (getApplicationContext (),MainActivity.this).execute ();
            }
        });

        // Set Filter on user Location
        //WoosmapSettings.currentLocationTimeFilter = 30;

        // Set Filter on search API
        //WoosmapSettings.searchAPITimeFilter = 30;
        //WoosmapSettings.searchAPIDistanceFilter = 50;

        // Set Filter on Accuracy of the location
        //WoosmapSettings.accuracyFilter = 10;

        // Instanciate woosmap object
        this.woosmap = Woosmap.getInstance().initializeWoosmap(this);

        // Set Keys
        WoosmapSettings.privateKeySearchAPI = "";
        WoosmapSettings.privateKeyGMPStatic = "";

        this.woosmap.setLocationReadyListener(new WoosLocationReadyListener());
        this.woosmap.setSearchAPIReadyListener (new WoosSearchAPIReadyListener ());
        this.woosmap.setVisitReadyListener (new WoosVisitReadyListener ());

        // Visit Detection Enable
        this.woosmap.setVisitEnable (true);

        // For android version >= 8 you have to create a channel or use the woosmap's channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.woosmap.createWoosmapNotifChannel();
        }
    }

    private void setFragment(Fragment fragment){
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container,fragment);
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
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSIONS_REQUEST_CODE);
            }
        }
    }



    private static class POITask extends AsyncTask<Void, Void, POI> {
        private final Context mContext;
        public MainActivity mActivity;

        POITask(Context context, MainActivity activity){
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected POI doInBackground(Void... voids) {
            POI newPOI = WoosmapDb.getInstance (mContext, true).getPOIsDAO ().getLastPOI ();
            return newPOI;
        }

        @Override
        protected void onPostExecute(POI poiToShow) {
            if (poiToShow == null)
                return;

            SimpleDateFormat displayDateFormat = new SimpleDateFormat("HH:mm:ss");
            if (mActivity.mapFragment.mGoolgeMap != null && mActivity.mapFragment.isVisible ()) {

                LatLng latLng = new LatLng (poiToShow.lat, poiToShow.lng);
                MarkerOptions markerOptions = new MarkerOptions ().position (latLng).title (poiToShow.city).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                if(!mActivity.mapFragment.markersPOI.isEmpty ()) {
                    for (MarkerOptions marker : mActivity.mapFragment.markersPOI) {
                        if (marker.getPosition ().equals (markerOptions.getPosition ())) {
                            return;
                        }
                    }
                }
                mActivity.mapFragment.markersPOI.add (markerOptions);
                mActivity.mapFragment.mGoolgeMap.addMarker (markerOptions);
            }
            if (mActivity.locationFragment.mLocationInfo != null && mActivity.locationFragment.isVisible ()) {
                String poiHTML = mContext.getString(R.string.html_POI, Double.toString (poiToShow.lat),
                        Double.toString (poiToShow.lng), displayDateFormat.format(poiToShow.dateTime),
                        poiToShow.city,poiToShow.zipCode,Double.toString (poiToShow.distance));
                if(mActivity.locationFragment.mLocationInfo.length () != 0)
                    mActivity.locationFragment.mLocationInfo.getEditableText().insert (0,Html.fromHtml(poiHTML) );
                else
                    mActivity.locationFragment.mLocationInfo.append(Html.fromHtml(poiHTML));
            }

        }
    }

    public static class LocationTask extends AsyncTask<Void, Void, MovingPosition> {
        private final Context mContext;
        public MainActivity mActivity;

        LocationTask(Context context, MainActivity activity){
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected  MovingPosition doInBackground(Void... voids) {
            MovingPosition movingPosition = WoosmapDb.getInstance (mContext,true).getMovingPositionsDao ().getLastMovingPosition ();
            return movingPosition;
        }

        @Override
        protected void onPostExecute(MovingPosition movingPosition) {
            if (movingPosition == null)
                return;

            SimpleDateFormat displayDateFormat = new SimpleDateFormat("HH:mm:ss");
            if (mActivity.locationFragment.mLocationInfo != null && mActivity.locationFragment.isVisible ()) {
                String locHTML = mContext.getString(R.string.html_position, Double.toString (movingPosition.lat),
                        Double.toString (movingPosition.lng), displayDateFormat.format(movingPosition.dateTime));
                if(mActivity.locationFragment.mLocationInfo.getText().length () != 0)
                    mActivity.locationFragment.mLocationInfo.getEditableText().insert (0,Html.fromHtml(locHTML) );
                else
                    mActivity.locationFragment.mLocationInfo.append(Html.fromHtml(locHTML));
            }

        }

    }

    public static class AllPOITask extends AsyncTask<Void, Void, POI[]> {
        private final Context mContext;
        public MainActivity mActivity;

        AllPOITask(Context context, MainActivity activity){
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected POI[] doInBackground(Void... voids) {
            POI[] poiList = WoosmapDb.getInstance (mContext, true).getPOIsDAO ().getAllPOIs ();
            return poiList;
        }

        @Override
        protected void onPostExecute(POI[] poiList) {
            SimpleDateFormat displayDateFormat = new SimpleDateFormat("HH:mm:ss");

            for (POI poiToShow : poiList) {
                LatLng latLng = new LatLng (poiToShow.lat,poiToShow.lng);
                MarkerOptions markerOptions = new MarkerOptions ().position (latLng).title (poiToShow.city);
                boolean markerToAdd = false;
                for (MarkerOptions marker : mActivity.mapFragment.markersPOI) {
                    if (!marker.getPosition ().equals (markerOptions.getPosition ())) {
                        markerToAdd = true;
                    }
                }
                if (markerToAdd || mActivity.mapFragment.markersPOI.isEmpty ())
                    mActivity.mapFragment.markersPOI.add (markerOptions);
                String poiHTML = mContext.getString(R.string.html_POI, Double.toString (poiToShow.lat),
                        Double.toString (poiToShow.lng), displayDateFormat.format(poiToShow.dateTime),
                        poiToShow.city,poiToShow.zipCode,Double.toString (poiToShow.distance));
                mActivity.locationFragment.mLocationInfo.append(Html.fromHtml(poiHTML));

            }

        }
    }

    public static class visitTask extends AsyncTask<Void, Void, Visit[]> {
        private final Context mContext;
        public MainActivity mActivity;

        visitTask(Context context, MainActivity activity){
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
            if (mActivity.visitFragment.mVisitInfo != null) {
                mActivity.visitFragment.mVisitInfo.setText ("");
                mActivity.mapFragment.markersVisit.clear ();
            }
            for (Visit visitToShow : staticList) {
                LatLng latLng = new LatLng (visitToShow.lat, visitToShow.lng);
                String startFormatedDate = displayDateFormat.format(visitToShow.startTime);
                String endFormatedDate = "";
                if (visitToShow.endTime == 0) {
                    //Visit in progress
                    endFormatedDate = "ongoing";
                }else {
                    endFormatedDate = displayDateFormat.format (visitToShow.endTime);
                }
                String infoVisites = " --> start: "+startFormatedDate+" / end: "+endFormatedDate+" NbPt : "+ visitToShow.nbPoint;
                MarkerOptions markerOptions = new MarkerOptions ().position (latLng).title (infoVisites);
                boolean markerToUpdate = false;
                for (MarkerOptions marker : mActivity.mapFragment.markersVisit) {
                    if (marker.getPosition ().equals (markerOptions.getPosition ())) {
                        //Update marker
                        markerToUpdate = true;
                        marker.title (markerOptions.getTitle ());
                    }
                }
                if (!markerToUpdate) {
                    mActivity.mapFragment.markersVisit.add (markerOptions);
                    if (mActivity.mapFragment.mGoolgeMap != null)
                        mActivity.mapFragment.mGoolgeMap.addMarker (markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                }

                if (mActivity.visitFragment.mVisitInfo != null) {
                    String visitHTML = mContext.getString (R.string.html_visit, Double.toString (visitToShow.lat),
                            Double.toString (visitToShow.lng), startFormatedDate,
                            endFormatedDate, visitToShow.nbPoint);

                    mActivity.visitFragment.mVisitInfo.append (Html.fromHtml (visitHTML));
                }


            }
        }
    }

    public static class clearDBTask extends AsyncTask<Void, Void, Void> {
        private final Context mContext;
        public MainActivity mActivity;

        clearDBTask(Context context, MainActivity activity){
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            WoosmapDb.getInstance (mContext,true).clearAllTables ();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mActivity.locationFragment.mLocationInfo != null)
                mActivity.locationFragment.mLocationInfo.setText ("");
            if (mActivity.visitFragment.mVisitInfo != null)
                mActivity.visitFragment.mVisitInfo.setText ("");
            mActivity.mapFragment.clearMarkers ();

        }

    }
}
