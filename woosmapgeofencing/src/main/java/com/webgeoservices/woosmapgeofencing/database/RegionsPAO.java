package com.webgeoservices.woosmapgeofencing.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface RegionsPAO {
    @Insert
    void createRegion(Region region);

    @Update
    void updateRegion(Region region);

    @Delete
    void deleteRegion(Region region);

    @Query("DELETE FROM regions WHERE identifier = :id")
    void deleteRegionFromId(String id);

    @Query("SELECT * FROM regions WHERE identifier LIKE '%POI_%'")
    Region [] getRegionPOI();

    @Query("SELECT * FROM regions ORDER BY dateTime DESC LIMIT 1")
    Region getLastRegion();

    @Query("SELECT * FROM regions ORDER BY dateTime DESC LIMIT 1,2")
    Region getPreviousLastRegion();

    @Query("SELECT * FROM regions WHERE identifier = :identifier")
    Region getRegionFromId(String identifier);

    @Query("DELETE FROM regions")
    void deleteAllRegions();

    @Query("SELECT * FROM regions ORDER BY dateTime")
    Region [] getAllRegions();

    @Query("DELETE FROM regions WHERE dateTime <= :dataDurationDelay")
    void deleteRegionsOlderThan(long dataDurationDelay);
}
