package com.webgeoservices.woosmapgeofencing;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class WoosmapMessagingService extends FirebaseMessagingService {

    protected Class<?> cls = null;

    public WoosmapMessagingService() {
        Log.d("WoosMessage", "WoosmapMessagingService");
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("woosmap_mobile_sdk", "onMessageReceived");
        WoosmapMessageBuilderMaps messageBuilderMaps;
        if (this.cls != null) {
            messageBuilderMaps = new WoosmapMessageBuilderMaps(this, this.cls);
        } else {
            messageBuilderMaps = new WoosmapMessageBuilderMaps(this);
        }
        WoosmapMessageDatas messageDatas = new WoosmapMessageDatas(remoteMessage.getData());
        if (messageDatas.isLocationRequest() && messageDatas.timestamp != null) {
            messageBuilderMaps.sendWoosmapNotification(messageDatas);
        }

    }

}