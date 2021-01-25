package com.webgeoservices.sample;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.PolyUtil;
import com.webgeoservices.woosmapgeofencing.Woosmap;
import com.webgeoservices.woosmapgeofencing.database.Region;
import com.webgeoservices.woosmapgeofencing.database.ZOI;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    GoogleMap mGoolgeMap;
    MapView mMapView;
    CheckBox locationEnableCheckbox;
    CheckBox POIEnableCheckbox;
    CheckBox ZOIEnableCheckbox;
    CheckBox visitEnableCheckbox;
    List<Marker> locationMarkerList = new ArrayList<>();
    List<Marker> poiMarkerList = new ArrayList<>();
    List<Marker> visitMarkerList = new ArrayList<>();
    List<MarkerOptions> markersLocations = new ArrayList<MarkerOptions>();
    List<MarkerOptions> markersPOI = new ArrayList<MarkerOptions>();
    List<MarkerOptions> markersVisit = new ArrayList<MarkerOptions>();
    List<Polygon> polygonsZOI = new ArrayList<Polygon>();
    List<ZOI> zois = new ArrayList<>();
    List<Circle> circleGeofence = new ArrayList<Circle>();
    List<Region> regions = new ArrayList<>();
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

        locationEnableCheckbox = (CheckBox) view.findViewById(R.id.LocationFilter);
        POIEnableCheckbox = (CheckBox) view.findViewById(R.id.POI);
        visitEnableCheckbox = (CheckBox) view.findViewById(R.id.Visit);
        ZOIEnableCheckbox = (CheckBox) view.findViewById(R.id.ZOI);

        locationEnableCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(locationEnableCheckbox.isChecked()) {
                    for (Marker marker : locationMarkerList) {
                        marker.setVisible(true);
                    }
                } else {
                    for (Marker marker : locationMarkerList) {
                        marker.setVisible(false);
                    }
                }
            }
        });


        POIEnableCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(POIEnableCheckbox.isChecked()) {
                    for (Marker marker : poiMarkerList) {
                        marker.setVisible(true);
                    }
                } else {
                    for (Marker marker : poiMarkerList) {
                        marker.setVisible(false);
                    }
                }
            }
        });

        visitEnableCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(visitEnableCheckbox.isChecked()) {
                    for (Marker marker : visitMarkerList) {
                        marker.setVisible(true);
                    }
                } else {
                    for (Marker marker : visitMarkerList) {
                        marker.setVisible(false);
                    }
                }
            }
        });

        ZOIEnableCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ZOIEnableCheckbox.isChecked()) {
                    for (Polygon polygon : polygonsZOI) {
                        polygon.setVisible(true);
                    }
                } else {
                    for (Polygon polygon : polygonsZOI) {
                        polygon.setVisible(false);
                    }
                }
            }
        });

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

    @SuppressLint("MissingPermission")
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
            mGoolgeMap.setOnMapLongClickListener(this);

            drawPolygon();
            drawCircleGeofence();

            if (!markersLocations.isEmpty()) {
                for (MarkerOptions marker : markersLocations) {
                    locationMarkerList.add(mGoolgeMap.addMarker(marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))));
                }
                if(!locationEnableCheckbox.isChecked()) {
                    for (Marker marker : locationMarkerList) {
                        marker.setVisible(false);
                    }
                }
            }

            if (!markersPOI.isEmpty()) {
                for (MarkerOptions marker : markersPOI) {
                    Marker poiMarker = mGoolgeMap.addMarker(marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                    poiMarkerList.add(poiMarker);
                }
                if(!POIEnableCheckbox.isChecked()) {
                    for (Marker marker : poiMarkerList) {
                        marker.setVisible(false);
                    }
                }

            }

            if (!markersVisit.isEmpty()) {
                for (MarkerOptions marker : markersVisit) {
                    boolean isMarkerInsideZoi = false;
                    ZOI zoiSelected = null;
                    for(Polygon zoiPolygon : polygonsZOI) {
                        isMarkerInsideZoi = PolyUtil.containsLocation(marker.getPosition(), zoiPolygon.getPoints(), true);
                        if(isMarkerInsideZoi) {
                            zoiSelected = (ZOI) zoiPolygon.getTag();
                            break;
                        }
                    }

                    Marker visitMarker = null;
                    if (isMarkerInsideZoi){
                        if(zoiSelected.period.equals("HOME_PERIOD")) {
                            marker.zIndex(1.0F);
                            visitMarker = mGoolgeMap.addMarker(marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                        }else if (zoiSelected.period.equals("WORK_PERIOD")) {
                            marker.zIndex(1.0F);
                            visitMarker = mGoolgeMap.addMarker(marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                        } else {
                            visitMarker = mGoolgeMap.addMarker(marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                        }
                    }else {
                        visitMarker = mGoolgeMap.addMarker(marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                    }
                    if(visitMarker != null) {
                        visitMarkerList.add(visitMarker);
                    }
                }
                if(!visitEnableCheckbox.isChecked()) {
                    for (Marker marker : visitMarkerList) {
                        marker.setVisible(false);
                    }
                }
            }
        }
    }

    public void drawPolygon() {
        if(!ZOIEnableCheckbox.isChecked()) {
            return;
        }
        if (!zois.isEmpty()) {
            for (ZOI zoiPoint : zois) {
                int fillZoiColor = 0x7F00ffff; //transparent cyan
                int strokeZoiColor = Color.BLUE;
                float zindex = 0.0F;

                if(zoiPoint.period.equals("HOME_PERIOD")){
                    fillZoiColor = Color.GREEN;
                    strokeZoiColor = Color.YELLOW;
                    zindex = 1.0F;
                } else if (zoiPoint.period.equals("WORK_PERIOD")){
                    fillZoiColor = Color.RED;
                    strokeZoiColor = Color.YELLOW;
                    zindex = 1.0F;
                }

                Polygon polygon = mGoolgeMap.addPolygon(
                        new PolygonOptions()
                                .add(GetPolygonPoints(zoiPoint.wktPolygon))
                                .strokeWidth(7)
                                .fillColor(fillZoiColor)
                                .strokeColor(strokeZoiColor)
                                .zIndex(zindex)

                );
                polygon.setTag(zoiPoint);

                polygon.setClickable(true);

                mGoolgeMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {
                    public void onPolygonClick(Polygon polygon) {
                        SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                        ZOI zoiSelected = (ZOI) polygon.getTag();
                        String startFormatedDate = displayDateFormat.format(zoiSelected.startTime);
                        String endFormatedDate = displayDateFormat.format(zoiSelected.endTime);

                        long timeSec= zoiSelected.duration/1000;
                        int hours = (int) timeSec/ 3600;
                        int temp = (int) timeSec- hours * 3600;
                        int mins = temp / 60;
                        temp = temp - mins * 60;
                        int secs = temp;

                        String duration = String.format("%02d", hours) + " hours "+ String.format("%02d", mins) +" mins "+ String.format("%02d", secs) +" secs";

                        Toast.makeText(getContext(),  "--> start: " + startFormatedDate + "\n--> end: " + endFormatedDate +
                                "\n" + "Nb visits: " + zoiSelected.idVisits.size() +
                                "\n" + "Duration: " + duration +
                                "\n" + "Qualifier: " + zoiSelected.period, Toast.LENGTH_SHORT).show();
                    }

                });
                polygonsZOI.add(polygon);
            }
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        String id = UUID.randomUUID().toString();
        Woosmap.getInstance().addGeofence( id, latLng, 100);
        addCircle( id, latLng, 100,false );
    }

    public void addCircle(String id, LatLng latLng, float radius, boolean didEnter){
        CircleOptions circleOptions=new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        if (didEnter) {
            circleOptions.strokeColor(Color.argb(255,255,99,71));
            circleOptions.fillColor(Color.argb(64,255,99,71));
        } else {
            circleOptions.strokeColor(Color.argb(255,152,251,152));
            circleOptions.fillColor(Color.argb(64,152,251,152));
        }
        circleOptions.strokeWidth(4);

        Circle circle = mGoolgeMap.addCircle(circleOptions);
        circle.setTag( id );
        circle.setClickable( true );
        circleGeofence.add( circle );
        mGoolgeMap.setOnCircleClickListener( new GoogleMap.OnCircleClickListener() {
            @Override
            public void onCircleClick(Circle circle) {
                Log.d("map","name " + circle.getTag());
                Woosmap.getInstance().removeGeofence( (String) circle.getTag() );
                circle.remove();
                circleGeofence.remove( circle );
            }

        });
    }

    public void drawCircleGeofence() {
        for (Region region : regions) {
           addCircle(region.identifier,new LatLng(region.lng,region.lat), (float) region.radius, region.didEnter) ;
        }
    }

    public LatLng[] GetPolygonPoints(String polygonWkt) {
        List<LatLng> points = new ArrayList<>();
        String sa1,sa2;
        sa1 = polygonWkt.replaceAll("POLYGON","");
        sa2 = sa1.replaceAll("[()]","");
        for ( String point : sa2.split( "," ) )
        {
            String[] latlong =  point.split(" ");
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
        markersLocations.clear();
        markersPOI.clear();
        markersVisit.clear();
        zois.clear();
        circleGeofence.clear();
        regions.clear();
        polygonsZOI.clear();
    }

    public void clearPolygon()  {
        for(Polygon poly : polygonsZOI){
            poly.remove();
        }
        polygonsZOI.clear();
    }

    public void clearCircleGeofence()  {
        for(Circle region : circleGeofence){
            region.remove();
        }
        circleGeofence.clear();
    }
}
