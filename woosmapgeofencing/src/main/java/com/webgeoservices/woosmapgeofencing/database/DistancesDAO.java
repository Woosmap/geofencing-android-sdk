package com.webgeoservices.woosmapgeofencing.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface DistancesDAO {
    @Insert
    long createDistance(Distance distance);

    @Update
    void updateDistance(Distance distance);

    @Delete
    void deleteDistance(Distance distance);

    @Query("SELECT * FROM distances ORDER BY dateTime DESC LIMIT 1")
    Distance getLastDistance();

    @Query("SELECT * FROM distances ORDER BY dateTime DESC LIMIT 1,2")
    Distance getPreviousDistance();

    @Query("DELETE FROM distances")
    void deleteAllDistances();

    @Query("SELECT * FROM distances ORDER BY dateTime LIMIT :limitOfPositions")
    public abstract LiveData<Distance []> getLiveDataDistances(int limitOfPositions);

    @Query("SELECT * FROM distances ORDER BY dateTime LIMIT :limitOfPositions")
    Distance [] getDistances(int limitOfPositions);

    @Query("DELETE FROM distances WHERE dateTime <= :date")
    void deleteOldDistances(long date);

    @Query("DELETE FROM distances WHERE dateTime <= :dataDurationDelay")
    void deleteDistanceOlderThan(long dataDurationDelay);
}
