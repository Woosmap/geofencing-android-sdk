package com.webgeoservices.woosmapgeofencing;

import android.os.Bundle;

import java.util.Map;

public class WoosmapMessageDatas {
    public String messageBody;
    public String title;
    public String type;
    public String longText;
    public String imageUrl;
    public String notificationId;
    private String notifFromWoosmap;
    public String locationRequest;
    public String open_uri;
    public String icon_url;
    public String timestamp;

    public WoosmapMessageDatas(Map<String, String> data) {
        this.messageBody = data.get("body");
        this.title = data.get("title");
        this.type = data.get("type");
        this.longText = data.get("long_text");
        this.imageUrl = data.get("image_url");
        this.notificationId = data.get("notification_id");
        this.notifFromWoosmap = data.get("notif_from_woosmap");
        this.locationRequest = data.get("location");
        this.open_uri = data.get("open_uri");
        this.icon_url = data.get("icon_url");
        this.timestamp = data.get("timestamp");
    }

    public WoosmapMessageDatas(Bundle data) {
        this.messageBody = data.getString("body");
        this.title = data.getString("title");
        this.type = data.getString("type");
        this.longText = data.getString("long_text");
        this.imageUrl = data.getString("image_url");
        this.notificationId = data.getString("notification_id");
        this.notifFromWoosmap = data.getString("notif_from_woosmap");
        this.locationRequest = data.getString("location");
        this.open_uri = data.getString("open_uri");
        this.icon_url = data.getString("icon_url");
        this.timestamp = data.getString("timestamp");
    }

    public boolean isFromWoosmap() {
        return this.notifFromWoosmap != null;
    }

    public boolean isLocationRequest() {
        return this.locationRequest != null;
    }
}
