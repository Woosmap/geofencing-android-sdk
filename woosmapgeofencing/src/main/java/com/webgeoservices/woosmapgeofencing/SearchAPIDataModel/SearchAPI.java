package com.webgeoservices.woosmapgeofencing.SearchAPIDataModel;

// searchAPI.java
public class SearchAPI {
    private String type;
    private Feature[] features;
    private Pagination pagination;

    public String getType() {
        return type;
    }

    public void setType(String value) {
        this.type = value;
    }

    public Feature[] getFeatures() {
        return features;
    }

    public void setFeatures(Feature[] value) {
        this.features = value;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination value) {
        this.pagination = value;
    }
}
