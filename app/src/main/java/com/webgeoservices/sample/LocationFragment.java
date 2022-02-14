package com.webgeoservices.sample;

import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.webgeoservices.sample.model.PlaceData;
import com.webgeoservices.sample.model.PlaceDataAdapter;
import com.webgeoservices.sample.model.PlaceDataComparator;
import com.webgeoservices.woosmapgeofencing.PositionsManager;
import com.webgeoservices.woosmapgeofencing.database.WoosmapDb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LocationFragment extends Fragment {

    PlaceDataAdapter adapter;
    ListView lvLocation;
    PositionsManager mPositionsManager;
    Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPositionsManager = new PositionsManager(getContext(), WoosmapDb.getInstance(getContext()));
        mContext = getContext();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate( R.layout.location, container, false );
        lvLocation = (ListView) view.findViewById(R.id.lvLocation);
        lvLocation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                PlaceData place = (PlaceData) lvLocation.getItemAtPosition(position);
                if (place.getType() == PlaceData.dataType.location) {
                    mPositionsManager.searchAPI( place.getLatitude(), place.getLongitude(), place.getLocationId() );
                } else if (place.getType() == PlaceData.dataType.POI && place.getMovingDuration() == null){
                    List<Pair<Double, Double>> listDestinationPoint = new ArrayList<>();
                    listDestinationPoint.add(new Pair(place.getPOILatitude(), place.getPOILongitude()));
                    // Test Distance API
                    /*listDestinationPoint.add(new Pair(48.709,2.403));
                    listDestinationPoint.add(new Pair(48.841,2.328));
                    listDestinationPoint.add(new Pair(48.823,2.326));
                    listDestinationPoint.add(new Pair(48.768,2.338));
                    listDestinationPoint.add(new Pair(49.123,2.524));
                    listDestinationPoint.add(new Pair(48.789,2.456));
                    listDestinationPoint.add(new Pair(49.987,0.223));*/
                    Double latOrigin = place.getLatitude();
                    Double lngOrigin = place.getLongitude();
                    mPositionsManager.calculateDistance(latOrigin, lngOrigin, listDestinationPoint, place.getLocationId());
                    Map<String, String> param = new HashMap<String, String>();
                    param.put( "modeDistance","walking" );
                    param.put( "distanceUnits","imperial" );
                    param.put( "distanceLanguage","en" );
                    mPositionsManager.calculateDistance(latOrigin, lngOrigin, listDestinationPoint, param, place.getLocationId() );
                    param.put( "provider","WoosmapTraffic" );
                    param.put( "trafficDistanceRouting","balanced" );
                    param.put( "modeDistance","driving" );
                    param.put( "distanceUnits","metric" );
                    param.put( "distanceLanguage","fr" );
                    mPositionsManager.calculateDistance(latOrigin, lngOrigin, listDestinationPoint, param, place.getLocationId() );
                }
            }
        });
        lvLocation.setAdapter(adapter);
        return  view;
    }


    public void loadData(ArrayList<PlaceData> arrayOfPlaceData) {
        int index = lvLocation.getFirstVisiblePosition();
        View v = lvLocation.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - lvLocation.getPaddingTop());

        adapter = new PlaceDataAdapter(mContext, arrayOfPlaceData);
        Collections.sort( arrayOfPlaceData, new PlaceDataComparator());

        lvLocation.setAdapter(adapter);
        lvLocation.setSelectionFromTop(index, top);
    }

    public void clearData() {
        adapter.clear();
    }

}
