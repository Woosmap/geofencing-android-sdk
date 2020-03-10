package com.webgeoservices.woosmapGeofencing

import android.content.Context
import android.location.Location
import androidx.test.runner.AndroidJUnit4
import androidx.test.InstrumentationRegistry
import android.util.Log
import com.webgeoservices.woosmapGeofencing.database.WoosmapDb
import org.hamcrest.core.IsEqual.equalTo
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class visitTest {
    private lateinit var db: WoosmapDb
    private lateinit var context: Context
    private var pointsList: MutableList<Location> = mutableListOf()

    fun populatePointsList(){
        val inputStream = InstrumentationRegistry.getContext().resources.assets.open(
                "home_work.csv"
        )
        val inputString = inputStream.bufferedReader().useLines {
            lines -> lines.forEach {
                val values = it.split(";")
                val location = Location("woosmap")
                location.latitude = values.elementAt(0).toDouble()
                location.longitude = values.elementAt(1).toDouble()
                location.accuracy = values.elementAt(2).toFloat()
                location.time = values.elementAt(3).toLong()
                pointsList.add(location)
            }
        }
    }

    @Before
    fun createDb() {
        context = InstrumentationRegistry.getTargetContext()
        db = WoosmapDb.getInstance(context, false)
        this.populatePointsList()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        //db.close()
    }

    @Test
    fun firstMovingPositions(){
        for (location in pointsList.sortedBy { l-> l.time }){
            Thread {
                Thread.sleep(800)
                val tmpPositionManager = PositionsManager(context, db)
                tmpPositionManager.manageLocation(location)
            }.run()
        }
        val positions = db.movingPositionsDao.allMovingPositions
        Log.e("debug", positions.toString())
        val visits = db.visitsDao.allStaticPositions
        assertThat(visits.size, equalTo(2))
    }
}