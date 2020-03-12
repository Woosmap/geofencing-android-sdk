package com.webgeoservices.woosmapgeofencing.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import android.content.Context;

@Database(entities = {Visit.class, MovingPosition.class, POI.class}, version = 4, exportSchema = false)
public abstract class WoosmapDb extends RoomDatabase {
    public abstract VisitsDao getVisitsDao();

    public abstract MovingPositionsDao getMovingPositionsDao();

    public abstract POIsDAO getPOIsDAO();

    private static volatile WoosmapDb instance;

    public static synchronized WoosmapDb getInstance(Context context, Boolean isProd) {
        if (instance == null) {
            if (!isProd) {
                instance = testCreate(context);
            } else {
                instance = create(context);
            }
        }
        return instance;
    }

    public static synchronized WoosmapDb getDevelopInstance(Context context) {
        return developCreate(context);
    }

    private static WoosmapDb create(final Context context) {
        return Room.databaseBuilder(
                context,
                WoosmapDb.class,
                "database-woosmap")
                .fallbackToDestructiveMigration()
                .build();
    }

    private static WoosmapDb developCreate(final Context context) {
        return Room.databaseBuilder(
                context,
                WoosmapDb.class,
                "database-woosmap")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
    }

    private static WoosmapDb testCreate(final Context context) {
        return Room.inMemoryDatabaseBuilder(
                context,
                WoosmapDb.class)
                .build();
    }
}