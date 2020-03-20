package com.webgeoservices.woosmapgeofencing;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import android.util.Log;

import static com.webgeoservices.woosmapgeofencing.WoosmapSettings.Tags.WoosmapSdkTag;

public class WoosmapRebootJobService extends JobIntentService {
    private static final int JOB_ID = 0x01;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WoosmapRebootJobService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d(WoosmapSdkTag, "start activity after reboot");
        Woosmap woosmap = Woosmap.getInstance().initializeWoosmapInBackground(getBaseContext());
        woosmap.onReboot();

    }
}
