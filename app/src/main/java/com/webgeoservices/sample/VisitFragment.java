package com.webgeoservices.sample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Comparator;


public class VisitFragment extends Fragment {

    PlaceDataAdapter adapter;
    ListView lvVisit;
    PositionsManager mPositionsManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPositionsManager = new PositionsManager(getContext(), WoosmapDb.getInstance(getContext(), true));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate( R.layout.visit, container, false );
        lvVisit = (ListView) view.findViewById(R.id.lvVisit);

        return  view;
    }


    public void loadData(ArrayList<PlaceData> arrayOfPlaceData) {
        int index = lvVisit.getFirstVisiblePosition();
        View v = lvVisit.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - lvVisit.getPaddingTop());

        adapter = new PlaceDataAdapter(getContext(), arrayOfPlaceData);
        Collections.sort( arrayOfPlaceData, new PlaceDataComparator());

        lvVisit.setAdapter(adapter);
        lvVisit.setSelectionFromTop(index, top);
    }

    public void clearData() {
        adapter.clear();
    }


}
