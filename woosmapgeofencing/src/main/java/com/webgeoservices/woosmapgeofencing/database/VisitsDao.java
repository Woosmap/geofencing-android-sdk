package com.webgeoservices.woosmapgeofencing.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface VisitsDao {

    @Insert
    void createStaticPosition(Visit visit);

    @Update
    void updateStaticPosition(Visit visit);

    @Delete
    void deleteStaticPositions(Visit visit);

    @Query("UPDATE visits SET isUpload=:status WHERE uuid=:uuid")
    void updateUploadStatus(int status, String uuid);

    @Query("SELECT * FROM visits WHERE uuid=:uuid")
    Visit getVisitFromUuid(String uuid);

    @Query("SELECT * FROM visits ORDER BY startTime DESC LIMIT 1")
    Visit getLastStaticPosition();

    @Query("SELECT * FROM visits ORDER BY startTime DESC LIMIT 2")
    Visit get2LastStaticPosition();

    @Query("SELECT * FROM visits WHERE endTime!=0 ORDER BY startTime DESC LIMIT 1")
    Visit getLastClosedStaticPosition();

    @Query("SELECT * FROM visits WHERE isUpload=2 ORDER BY startTime DESC LIMIT 1")
    Visit getLastUploadedStaticPosition();

    @Query("DELETE FROM visits")
    void deleteAllStaticPositions();

    @Query("DELETE FROM visits WHERE isUpload=2")
    void deleteUploadedStaticPositions();

    @Query("SELECT * FROM visits ORDER BY startTime DESC")
    Visit[] getAllStaticPositions();

    @Query("SELECT * FROM visits WHERE isUpload=0 AND endTime=0 ORDER BY startTime DESC LIMIT 1")
    Visit getNotUploadedInProgressStaticPositions();

    @Query("SELECT * FROM visits WHERE isUpload=1 AND endTime != 0 ORDER BY startTime DESC LIMIT 2")
    Visit[] getNotUploadedFinishedStaticPositions();

    @Query("SELECT * FROM visits WHERE isUpload!=2 AND endTime != 0 ORDER BY startTime DESC")
    Visit[] getFailedStaticPositions();

    @Query("SELECT * FROM visits WHERE startTime < :dataDurationDelay")
    Visit[] getVisitOlderThan(long dataDurationDelay);

    @Query("DELETE FROM visits WHERE startTime < :dataDurationDelay")
    void deleteVisitOlderThan(long dataDurationDelay);

}