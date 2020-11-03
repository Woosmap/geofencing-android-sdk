package com.webgeoservices.woosmapgeofencing

import android.content.Context
import android.location.Location
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.webgeoservices.woosmapgeofencing.SearchAPIDataModel.SearchAPI
import com.webgeoservices.woosmapgeofencing.WoosmapSettings.Tags.WoosmapVisitsTag
import com.webgeoservices.woosmapgeofencing.database.*
import org.jetbrains.anko.doAsync
import java.util.*
import com.android.volley.RequestQueue


class PositionsManager(val context: Context, private val db: WoosmapDb) {

    private var temporaryCurrentVisits: MutableList<Visit> = mutableListOf<Visit>()
    private var temporaryFinishedVisits: MutableList<Visit> = mutableListOf<Visit>()
    private var requestQueue: RequestQueue? = null


    private fun visitsDetectionAlgo(lastVisit: Visit, location: Location) {
        Log.d(WoosmapVisitsTag, "get New Location")
        val lastVisitLocation = Location("Woosmap")
        lastVisitLocation.latitude = lastVisit.lat
        lastVisitLocation.longitude = lastVisit.lng

        if (location.time < lastVisit.startTime) {
            return
        }

        // Visit is active
        if (lastVisit.endTime.compareTo(0) == 0) {
            val distance = lastVisitLocation.distanceTo(location)
            val accuracy = location.accuracy
            // Test if we are still in visit
            if (distance <= accuracy * 2) {
                //if new position accuracy is better than the visit, we do an Update
                if (lastVisit.accuracy >= location.accuracy) {
                    lastVisit.lat = location.latitude
                    lastVisit.lng = location.longitude
                    lastVisit.accuracy = location.accuracy
                }
                lastVisit.nbPoint += 1
                this.db.visitsDao.updateStaticPosition(lastVisit)
                Log.d(WoosmapVisitsTag, "Always Static")
            }
            //Visit out
            else {
                //Close the current visit
                lastVisit.endTime = location.time
                this.finishVisit(lastVisit)

                val movingPosition = MovingPosition()
                movingPosition.lat = location.latitude
                movingPosition.lng = location.longitude
                movingPosition.accuracy = location.accuracy
                movingPosition.dateTime = location.time
                this.createMovingPositionFromLocation(location)
                Log.d(WoosmapVisitsTag, "Not static Anyway")
            }
        }
        //not visit in progress
        else {
            val previousMovingPosition = this.db.movingPositionsDao.lastMovingPosition
                    ?: this.createMovingPositionFromLocation(location)
            val distance = this.distanceBetweenLocationAndPosition(previousMovingPosition, location)
            Log.d(WoosmapVisitsTag, "distance : $distance")
            if (distance >= WoosmapSettings.distanceDetectionThresholdVisits) {
                this.createMovingPositionFromLocation(location)
                Log.d(WoosmapVisitsTag, "We're Moving")
            } else { //Create a new visit
                val olderPosition = this.db.movingPositionsDao.previousLastMovingPosition
                if (olderPosition != null) {
                    val distanceVisit = this.distanceBetweenLocationAndPosition(olderPosition, location)
                    if (distanceVisit <= WoosmapSettings.distanceDetectionThresholdVisits) {
                        // less than distance of dectection visit of before last position, they are a visit
                        val visit = Visit()
                        visit.uuid = UUID.randomUUID().toString()
                        visit.lat = previousMovingPosition.lat
                        visit.lng = previousMovingPosition.lng
                        visit.accuracy = previousMovingPosition.accuracy
                        visit.startTime = previousMovingPosition.dateTime
                        visit.endTime = 0
                        visit.nbPoint = 1
                        this.createVisit(visit)
                        Log.d(WoosmapVisitsTag, "Create new Visit")
                    } else {
                        //its a static position
                        this.createMovingPositionFromLocation(location)
                    }
                }
            }
        }
    }

    private fun addPositionFromLocation(location: Location) {
        val previousMovingPosition = this.db.movingPositionsDao.lastMovingPosition
                ?: this.createMovingPositionFromLocation(location)
        val distance = this.distanceBetweenLocationAndPosition(previousMovingPosition, location)
        Log.d(WoosmapVisitsTag, "distance : " + distance.toString())
        if (distance >= WoosmapSettings.currentLocationDistanceFilter) {
            this.createMovingPositionFromLocation(location)
        }
    }

    fun createMovingPositionFromLocation(location: Location): MovingPosition {
        val movingPosition = MovingPosition()
        movingPosition.lat = location.latitude
        movingPosition.lng = location.longitude
        movingPosition.accuracy = location.accuracy
        movingPosition.dateTime = location.time
        movingPosition.isUpload = 0
        this.db.movingPositionsDao.createMovingPosition(movingPosition)

        if (filterTimeBetweenRequestSearAPI(movingPosition))
            return movingPosition

        requestSearchAPI(movingPosition)
        return movingPosition
    }

    private fun filterDistanceBetweenRequestSearAPI(newPOI: POI): Boolean {
        if (WoosmapSettings.searchAPIDistanceFilter == 0)
            return false
        // No data in db, No filter
        val previousPOIPosition = this.db.poIsDAO.lastPOI ?: return false

        val locationFromPosition = Location("woosmap")
        locationFromPosition.latitude = newPOI.lat
        locationFromPosition.longitude = newPOI.lng

        val locationToPosition = Location("woosmap")
        locationToPosition.latitude = previousPOIPosition.lat
        locationToPosition.longitude = previousPOIPosition.lng

        // Check time between last position in db and the current position
        if (locationToPosition.distanceTo(locationFromPosition) > WoosmapSettings.searchAPITimeFilter)
            return false
        return true
    }

    private fun filterTimeBetweenRequestSearAPI(movingPosition: MovingPosition): Boolean {
        if (WoosmapSettings.searchAPITimeFilter == 0)
            return false
        // No data in db, No filter
        val previousPOIPosition = this.db.poIsDAO.lastPOI ?: return false

        // Check time between last POI position in db and the current position
        if ((movingPosition.dateTime - previousPOIPosition.dateTime) / 1000 > WoosmapSettings.searchAPITimeFilter)
            return false
        return true
    }

    private fun distanceBetweenLocationAndPosition(position: MovingPosition, location: Location): Float {
        val locationFromPosition = Location("woosmap")
        locationFromPosition.latitude = position.lat
        locationFromPosition.longitude = position.lng
        return location.distanceTo(locationFromPosition)
    }

    private fun timeBetweenLocationAndPosition(position: MovingPosition, location: Location): Long {
        return (location.time - position.dateTime) / 1000
    }

    fun manageLocation(location: Location) {
        Log.d(WoosmapVisitsTag, location.toString())

        //Filter on the accuracy of the location
        if (filterAccurary(location))
            return
        //Filter Time between the last Location and the current Location
        if (filterTimeLocation(location))
            return

        addPositionFromLocation(location)

        if (Woosmap.getInstance().isVisitEnable) {
            val lastVisit = this.db.visitsDao.lastStaticPosition
            if (lastVisit != null) {
                this.visitsDetectionAlgo(lastVisit, location)
            } else {
                Log.d(WoosmapVisitsTag, "Empty")
                val staticLocation = Visit()
                staticLocation.uuid = UUID.randomUUID().toString()
                staticLocation.lat = location.latitude
                staticLocation.lng = location.longitude
                staticLocation.accuracy = location.accuracy
                staticLocation.startTime = location.time
                staticLocation.endTime = 0
                staticLocation.nbPoint = 0
                this.createVisit(staticLocation)
            }
        }
    }

    private fun filterAccurary(location: Location): Boolean {
        // No parameter, No filter
        if (WoosmapSettings.accuracyFilter == 0)
            return false
        if (location.accuracy > WoosmapSettings.accuracyFilter)
            return true
        return false
    }

    public fun asyncManageLocation(locations: List<Location>) {
        doAsync {
            try {
                temporaryCurrentVisits = mutableListOf<Visit>()
                temporaryFinishedVisits = mutableListOf<Visit>()
                for (location in locations.sortedBy { l -> l.time }) {
                    manageLocation(location)
                }

            } catch (e: Exception) {
                Log.e(WoosmapVisitsTag, e.toString())
            }
        }
    }

    fun requestSearchAPI(positon: MovingPosition) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this.context)
        }

        if (WoosmapSettings.searchAPIEnable == false)
            return

        if(WoosmapSettings.privateKeySearchAPI.isEmpty()){
            return
        }

        val url = String.format(WoosmapSettings.Urls.SearchAPIUrl, WoosmapSettings.privateKeySearchAPI, positon.lat, positon.lng)
        val req = StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    Thread {
                        val gson = Gson()
                        val data = gson.fromJson(response, SearchAPI::class.java)
                        val featureSearch = data.features[0]
                        val city = featureSearch.properties.address.city
                        val zipcode = featureSearch.properties.address.zipcode
                        val distance = featureSearch.properties.distance.toString()
                        val longitudePOI = featureSearch.geometry.coordinates[0]
                        val latitudePOI = featureSearch.geometry.coordinates[1]
                        val POIaround = POI()
                        POIaround.city = city
                        POIaround.zipCode = zipcode
                        POIaround.dateTime = positon.dateTime
                        POIaround.distance = distance.toDouble()
                        POIaround.locationId = positon.id
                        POIaround.lat = latitudePOI
                        POIaround.lng = longitudePOI

                        if (!filterDistanceBetweenRequestSearAPI(POIaround)) {
                            this.db.poIsDAO.createPOI(POIaround)
                            if (Woosmap.getInstance().searchAPIReadyListener != null) {
                                Woosmap.getInstance().searchAPIReadyListener.SearchAPIReadyCallback(POIaround)
                            }
                        }


                    }.start()
                },
                Response.ErrorListener { error ->
                    Log.e(WoosmapSettings.Tags.WoosmapSdkTag, error.toString() + " search API")
                })
        requestQueue?.add(req)
    }

    private fun createVisit(visit: Visit) {
        this.db.visitsDao.createStaticPosition(visit)
        temporaryCurrentVisits.add(visit)
    }

    private fun finishVisit(visit: Visit) {
        visit.duration = visit.endTime - visit.startTime;
        this.db.visitsDao.updateStaticPosition(visit)

        if(visit.duration >= WoosmapSettings.durationVisitFilter) {
            // Refresh zoi on Visit
            val figmmForVisitsCreator = FigmmForVisitsCreator(db)
            figmmForVisitsCreator.figmmForVisit(visit)
        }

        temporaryFinishedVisits.add(visit)
        if (Woosmap.getInstance().visitReadyListener != null) {
            Woosmap.getInstance().visitReadyListener.VisitReadyCallback(visit)
        }
    }

    fun cleanOldPositions() {
        Thread {
            val lastStaticPosition = this.db.visitsDao.lastUploadedStaticPosition
            if (lastStaticPosition != null) {
                this.db.movingPositionsDao.deleteOldPositions(lastStaticPosition.startTime)
            }
            this.db.visitsDao.deleteUploadedStaticPositions()
        }.start()
    }

    fun filterTimeLocation(location: Location): Boolean {
        // No parameter, No filter
        if (WoosmapSettings.currentLocationTimeFilter == 0)
            return false

        // No data in db, No filter
        val previousMovingPosition = this.db.movingPositionsDao.lastMovingPosition ?: return false

        // Check time between last position in db and the current position
        if (timeBetweenLocationAndPosition(previousMovingPosition, location) > WoosmapSettings.currentLocationTimeFilter)
            return false
        return true
    }
}