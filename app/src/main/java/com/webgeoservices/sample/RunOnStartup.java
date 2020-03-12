package com.webgeoservices.sample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.webgeoservices.woosmapgeofencing.WoosmapRebootJobService;

public class RunOnStartup extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            WoosmapRebootJobService.enqueueWork(context, new Intent());
        }
    }
}
