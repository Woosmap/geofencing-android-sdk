package com.webgeoservices.sample

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.webgeoservices.woosmapgeofencing.FigmmForVisitsCreator
import com.webgeoservices.woosmapgeofencing.WoosmapSettings
import com.webgeoservices.woosmapgeofencing.database.MovingPosition
import com.webgeoservices.woosmapgeofencing.database.POI
import com.webgeoservices.woosmapgeofencing.database.Visit
import com.webgeoservices.woosmapgeofencing.database.WoosmapDb
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class DelayDataDurationTest {
    val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SS")
    private lateinit var context: Context

    @Before
    fun setUp() {
        cleanDatabase()
    }

    @Test
    fun cleanDatabase() {
        context =  ApplicationProvider.getApplicationContext<Context>()
        WoosmapDb.getInstance(context).clearAllTables()
    }

    @Test
    fun test_when_clean_old_geographic_data_then_clean_data_older_than_data_duration_delay() {
        WoosmapSettings.numberOfDayDataDuration = 30
        var lastUpdate = formatter.parse("2018-12-24 14:25:03+00").time

        val mPrefs = context.getSharedPreferences("WGSGeofencingPref", Context.MODE_PRIVATE)
        //Update date
        mPrefs.edit().putLong("lastUpdate", lastUpdate).apply()

        val lng = 3.8793329
        val lat = 43.6053862
        val accuracy = 20.0

        val figmmForVisitsCreator = FigmmForVisitsCreator(WoosmapDb.getInstance(context))

        //Calendar set to the current date
        val calendar = Calendar.getInstance()

        for (day in 1..60) {
            val dateCaptured: Long = calendar.timeInMillis

            val visit = Visit()
            visit.uuid = UUID.randomUUID().toString()
            visit.lat = lng
            visit.lng = lat
            visit.startTime = dateCaptured
            visit.endTime = dateCaptured + (24 * 3600) * 1000
            visit.accuracy = accuracy.toFloat()
            visit.duration = visit.endTime - visit.startTime
            WoosmapDb.getInstance(context).visitsDao.createStaticPosition(visit)

            figmmForVisitsCreator.figmmForVisitTest(visit)

            val POIaround = POI()
            POIaround.city = "cityTest"
            POIaround.zipCode = "zipcodeTest"
            POIaround.dateTime = dateCaptured
            POIaround.distance = 10.0
            POIaround.locationId = day
            POIaround.lat = lat
            POIaround.lng = lng
            WoosmapDb.getInstance(context).poIsDAO.createPOI(POIaround)

            val movingPosition = MovingPosition()
            movingPosition.lat = lat
            movingPosition.lng = lng
            movingPosition.accuracy = accuracy.toFloat()
            movingPosition.dateTime = dateCaptured
            WoosmapDb.getInstance(context).movingPositionsDao.createMovingPosition(movingPosition)

            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        figmmForVisitsCreator.update_db()

        WoosmapDb.getInstance(context).cleanOldGeographicData(context)

        Assert.assertEquals( WoosmapDb.getInstance(context).visitsDao.allStaticPositions.size, 30)
        Assert.assertEquals( WoosmapDb.getInstance(context).poIsDAO.allPOIs.size, 30)
        Assert.assertEquals( WoosmapDb.getInstance(context).movingPositionsDao.getMovingPositions(-1).size, 30)

    }

}