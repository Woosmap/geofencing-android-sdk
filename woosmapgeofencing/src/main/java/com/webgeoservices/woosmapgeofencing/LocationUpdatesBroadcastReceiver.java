package com.webgeoservices.woosmapgeofencing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
//import android.os.AsyncTask;

import com.google.android.gms.location.LocationResult;
import com.webgeoservices.woosmapgeofencing.database.WoosmapDb;

import java.util.List;

public class LocationUpdatesBroadcastReceiver extends BroadcastReceiver {

    static final String ACTION_PROCESS_UPDATES =
            "com.google.android.gms.location.sample.locationupdatespendingintent.action" +
                    ".PROCESS_UPDATES";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PROCESS_UPDATES.equals(action)) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    List<Location> locations = result.getLocations();
                    Woosmap woos = Woosmap.getInstance().initializeWoosmapInBackground(context);
                    WoosmapDb db = WoosmapDb.getInstance(context, true);
                    PositionsManager positionsManager = new PositionsManager(context, db);
                    positionsManager.asyncManageLocation(locations);

                }
            }
        }
    }
}
