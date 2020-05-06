package com.webgeoservices.sample;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.webgeoservices.woosmapgeofencing.database.ZOI;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class MapFragment extends Fragment implements OnMapReadyCallback {

    GoogleMap mGoolgeMap;
    MapView mMapView;
    List<MarkerOptions> markersPOI = new ArrayList<MarkerOptions>();
    List<MarkerOptions> markersVisit = new ArrayList<MarkerOptions>();
    List<Polygon> polygonZOI = new ArrayList<Polygon>();
    List<ZOI> zois = new ArrayList<> ();


    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    private static final int REQUEST_CODE = 101;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.map, container, false);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
        fetchLastLocation();


        return view;
    }

    private void fetchLastLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;
                }
            }
        });
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
        fetchLastLocation();
        mMapView = view.findViewById(R.id.map);
        if (mMapView != null) {
            mMapView.onCreate(null);
            mMapView.onResume();
            mMapView.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        if (currentLocation != null && isVisible()) {
            LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            MapsInitializer.initialize(getActivity());
            mGoolgeMap = googleMap;
            mGoolgeMap.setMyLocationEnabled(true);
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));

            if (!markersPOI.isEmpty()) {
                for (MarkerOptions marker : markersPOI) {
                    mGoolgeMap.addMarker(marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                }
            }

            if (!markersVisit.isEmpty()) {
                for (MarkerOptions marker : markersVisit) {
                    mGoolgeMap.addMarker(marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                }
            }

            drawPolygon();

        }

    }

    public void drawPolygon() {
        if (!zois.isEmpty()) {
            for (ZOI zoiPoint : zois) {
                Polygon polygon =  mGoolgeMap.addPolygon(
                        new PolygonOptions()
                                .add(GetPolygonPoints(zoiPoint.wktPolygon))
                                .strokeWidth(7)
                                .fillColor(Color.CYAN)
                                .strokeColor(Color.BLUE)

                );

                polygon.setTag(zoiPoint);

                polygon.setClickable(true);

                mGoolgeMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {
                    public void onPolygonClick(Polygon polygon) {
                        SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                        ZOI zoiSelected = (ZOI) polygon.getTag();
                        String startFormatedDate = displayDateFormat.format(zoiSelected.startTime);
                        String endFormatedDate = displayDateFormat.format(zoiSelected.endTime);
                        DateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.US);
                        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                        String duration = formatter.format(new Date(zoiSelected.duration));

                        Toast.makeText(getContext(),  "--> start: " + startFormatedDate + "\n--> end: " + endFormatedDate +
                                "\n" + "Nb visits: " + zoiSelected.idVisits.size() +
                                "\n" + "Duration: " + duration, Toast.LENGTH_LONG).show();
                    }

                });
                polygonZOI.add(polygon);
            }
        }
    }



    public LatLng[] GetPolygonPoints(String polygonWkt) {
        List<LatLng> points = new ArrayList<>();

        String sa1,sa2,sa3,sa4;

        sa1 = polygonWkt.replaceAll("POLYGON","");
        sa2 = sa1.replaceAll("[()]","");
        sa3 = sa2.replaceAll(",","#");
        sa4 = sa3.replaceAll(" ",",");

        String[] splitString = sa4.split( "#" );

        for ( String point : splitString )
        {
            String[] latlong =  point.split(",");
            double latitude = Double.parseDouble(latlong[1]);
            double longitude = Double.parseDouble(latlong[0]);
            points.add(new LatLng(latitude,longitude));
        }

        return points.toArray(new LatLng[points.size()]);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fetchLastLocation();
                }
                break;
        }
    }

    public void clearMarkers() {
        if (mGoolgeMap != null)
            mGoolgeMap.clear();
        markersPOI.clear();
        markersVisit.clear();
        zois.clear();
    }

    public void clearPolygon()  {
        for(Polygon poly : polygonZOI){
            poly.remove();
        }
    }
}
