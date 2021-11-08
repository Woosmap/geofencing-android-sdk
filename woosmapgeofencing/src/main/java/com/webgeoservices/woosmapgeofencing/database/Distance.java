package com.webgeoservices.woosmapgeofencing.database;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "distances")
public class Distance {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public long dateTime;
    public double originLatitude;
    public double originLongitude;
    public double destinationLatitude;
    public double destinationLongitude;
    public int distance;
    public String distanceText;
    public int duration;
    public String durationText;
    public String mode;
    public String units;
    public String routing;
    public String language;
    public int locationId;

}
