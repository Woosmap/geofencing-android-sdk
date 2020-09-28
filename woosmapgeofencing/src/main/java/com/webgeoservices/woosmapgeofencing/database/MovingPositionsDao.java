package com.webgeoservices.woosmapgeofencing.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface MovingPositionsDao {

    @Insert
    void createMovingPosition(MovingPosition movingPosition);

    @Update
    void updateMovingPosition(MovingPosition movingPosition);

    @Delete
    void deleteMovingPosition(MovingPosition movingPosition);

    @Query("SELECT * FROM movingpositions ORDER BY dateTime DESC LIMIT 1")
    MovingPosition getLastMovingPosition();

    @Query("SELECT * FROM movingpositions ORDER BY dateTime DESC LIMIT 1,2")
    MovingPosition getPreviousLastMovingPosition();

    @Query("SELECT * FROM movingpositions WHERE isUpload=0 ORDER BY dateTime DESC")
    MovingPosition [] getFailedMovingPosition();

    @Query("DELETE FROM movingpositions")
    void deleteAllMovingPositions();

    @Query("SELECT * FROM movingpositions ORDER BY dateTime LIMIT :limitOfPositions")
    MovingPosition [] getAllMovingPositions(int limitOfPositions);

    @Query("DELETE FROM movingpositions WHERE dateTime <= :date AND isUpload=1")
    void deleteOldPositions(long date);

    @Query("DELETE FROM movingpositions WHERE dateTime <= :dataDurationDelay")
    void deleteMovingOlderThan(long dataDurationDelay);
}