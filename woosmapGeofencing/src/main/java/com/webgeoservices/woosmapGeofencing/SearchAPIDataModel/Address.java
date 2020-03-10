package com.webgeoservices.woosmapGeofencing.SearchAPIDataModel;

public class Address {
    private Object lines;
    private String countryCode;
    private String city;
    private String zipcode;

    public Object getLines() { return lines; }
    public void setLines(Object value) { this.lines = value; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String value) { this.countryCode = value; }

    public String getCity() { return city; }
    public void setCity(String value) { this.city = value; }

    public String getZipcode() { return zipcode; }
    public void setZipcode(String value) { this.zipcode = value; }
}
