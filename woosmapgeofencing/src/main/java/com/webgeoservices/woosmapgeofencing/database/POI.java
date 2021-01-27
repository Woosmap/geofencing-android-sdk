package com.webgeoservices.woosmapgeofencing.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "POI")

public class POI {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int locationId;
    public double lat;
    public double lng;
    public String name;
    public String idStore;
    public String city;
    public String zipCode;
    public double distance;
    public String travelingDistance;
    public String duration;
    public long dateTime;
    public int isUpload = 0;

}

