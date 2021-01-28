package com.webgeoservices.woosmapgeofencing.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;

@Entity(tableName = "ZOI")
public class ZOI {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String uuid;
    public ArrayList<String> idVisits = new ArrayList<>();
    public double lngMean;
    public double latMean;
    public double age;
    public double accumulator;
    public double covariance_det;
    public double prior_probability;
    public double x00Covariance_matrix_inverse;
    public double x01Covariance_matrix_inverse;
    public double x10Covariance_matrix_inverse;
    public double x11Covariance_matrix_inverse;
    public String wktPolygon;
    public long startTime;
    public long endTime;
    public long duration;
    public String period;
    public ArrayList<String> weekly_density;

}


