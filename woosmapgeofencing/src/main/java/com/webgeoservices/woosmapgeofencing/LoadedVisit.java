package com.webgeoservices.woosmapgeofencing;

public class LoadedVisit {

    private double x;
    private double y;
    private double accuracy;
    private String id;
    public long startime;
    public long endtime;


    public LoadedVisit(double x, double y, String id) {
        this.x = x;
        this.y = y;
        this.id = id;
        this.accuracy = 20.0;
        this.startime = 0;
        this.endtime = 0;
    }

    public LoadedVisit(double x, double y, double accuracy, String id) {
        this.x = x;
        this.y = y;
        this.id = id;
        this.accuracy = accuracy;
        this.startime = 0;
        this.endtime = 0;
    }

    public LoadedVisit(double x, double y, double accuracy, String id, long startime, long endtime) {
        this.x = x;
        this.y = y;
        this.id = id;
        this.accuracy = accuracy;
        this.startime = startime;
        this.endtime = endtime;
    }

    public LoadedVisit() {
        this.x = 0;
        this.y = 0;
        this.id = "1";
        this.accuracy = 20.0;
        this.startime = 0;
        this.endtime = 0;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAccuray() {
        return accuracy;
    }

    public String getId() {
        return id;
    }

}
