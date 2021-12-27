package com.webgeoservices.woosmapgeofencing.database;

import androidx.lifecycle.LiveData;
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

    @Query("SELECT * FROM POI WHERE distance = (SELECT MAX(distance) FROM POI WHERE distance = (SELECT MAX(dateTime) FROM POI)) ")
    POI getLastfurthestPOI();

    @Query("SELECT * FROM POI WHERE locationId = :locId")
    POI getPOIbyLocationID(int locId);

    @Query("SELECT * FROM POI WHERE idStore = :idStore")
    POI getPOIbyStoreId(String idStore);

    @Query("SELECT * FROM POI WHERE locationId = :locId")
    public abstract LiveData<POI> getPOIbyLocationID2(int locId);

    @Query("SELECT * FROM POI ORDER BY dateTime DESC LIMIT 1,2")
    POI getPreviousLastPOI();

    @Query("DELETE FROM POI")
    void deleteAllPOIs();

    @Query("SELECT * FROM POI ORDER BY dateTime")
    POI [] getAllPOIs();

    @Query("DELETE FROM POI WHERE dateTime <= :dataDurationDelay")
    void deletePOIOlderThan(long dataDurationDelay);

    @Query("SELECT * FROM POI ORDER BY dateTime")
    LiveData<POI[]> getAllLivePOIs();
}


