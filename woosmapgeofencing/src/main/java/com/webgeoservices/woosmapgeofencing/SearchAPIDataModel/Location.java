package com.webgeoservices.woosmapgeofencing.SearchAPIDataModel;

import android.os.Parcel;
import android.os.Parcelable;

/***
 * Represents geographical location of a POI in terms of latitude and longitude
 */
public class Location implements Parcelable {
    private double lat;
    private double lng;

    /***
     * The constructor
     */
    private Location(){ }

    /***
     * The constructor
     * @param lat - latitude of location
     * @param lng - longitude of location
     */
    protected Location(double lat,double lng){
        this.lat = lat;
        this.lng = lng;
    }

    /***
     * Constructor which creates object from parcel
     * @param in
     */
    protected Location(Parcel in) {
        lat = in.readDouble();
        lng = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(lat);
        dest.writeDouble(lng);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Location> CREATOR = new Creator<Location>() {
        @Override
        public Location createFromParcel(Parcel in) {
            return new Location(in);
        }

        @Override
        public Location[] newArray(int size) {
            return new Location[size];
        }
    };

    /***
     * Returns latitude
     * @return Double
     */
    public double getLat() {
        return lat;
    }

    /***
     * Returns longitude
     * @return Double
     */
    public double getLng() {
        return lng;
    }
}
