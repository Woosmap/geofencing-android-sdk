package com.webgeoservices.woosmapGeofencing.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "visits")
public class Visit {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String uuid;
    public double lat;
    public double lng;
    public float accuracy;
    public long startTime;
    public long endTime;
    public int nbPoint;
    public int isUpload = 0; //0 -> not uploaded; 1 -> not finished but uploaded; 2 -> finished and uploaded
}

