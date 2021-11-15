package com.webgeoservices.woosmapgeofencing.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import android.content.Context;
import android.content.SharedPreferences;

import com.webgeoservices.woosmapgeofencing.FigmmForVisitsCreator;
import com.webgeoservices.woosmapgeofencing.WoosmapSettings;

import static android.content.Context.MODE_PRIVATE;

@Database(entities = {Visit.class, MovingPosition.class, POI.class, ZOI.class, Region.class, RegionLog.class, Distance.class}, version = 20, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class WoosmapDb extends RoomDatabase {

    public abstract VisitsDao getVisitsDao();

    public abstract MovingPositionsDao getMovingPositionsDao();

    public abstract POIsDAO getPOIsDAO();

    public abstract ZOIsDAO getZOIsDAO();

    public abstract RegionsPAO getRegionsDAO();

    public abstract RegionLogsPAO getRegionLogsDAO();

    public abstract DistancesDAO getDistanceDAO();

    private static volatile WoosmapDb instance;

    public static synchronized WoosmapDb getInstance(Context context) {
        if (instance == null) {
            instance = create(context);
        }
        return instance;
    }

    private static WoosmapDb create(final Context context) {
        return Room.databaseBuilder(
                context,
                WoosmapDb.class,
                "database-woosmap")
                .fallbackToDestructiveMigration()
                .build();
    }

    public void cleanOldGeographicData(final Context context) {
        SharedPreferences mPrefs = context.getSharedPreferences("WGSGeofencingPref",MODE_PRIVATE);

        long lastUpdate = mPrefs.getLong("lastUpdate", 0);
        if (lastUpdate != 0) {
            long dateNow = System.currentTimeMillis();
            long timeDiffFromNow = dateNow - lastUpdate;
            //update date if no updating since 1 day
            FigmmForVisitsCreator figmmForVisitsCreator = new FigmmForVisitsCreator(WoosmapDb.getInstance(context));
            if (timeDiffFromNow > 86400000) {
                figmmForVisitsCreator.deleteVisitOnZoi(dateNow - WoosmapSettings.dataDurationDelay);
                getVisitsDao().deleteVisitOlderThan(dateNow - WoosmapSettings.dataDurationDelay);
                getMovingPositionsDao().deleteMovingOlderThan(dateNow - WoosmapSettings.dataDurationDelay);
                getPOIsDAO().deletePOIOlderThan(dateNow - WoosmapSettings.dataDurationDelay);
                getDistanceDAO().deleteDistanceOlderThan(dateNow - WoosmapSettings.dataDurationDelay);
                getRegionLogsDAO().deleteRegionLogsOlderThan(dateNow - WoosmapSettings.dataDurationDelay);
                getRegionsDAO().deleteRegionsOlderThan(dateNow - WoosmapSettings.dataDurationDelay);
                //Update date
                mPrefs.edit().putLong("lastUpdate", System.currentTimeMillis()).apply();
            }
        } else {
            //Update date
            mPrefs.edit().putLong("lastUpdate", System.currentTimeMillis()).apply();
        }
    }
}