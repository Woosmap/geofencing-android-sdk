package com.webgeoservices.woosmapgeofencing.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface POIsDAO {

    @Insert
    void createPOI(POI poi);

    @Update
    void updatePOI(POI poi);

    @Delete
    void deletePOI(POI poi);

    @Query("SELECT * FROM POI ORDER BY dateTime DESC LIMIT 1")
    POI getLastPOI();

    @Query("SELECT * FROM POI ORDER BY dateTime DESC LIMIT 1,2")
    POI getPreviousLastPOI();

    @Query("SELECT * FROM POI WHERE isUpload=0 ORDER BY dateTime DESC")
    POI [] getFailedPOI();

    @Query("DELETE FROM POI")
    void deleteAllPOIs();

    @Query("SELECT * FROM POI ORDER BY dateTime DESC LIMIT 50")
    POI [] getAllPOIs();

    @Query("DELETE FROM POI WHERE dateTime <= :date AND isUpload=1")
    void deleteOldPOI(long date);
}


