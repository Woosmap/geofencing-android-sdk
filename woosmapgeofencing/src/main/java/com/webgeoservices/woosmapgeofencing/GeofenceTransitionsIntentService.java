package com.webgeoservices.woosmapgeofencing;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.webgeoservices.woosmapgeofencing.database.WoosmapDb;

import java.util.List;

import static com.webgeoservices.woosmapgeofencing.WoosmapSettings.Tags.WoosmapGeofenceTag;

public class GeofenceTransitionsIntentService extends IntentService {

    private static final String TAG = "GeofenceTransitionsIS";

    public GeofenceTransitionsIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final Context context = getApplicationContext();
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            Log.d(WoosmapGeofenceTag, String.valueOf(geofenceTransition));
            Log.d(WoosmapGeofenceTag, triggeringGeofences.toString());

            WoosmapDb db = WoosmapDb.getInstance(context, true);
            PositionsManager positionsManager = new PositionsManager(context, db);

            for (int i = 0; i < geofencingEvent.getTriggeringGeofences().size(); i++) {
                positionsManager.didEventRegion(geofencingEvent.getTriggeringGeofences().get(i).getRequestId(), geofenceTransition );
            }

        }
    }
}