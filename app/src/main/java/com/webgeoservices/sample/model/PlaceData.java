package com.webgeoservices.sample.model;

public class PlaceData {
    public enum dataType {
        POI,
        location,
        visit,
        ZOI
    }

    private long date;
    private Double latitude;
    private Double longitude;
    private String city;
    private Double distance;
    private String zipCode;
    private dataType type;
    private Double accuracy;
    private long arrivalDate;
    private long departureDate;
    private long duration;
    private String movingDuration;
    private int locationId;

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public dataType getType() {
        return type;
    }

    public void setType(dataType type) {
        this.type = type;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public long getArrivalDate() {
        return arrivalDate;
    }

    public void setArrivalDate(long arrivalDate) {
        this.arrivalDate = arrivalDate;
    }

    public long getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(long departureDate) {
        this.departureDate = departureDate;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getMovingDuration() {
        return movingDuration;
    }

    public void setMovingDuration(String movingDuration) {
        this.movingDuration = movingDuration;
    }

    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public PlaceData(long date, Double latitude, Double longitude, String city, Double distance, String zipCode, dataType type, Double accuracy, long arrivalDate, long departureDate, long duration, String movingDuration, int locationId) {
        this.date = date;
        this.latitude = latitude;
        this.longitude = longitude;
        this.city = city;
        this.distance = distance;
        this.zipCode = zipCode;
        this.type = type;
        this.accuracy = accuracy;
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
        this.duration = duration;
        this.movingDuration = movingDuration;
        this.locationId = locationId;
    }

    public PlaceData() {
        this.date = 0;
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.city = "";
        this.distance = 0.0;
        this.zipCode = "";
        this.type = dataType.location;
        this.accuracy = 0.0;
        this.arrivalDate = 0;
        this.departureDate = 0;
        this.duration = 0;
        this.movingDuration = "";
        this.locationId = 0;
    }


}
