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
import java.util.List;


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
                    Double latOrigin = place.getLatitude();
                    Double lngOrigin = place.getLongitude();
                    mPositionsManager.distanceAPI(latOrigin,lngOrigin,listDestinationPoint,place.getLocationId());
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
