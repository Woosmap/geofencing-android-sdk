package com.webgeoservices.woosmapGeofencing.SearchAPIDataModel;

public class Feature {
    private String type;
    private Properties properties;
    private Geometry geometry;

    public String getType() { return type; }
    public void setType(String value) { this.type = value; }

    public Properties getProperties() { return properties; }
    public void setProperties(Properties value) { this.properties = value; }

    public Geometry getGeometry() { return geometry; }
    public void setGeometry(Geometry value) { this.geometry = value; }
}
