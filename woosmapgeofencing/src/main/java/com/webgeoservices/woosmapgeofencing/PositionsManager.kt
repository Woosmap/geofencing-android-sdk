package com.webgeoservices.woosmapgeofencing

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.util.Log
import android.util.Pair
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.webgeoservices.woosmapgeofencing.DistanceAPIDataModel.DistanceAPI
import com.webgeoservices.woosmapgeofencing.SearchAPIDataModel.SearchAPI
import com.webgeoservices.woosmapgeofencing.WoosmapSettings.*
import com.webgeoservices.woosmapgeofencing.WoosmapSettings.Tags.WoosmapVisitsTag
import com.webgeoservices.woosmapgeofencing.database.*
import org.jetbrains.anko.doAsync
import java.util.*


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
                Log.d(WoosmapVisitsTag, "Not static Anyway")
            }
        }
        //not visit in progress
        else {
            val previousMovingPosition = this.db.movingPositionsDao.lastMovingPosition
            var distance = 0.0F;
            if (previousMovingPosition != null) {
                distance = this.distanceBetweenLocationAndPosition(previousMovingPosition, location)
            }
            Log.d(WoosmapVisitsTag, "distance : $distance")
            if (distance >= WoosmapSettings.distanceDetectionThresholdVisits) {
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
        val previousMovingPosition = this.db.movingPositionsDao.lastMovingPosition
                ?: null
        var distance = 1.0F
        if (previousMovingPosition != null) {
            distance = this.distanceBetweenLocationAndPosition(previousMovingPosition, location)
        }

        val movingPosition = MovingPosition()
        movingPosition.lat = location.latitude
        movingPosition.lng = location.longitude
        movingPosition.accuracy = location.accuracy
        movingPosition.dateTime = location.time
        movingPosition.isUpload = 0

        val id = this.db.movingPositionsDao.createMovingPosition(movingPosition)
        movingPosition.id = id.toInt()

        if (filterTimeBetweenRequestSearAPI(movingPosition))
            return movingPosition

        if (WoosmapSettings.searchAPIEnable == true){
            if (distance > 0.0)
                requestSearchAPI(movingPosition)
        }


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

        if (WoosmapSettings.visitEnable) {
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

    fun asyncManageLocation(locations: List<Location>) {
        doAsync {
            try {
                temporaryCurrentVisits = mutableListOf<Visit>()
                temporaryFinishedVisits = mutableListOf<Visit>()
                for (location in locations.sortedBy { l -> l.time }) {
                    manageLocation(location)
                }

                detectVisitInZOIClassified()

            } catch (e: Exception) {
                Log.e(WoosmapVisitsTag, e.toString())
            }
        }
    }

    private fun detectVisitInZOIClassified() {
        if( temporaryCurrentVisits.isEmpty() && temporaryFinishedVisits.isEmpty()) {
            return
        }

        val ZOIsClassified = this.db.zoIsDAO.getWorkHomeZOI()

        val lastVisitLocation = Location("lastVisit")
        var didEnter = false

        if( !temporaryCurrentVisits.isEmpty()) {
            lastVisitLocation.latitude =  temporaryCurrentVisits.first().lat
            lastVisitLocation.longitude =  temporaryCurrentVisits.first().lng
            didEnter = true
        }

        if( !temporaryFinishedVisits.isEmpty()) {
            lastVisitLocation.latitude =  temporaryFinishedVisits.first().lat
            lastVisitLocation.longitude =  temporaryFinishedVisits.first().lng
            didEnter = false
        }

        for (zoi in ZOIsClassified) {
            val zoiCenterLocation = Location("zoiCenter")
            zoiCenterLocation.latitude = SphericalMercator.y2lat(zoi.lngMean)
            zoiCenterLocation.longitude = SphericalMercator.x2lon(zoi.latMean)
            val distance = zoiCenterLocation.distanceTo(lastVisitLocation)
            if(distance < radiusDetectionClassifiedZOI) {
                var regionLog = RegionLog()
                regionLog.identifier = zoi.period
                regionLog.dateTime = System.currentTimeMillis()
                regionLog.didEnter = didEnter
                regionLog.lat = lastVisitLocation.latitude
                regionLog.lng = lastVisitLocation.longitude
                regionLog.radius = radiusDetectionClassifiedZOI.toDouble()
                this.db.regionLogsDAO.createRegionLog(regionLog)

                if (Woosmap.getInstance().regionLogReadyListener != null) {
                    Woosmap.getInstance().regionLogReadyListener.RegionLogReadyCallback(regionLog)
                }
            }
        }

    }

    fun requestSearchAPI(positon: MovingPosition) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this.context)
        }

        if(WoosmapSettings.privateKeyWoosmapAPI.isEmpty()){
            return
        }

        val url = String.format(WoosmapSettings.Urls.SearchAPIUrl, WoosmapSettings.Urls.WoosmapURL, WoosmapSettings.privateKeyWoosmapAPI, positon.lat, positon.lng)
        val req = StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    Thread {
                        val gson = Gson()
                        val data = gson.fromJson(response, SearchAPI::class.java)
                        val featureSearch = data.features[0]
                        val name = featureSearch.properties.name
                        val city = featureSearch.properties.address.city
                        val zipcode = featureSearch.properties.address.zipcode
                        val idStore = featureSearch.properties.storeID
                        val distance = featureSearch.properties.distance.toString()
                        val longitudePOI = featureSearch.geometry.coordinates[0]
                        val latitudePOI = featureSearch.geometry.coordinates[1]
                        val POIaround = POI()
                        POIaround.city = city
                        POIaround.zipCode = zipcode
                        POIaround.dateTime = positon.dateTime
                        POIaround.distance = distance.toDouble()
                        POIaround.locationId = positon.id
                        POIaround.idStore = idStore
                        POIaround.name = name
                        POIaround.lat = latitudePOI
                        POIaround.lng = longitudePOI
                        POIaround.data = response

                        createPOIRegion("POI_" + featureSearch.properties.name, latitudePOI, longitudePOI, idStore )

                        if (!filterDistanceBetweenRequestSearAPI(POIaround)) {
                            if(WoosmapSettings.distanceAPIEnable) {
                                requestDistanceAPI(POIaround,positon)
                            }else {
                                this.db.poIsDAO.createPOI(POIaround)
                                if (Woosmap.getInstance().searchAPIReadyListener != null) {
                                    Woosmap.getInstance().searchAPIReadyListener.SearchAPIReadyCallback(POIaround)
                                }
                            }
                        }

                    }.start()
                },
                Response.ErrorListener { error ->
                    Log.e(WoosmapSettings.Tags.WoosmapSdkTag, error.toString() + " search API")
                })
        requestQueue?.add(req)
    }

    fun requestDistanceAPI(POIaround: POI, positon: MovingPosition) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this.context)
        }

        if(WoosmapSettings.privateKeyWoosmapAPI.isEmpty()){
            return
        }

        val destination = POIaround.lat.toString() + "," + POIaround.lng.toString()

        val url = String.format(WoosmapSettings.Urls.DistanceAPIUrl, WoosmapSettings.Urls.WoosmapURL, WoosmapSettings.modeDistance, positon.lat, positon.lng, destination, WoosmapSettings.privateKeyWoosmapAPI)
        val req = StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    Thread {
                        val gson = Gson()
                        val data = gson.fromJson(response, DistanceAPI::class.java)
                        val status = data.status

                        if(status == "OK"){
                            if(data.rows.get(0).elements.get(0).status == "OK" ) {
                                POIaround.travelingDistance = data.rows.get(0).elements.get(0).distance.text
                                POIaround.duration = data.rows[0].elements[0].duration.text
                            }
                        }

                        this.db.poIsDAO.createPOI(POIaround)
                        if (Woosmap.getInstance().searchAPIReadyListener != null) {
                            Woosmap.getInstance().searchAPIReadyListener.SearchAPIReadyCallback(POIaround)
                        }

                    }.start()
                },
                Response.ErrorListener { error ->
                    Log.e(WoosmapSettings.Tags.WoosmapSdkTag, error.toString() + " Distance API")
                })
        requestQueue?.add(req)
    }

    fun searchAPI(lat: Double, lng: Double, positionId: Int = 0) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this.context)
        }

        if(WoosmapSettings.privateKeyWoosmapAPI.isEmpty()){
            return
        }

        val url = String.format(WoosmapSettings.Urls.SearchAPIUrl, WoosmapSettings.Urls.WoosmapURL, WoosmapSettings.privateKeyWoosmapAPI, lat, lng)
        val req = StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    Thread {
                        val gson = Gson()
                        val data = gson.fromJson(response, SearchAPI::class.java)
                        val featureSearch = data.features[0]
                        val name = featureSearch.properties.name
                        val idStore = featureSearch.properties.storeID
                        val city = featureSearch.properties.address.city
                        val zipcode = featureSearch.properties.address.zipcode
                        val distance = featureSearch.properties.distance.toString()
                        val longitudePOI = featureSearch.geometry.coordinates[0]
                        val latitudePOI = featureSearch.geometry.coordinates[1]
                        val POIaround = POI()
                        POIaround.city = city
                        POIaround.name = name
                        POIaround.idStore = idStore
                        POIaround.zipCode = zipcode
                        POIaround.distance = distance.toDouble()
                        POIaround.dateTime = System.currentTimeMillis()
                        POIaround.lat = latitudePOI
                        POIaround.lng = longitudePOI
                        POIaround.locationId = positionId
                        POIaround.data = response

                        this.db.poIsDAO.createPOI(POIaround)
                        if (Woosmap.getInstance().searchAPIReadyListener != null) {
                            Woosmap.getInstance().searchAPIReadyListener.SearchAPIReadyCallback(POIaround)
                        }

                    }.start()
                },
                Response.ErrorListener { error ->
                    Log.e(WoosmapSettings.Tags.WoosmapSdkTag, error.toString() + " search API")
                })
        requestQueue?.add(req)
    }

    private fun createPOIRegion(POIid: String, latitudePOI: Double, longitudePOI: Double, POIidStore: String) {

        Thread {

            var regionsPOI = this.db.regionsDAO.regionPOI
            var regionExist = false

            regionsPOI.forEach {
                if(!it.identifier.contains(POIid)) {
                    //Remove last POI geofence in geofencing manager
                    Woosmap.getInstance().removeGeofence(it.identifier)
                    //Remove last POI geofence in db
                    this.db.regionsDAO.deleteRegionFromId(it.identifier)
                    regionExist = false
                }else {
                    regionExist = true
                }
            }

            if (!regionExist) {
                Woosmap.getInstance().addGeofence(POIid + "_" + firstSearchAPIRegionRadius, LatLng(latitudePOI, longitudePOI), firstSearchAPIRegionRadius.toFloat(), POIidStore)
                Woosmap.getInstance().addGeofence(POIid + "_" + secondSearchAPIRegionRadius, LatLng(latitudePOI, longitudePOI), secondSearchAPIRegionRadius.toFloat(), POIidStore)
                Woosmap.getInstance().addGeofence(POIid + "_" + thirdSearchAPIRegionRadius, LatLng(latitudePOI, longitudePOI), thirdSearchAPIRegionRadius.toFloat(), POIidStore)
            }

        }.start()

    }


    fun distanceAPI(latOrigin: Double, lngOrigin: Double, listPosition: MutableList<Pair<Double, Double>>, locationId: Int = 0) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this.context)
        }

        if(WoosmapSettings.privateKeyWoosmapAPI.isEmpty()){
            return
        }

        var destination = ""
        listPosition.forEach {
            destination += it.first.toString() + "," + it.second.toString() + "|"
        }

        val url = String.format(WoosmapSettings.Urls.DistanceAPIUrl, WoosmapSettings.Urls.WoosmapURL, WoosmapSettings.modeDistance, latOrigin, lngOrigin, destination, WoosmapSettings.privateKeyWoosmapAPI)
        val req = StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    Thread {
                        val gson = Gson()
                        val data = gson.fromJson(response, DistanceAPI::class.java)
                        val status = data.status
                        if(locationId != 0 && status.contains("OK") && data.rows.get(0).elements.get(0).status.contains("OK")) {
                            var poiToUpdate = this.db.poIsDAO.getPOIbyLocationID(locationId)
                            poiToUpdate.travelingDistance = data.rows.get(0).elements.get(0).distance.text
                            poiToUpdate.duration = data.rows.get(0).elements.get(0).duration.text
                            this.db.poIsDAO.updatePOI(poiToUpdate)
                        }

                        if (Woosmap.getInstance().distanceAPIReadyListener != null) {
                            Woosmap.getInstance().distanceAPIReadyListener.DistanceAPIReadyCallback(data)
                        }

                    }.start()
                },
                Response.ErrorListener { error ->
                    Log.e(WoosmapSettings.Tags.WoosmapSdkTag, error.toString() + " Distance API")
                })
        requestQueue?.add(req)
    }

    private fun createVisit(visit: Visit) {
        this.db.visitsDao.createStaticPosition(visit)
        temporaryCurrentVisits.add(visit)
    }

    private fun finishVisit(visit: Visit) {
        visit.duration = visit.endTime - visit.startTime
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


    fun createRegion(identifier: String, radius: Double, lat: Double, lng: Double, idStore: String )  {
        var region = Region()
        region.lat = lat
        region.lng = lng
        region.identifier = identifier
        region.idStore = idStore
        region.radius = radius
        region.dateTime = System.currentTimeMillis()

        Thread {
            this.db.regionsDAO.createRegion(region)

            if (Woosmap.getInstance().regionReadyListener != null) {
                Woosmap.getInstance().regionReadyListener.RegionReadyCallback(region)
            }
        }.start()

    }

    fun didEventRegion(geofenceIdentifier: String, transition: Int) {
        Thread {
            val regionDetected = this.db.regionsDAO.getRegionFromId(geofenceIdentifier) ?: return@Thread

            if(transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                regionDetected.didEnter = true
            } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                regionDetected.didEnter = false
            }

            regionDetected.dateTime = System.currentTimeMillis()
            this.db.regionsDAO.updateRegion(regionDetected)

            var regionLog = RegionLog()
            regionLog.identifier = regionDetected.identifier
            regionLog.dateTime = regionDetected.dateTime
            regionLog.didEnter = regionDetected.didEnter
            regionLog.lat = regionDetected.lat
            regionLog.lng = regionDetected.lng
            regionLog.idStore = regionDetected.idStore
            regionLog.radius =regionDetected.radius
            this.db.regionLogsDAO.createRegionLog(regionLog)

            if (Woosmap.getInstance().regionLogReadyListener != null) {
                Woosmap.getInstance().regionLogReadyListener.RegionLogReadyCallback(regionLog)
            }
        }.start()
    }

    fun removeGeofence(id: String) {
        Thread {
            this.db.regionsDAO.deleteRegionFromId(id)
        }.start()

    }


    @SuppressLint("MissingPermission")
    fun addGeofence(geofenceHelper: GeofenceHelper, geofencingRequest: GeofencingRequest, geofencePendingIntent: PendingIntent, geofencingClient: GeofencingClient, id: String, radius: Float, latitude: Double, longitude: Double, idStore: String) {
        Thread {
            val region = this.db.regionsDAO.getRegionFromId(id)
            if(region != null) {
                Log.d(WoosmapSettings.Tags.WoosmapSdkTag, "Region already exist")
            } else {
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                    addOnSuccessListener {
                        Log.d(WoosmapSettings.Tags.WoosmapSdkTag,"onSuccess: Geofence Added...")
                        createRegion(id, radius.toDouble(),latitude,longitude,idStore)
                    }
                    addOnFailureListener {
                        val errorMessage = geofenceHelper.getErrorString(exception)
                        Log.d(WoosmapSettings.Tags.WoosmapSdkTag,"onFailure "+errorMessage)
                    }
                }
            }
        }.start()
    }

}