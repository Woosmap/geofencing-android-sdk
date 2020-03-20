package com.webgeoservices.woosmapgeofencing.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "movingpositions")
public class MovingPosition {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public double lat;
    public double lng;
    public float accuracy;
    public float speed = 0;
    public long dateTime;
    public int isUpload = 0;
}

