package com.webgeoservices.sample.model;

import com.webgeoservices.woosmapgeofencing.database.Region;
import com.webgeoservices.woosmapgeofencing.database.RegionLog;

import java.util.Comparator;

public class PlaceData {

    public enum dataType {
        POI,
        location,
        visit,
        region,
        regionLog,
        ZOI
    }

    private long date;
    private Double latitude;
    private Double longitude;
    private Double POILatitude;
    private Double POILongitude;
    private String city;
    private String travelingDistance;
    private Double distance;
    private String zipCode;
    private dataType type;
    private Double accuracy;
    private long arrivalDate;
    private long departureDate;
    private long duration;
    private String movingDuration;
    private int locationId;
    private String regionIdentifier;
    private boolean didEnter;
    private String idStore;
    private double radius;

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

    public Double getPOILatitude() {
        return POILatitude;
    }

    public void setPOILatitude(Double POILatitude) {
        this.POILatitude = POILatitude;
    }

    public Double getPOILongitude() {
        return POILongitude;
    }

    public void setPOILongitude(Double POILongitude) {
        this.POILongitude = POILongitude;
    }

    public String getTravelingDistance() {
        return travelingDistance;
    }

    public void setTravelingDistance(String travelingDistance) {
        this.travelingDistance = travelingDistance;
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

    public boolean isDidEnter() {
        return didEnter;
    }

    public void setDidEnter(boolean didEnter) {
        this.didEnter = didEnter;
    }

    public String getRegionIdentifier() {
        return regionIdentifier;
    }

    public void setRegionIdentifier(String regionIdentifier) {
        this.regionIdentifier = regionIdentifier;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }


    public String getIdStore() {
        return idStore;
    }

    public void setIdStore(String idStore) {
        this.idStore = idStore;
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
        this.POILatitude = 0.0;
        this.POILongitude = 0.0;
        this.city = "";
        this.distance = 0.0;
        this.zipCode = "";
        this.type = dataType.location;
        this.accuracy = 0.0;
        this.arrivalDate = 0;
        this.departureDate = 0;
        this.duration = 0;
        this.travelingDistance = "";
        this.movingDuration = "";
        this.locationId = 0;
        this.radius = 0.0;
        this.regionIdentifier = "";
        this.didEnter = false;
        this.idStore = "";
    }

    public PlaceData(Region region) {
        this.setType( PlaceData.dataType.region );
        this.setLatitude( region.lat );
        this.setLongitude( region.lng );
        this.setDidEnter( region.didEnter );
        this.setIdStore(region.idStore);
        this.setRegionIdentifier( region.identifier );
        this.setDate( region.dateTime );
        this.setRadius( region.radius );
    }

    public PlaceData(RegionLog regionLog) {
        this.setType( PlaceData.dataType.regionLog );
        this.setLatitude( regionLog.lat );
        this.setLongitude( regionLog.lng );
        this.setDidEnter( regionLog.didEnter );
        this.setIdStore( regionLog.idStore );
        this.setRegionIdentifier( regionLog.identifier );
        this.setDate( regionLog.dateTime );
        this.setRadius( regionLog.radius );
    }

}

