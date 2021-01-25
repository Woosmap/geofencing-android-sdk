package com.webgeoservices.woosmapgeofencing.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "regionLogs")

public class RegionLog {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int locationId;
    public String identifier;
    public double lat;
    public double lng;
    public double radius;
    public boolean didEnter = false;
    public String idStore = "";
    public long dateTime;
}
