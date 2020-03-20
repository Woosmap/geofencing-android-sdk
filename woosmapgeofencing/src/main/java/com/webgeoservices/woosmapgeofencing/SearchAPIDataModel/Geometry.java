package com.webgeoservices.woosmapgeofencing.SearchAPIDataModel;

public class Geometry {
    private String type;
    private double[] coordinates;

    public String getType() {
        return type;
    }

    public void setType(String value) {
        this.type = value;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(double[] value) {
        this.coordinates = value;
    }
}
