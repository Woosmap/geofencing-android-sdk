package com.webgeoservices.woosmapgeofencing.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface ZOIsDAO {
    @Insert
    void createZoi(ZOI zoi);

    @Insert
    void createAllZoi(ZOI[] zoiList);

    @Update
    void updateZOI(ZOI zoi);

    @Query("SELECT * FROM ZOI")
    ZOI[] getAllZois();

    @Query("SELECT * FROM ZOI")
    public abstract LiveData<ZOI[]> getAllLiveZois();

    @Query("DELETE FROM ZOI")
    void deleteAllZOI();
}