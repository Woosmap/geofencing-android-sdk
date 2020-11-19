package com.webgeoservices.sample.model;

import java.util.Comparator;

public class PlaceDataComparator implements Comparator<PlaceData> {

    @Override
    public int compare(PlaceData o1, PlaceData o2) {
        return Long.compare(o2.getDate(), o1.getDate());
    }
}
