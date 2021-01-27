package com.webgeoservices.woosmapgeofencing.SearchAPIDataModel;

public class Properties {
    private String store_id;
    private String name;
    private Object contact;
    private Address address;
    private UserProperties userProperties;
    private Object[] tags;
    private Object[] types;
    private Double distance;

    public String getStoreID() {
        return store_id;
    }

    public void setStoreID(String value) {
        this.store_id = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public Object getContact() {
        return contact;
    }

    public void setContact(Object value) {
        this.contact = value;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address value) {
        this.address = value;
    }

    public UserProperties getUserProperties() {
        return userProperties;
    }

    public void setUserProperties(UserProperties value) {
        this.userProperties = value;
    }

    public Object[] getTags() {
        return tags;
    }

    public void setTags(Object[] value) {
        this.tags = value;
    }

    public Object[] getTypes() {
        return types;
    }

    public void setTypes(Object[] value) {
        this.types = value;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double value) {
        this.distance = value;
    }
}
