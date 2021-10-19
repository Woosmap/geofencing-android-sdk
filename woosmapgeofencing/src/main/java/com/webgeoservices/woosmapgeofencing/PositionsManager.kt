package com.webgeoservices.woosmapgeofencing

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.util.Log
import android.util.Pair
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.webgeoservices.woosmapgeofencing.DistanceAPIDataModel.DistanceAPI
import com.webgeoservices.woosmapgeofencing.SFMCAPI.SFMCAPI
import com.webgeoservices.woosmapgeofencing.SearchAPIDataModel.SearchAPIResponseItem
import com.webgeoservices.woosmapgeofencing.WoosmapSettings.*
import com.webgeoservices.woosmapgeofencing.WoosmapSettings.Tags.WoosmapSdkTag
import com.webgeoservices.woosmapgeofencing.WoosmapSettings.Tags.WoosmapVisitsTag
import com.webgeoservices.woosmapgeofencing.database.*
import org.jetbrains.anko.doAsync
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class PositionsManager(val context: Context, private val db: WoosmapDb) {

    private var temporaryCurrentVisits: MutableList<Visit> = mutableListOf<Visit>()
    private var temporaryFinishedVisits: MutableList<Visit> = mutableListOf<Visit>()
    private var requestQueue: RequestQueue? = null

    var tz = TimeZone.getTimeZone("UTC")
    var displayDateFormatAirship = SimpleDateFormat("dd-MM-yyyy HH:mm:ss'Z'")
    var displayDateFormatISO8601 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

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

        if (filterTimeBetweenRequestSearchAPI(movingPosition))
            return movingPosition

        if (distance > 0.0) {
            if (WoosmapSettings.searchAPIEnable == true) {
                requestSearchAPI(movingPosition)
            }

            if (WoosmapSettings.checkIfPositionIsInsideGeofencingRegionsEnable == true) {
                checkIfPositionIsInsideGeofencingRegions(movingPosition)
            }
        }



        return movingPosition
    }

    private fun checkIfPositionIsInsideGeofencingRegions(movingPosition: MovingPosition) {

        var regions = this.db.regionsDAO.allRegions

        regions.forEach {
            val regionCenter = Location("woosmap")
            regionCenter.latitude = it.lat
            regionCenter.longitude = it.lng

            val locationPosition = Location("woosmap")
            locationPosition.latitude = movingPosition.lat
            locationPosition.longitude = movingPosition.lng

            val isInside = locationPosition.distanceTo(regionCenter) < it.radius

            if(isInside != it.isCurrentPositionInside) {
                it.isCurrentPositionInside = isInside
                it.dateTime = System.currentTimeMillis()
                this.db.regionsDAO.updateRegion(it)

                var regionLog = RegionLog()
                regionLog.identifier = it.identifier
                regionLog.dateTime = it.dateTime
                regionLog.didEnter = it.didEnter
                regionLog.lat = it.lat
                regionLog.lng = it.lng
                regionLog.idStore = it.idStore
                regionLog.radius = it.radius
                regionLog.isCurrentPositionInside = isInside
                this.db.regionLogsDAO.createRegionLog(regionLog)
                if (WoosmapSettings.modeHighFrequencyLocation) {
                    if (Woosmap.getInstance().regionLogReadyListener != null) {
                        Woosmap.getInstance().regionLogReadyListener.RegionLogReadyCallback(regionLog)
                    }
                    if (Woosmap.getInstance().airshipRegionLogReadyListener != null) {
                        Woosmap.getInstance().airshipRegionLogReadyListener.AirshipRegionLogReadyCallback(setDataConnectorRegionLog(regionLog))
                    }
                    if (Woosmap.getInstance().marketingCloudRegionLogReadyListener != null) {
                        Woosmap.getInstance().marketingCloudRegionLogReadyListener.MarketingCloudRegionLogReadyCallback(setDataConnectorRegionLog(regionLog,true))
                    }
                    //SFMC connector API
                    sendDataToSFMC(regionLog)
                } else if(it.didEnter != isInside) {
                    if (Woosmap.getInstance().regionLogReadyListener != null) {
                        Woosmap.getInstance().regionLogReadyListener.RegionLogReadyCallback(regionLog)
                    }
                    if (Woosmap.getInstance().airshipRegionLogReadyListener != null) {
                        Woosmap.getInstance().airshipRegionLogReadyListener.AirshipRegionLogReadyCallback(setDataConnectorRegionLog(regionLog))
                    }
                    if (Woosmap.getInstance().marketingCloudRegionLogReadyListener != null) {
                        Woosmap.getInstance().marketingCloudRegionLogReadyListener.MarketingCloudRegionLogReadyCallback(setDataConnectorRegionLog(regionLog,true))
                    }
                    //SFMC connector API
                    sendDataToSFMC(regionLog)
                }
            }
        }

    }

    private fun sendDataToSFMC(regionLog: RegionLog) {
        var eventName = ""

        eventName =
            if (regionLog.identifier.contains("HOME") || regionLog.identifier.contains("WORK")) {
                "woos_zoi_classified_"
            } else {
                "woos_geofence_"
            }

        // Create and name an event
        if (regionLog.isCurrentPositionInside)
            eventName += "entered_event"
        else
            eventName += "exited_event"

        var key = ""

        when (eventName) {
            "woos_zoi_classified_entered_event"-> key = SFMCCredentials.get("zoiClassifiedEnteredEventDefinitionKey").toString()
            "woos_zoi_classified_exited_event"-> key = SFMCCredentials.get("zoiClassifiedExitedEventDefinitionKey").toString()
            "woos_geofence_entered_event"-> key = SFMCCredentials.get("regionEnteredEventDefinitionKey").toString()
            "woos_geofence_exited_event"-> key = SFMCCredentials.get("regionExitedEventDefinitionKey").toString()
        }

        if(key != "") {
            val SFMC = SFMCAPI(context)
            SFMC.pushDatatoMC(setDataConnectorRegionLog(regionLog, true), key);
        }

    }

    private fun setDataConnectorRegionLog(regionLog: RegionLog, formatDateISO8601: Boolean = false): HashMap<String, Any>? {
        var data = HashMap<String, Any>()
        var eventName = ""
        displayDateFormatISO8601.timeZone = tz
        displayDateFormatAirship.timeZone = tz

        eventName =
            if (regionLog.identifier.contains("HOME") || regionLog.identifier.contains("WORK")) {
                "woos_zoi_classified_"
            } else {
                "woos_geofence_"
            }

        // Create and name an event
        if (regionLog.isCurrentPositionInside)
            eventName += "entered_event"
        else
            eventName += "exited_event"

        if(regionLog.idStore.isEmpty()) {
            data.put("event", eventName)
            data.put("id", regionLog.id)
            if(formatDateISO8601) {
                data.put("date", displayDateFormatISO8601.format(regionLog.dateTime))
            } else {
                data.put("date", displayDateFormatAirship.format(regionLog.dateTime))
            }
            data.put("radius", regionLog.radius)
            data.put("latitude", regionLog.lat)
            data.put("longitude", regionLog.lng)
        } else {
            val poi = this.db.poIsDAO.getPOIbyStoreId(regionLog.idStore)
            setDataAirshipPOI(poi,formatDateISO8601)?.let { data.putAll(it) }
            if(formatDateISO8601) {
                data.put("date", displayDateFormatISO8601.format(regionLog.dateTime))
            } else {
                data.put("date", displayDateFormatAirship.format(regionLog.dateTime))
            }
            data.put("event", eventName)
            data.put("id", regionLog.id)
        }

        return data
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

    private fun filterTimeBetweenRequestSearchAPI(movingPosition: MovingPosition): Boolean {
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
                if (Woosmap.getInstance().airshipRegionLogReadyListener != null) {
                    Woosmap.getInstance().airshipRegionLogReadyListener.AirshipRegionLogReadyCallback(setDataConnectorRegionLog(regionLog))
                }
                if (Woosmap.getInstance().marketingCloudRegionLogReadyListener != null) {
                    Woosmap.getInstance().marketingCloudRegionLogReadyListener.MarketingCloudRegionLogReadyCallback(setDataConnectorRegionLog(regionLog,true))
                }
                //SFMC connector API
                sendDataToSFMC(regionLog)
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

        val url = getStoreAPIUrl(positon.lat, positon.lng)
        val req = StringRequest(Request.Method.GET, url,
            { response ->
                Thread {
                    assert(response != null)
                    val jsonObject = JSONObject(response.toString())
                    var POIList: MutableList<POI> = mutableListOf<POI>()
                    if (!jsonObject.has("error_message")) {
                        val features = jsonObject.getJSONArray("features")
                        if ( features.length() > 0) {
                            for (i in 0 until features.length()) {
                                val searchAPIResponseItem = SearchAPIResponseItem.fromJSON(
                                    jsonObject.getJSONArray("features").getJSONObject(i)
                                )
                                if(searchAPIResponseItem != null) {
                                    val POIaround = POI()
                                    POIaround.city = searchAPIResponseItem.city
                                    POIaround.zipCode = searchAPIResponseItem.zipCode
                                    POIaround.dateTime = positon.dateTime
                                    POIaround.distance = searchAPIResponseItem.distance
                                    POIaround.locationId = positon.id
                                    POIaround.idStore = searchAPIResponseItem.idstore
                                    POIaround.name = searchAPIResponseItem.name
                                    POIaround.lat = searchAPIResponseItem.geometry.location.lat
                                    POIaround.lng = searchAPIResponseItem.geometry.location.lng
                                    POIaround.radius = searchAPIResponseItem.radius.toInt()
                                    POIaround.address = searchAPIResponseItem.formattedAddress
                                    POIaround.contact = searchAPIResponseItem.contact
                                    POIaround.types =
                                        searchAPIResponseItem.types.joinToString(" - ")
                                    POIaround.tags = searchAPIResponseItem.tags.joinToString(" - ")
                                    POIaround.countryCode = searchAPIResponseItem.countryCode
                                    POIaround.openNow = searchAPIResponseItem.openNow
                                    POIaround.data = response

                                    POIList.add(POIaround)
                                    createPOIRegion(
                                        "POI_" + POIaround.name,
                                        POIaround.radius,
                                        POIaround.lat,
                                        POIaround.lng,
                                        POIaround.idStore
                                    )

                                    if (!filterDistanceBetweenRequestSearAPI(POIaround)) {
                                        if (WoosmapSettings.distanceAPIEnable) {
                                            requestDistanceAPI(POIaround, positon)
                                        } else {
                                            this.db.poIsDAO.createPOI(POIaround)
                                            if (Woosmap.getInstance().searchAPIReadyListener != null) {
                                                Woosmap.getInstance().searchAPIReadyListener.SearchAPIReadyCallback(
                                                    POIaround
                                                )
                                            }
                                            if (Woosmap.getInstance().airshipSearchAPIReadyListener != null) {
                                                Woosmap.getInstance().airshipSearchAPIReadyListener.AirshipSearchAPIReadyCallback(
                                                    setDataAirshipPOI(POIaround)
                                                )
                                            }
                                            if (Woosmap.getInstance().marketingCloudSearchAPIReadyListener != null) {
                                                Woosmap.getInstance().marketingCloudSearchAPIReadyListener.MarketingCloudSearchAPIReadyCallback(
                                                    setDataAirshipPOI(POIaround,true)
                                                )
                                            }
                                            if (SFMCCredentials.get("poiEventDefinitionKey") != null) {
                                                val SFMC = SFMCAPI(context)
                                                SFMC.pushDatatoMC(setDataAirshipPOI(POIaround, true),
                                                    SFMCCredentials.get("poiEventDefinitionKey").toString()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            cleanPOIRegion(POIList)
                        } else {
                            Log.d(WoosmapSdkTag, "ZERO RESULTS")
                        }
                    } else {
                        Log.d(WoosmapSdkTag, jsonObject.getString("error_message"))
                    }

                }.start()
            },
            { error ->
                Log.e(WoosmapSdkTag, error.toString() + " search API")
            })
        requestQueue?.add(req)
    }

    private fun setDataAirshipPOI(POIaround: POI, formatDateISO8601: Boolean = false): HashMap<String, Any>? {
        var data = HashMap<String, Any>()
        displayDateFormatISO8601.timeZone = tz
        displayDateFormatAirship.timeZone = tz
        data.put("event", "woos_poi_event")
        data.put("city", POIaround.city)
        data.put("zipCode", POIaround.zipCode)
        if(formatDateISO8601) {
            data.put("date", displayDateFormatISO8601.format(POIaround.dateTime))
        } else {
            data.put("date", displayDateFormatAirship.format(POIaround.dateTime))
        }
        data.put("distance", POIaround.distance)
        data.put("idStore", POIaround.idStore)
        data.put("name", POIaround.name)
        data.put("lat", POIaround.lat)
        data.put("lng", POIaround.lng)
        data.put("radius", POIaround.radius)
        data.put("address", POIaround.address)
        data.put("contact", POIaround.contact)
        data.put("types", POIaround.types)
        data.put("tags", POIaround.tags)
        if(POIaround.countryCode != "null")
            data.put("country_code", POIaround.countryCode)
        data.put("openNow",POIaround.openNow)

        //user Properties
        try {
            var json = JSONObject(POIaround.data)
            val userPropertiesFiltered =
                SearchAPIResponseItem.getUserProperties(json, POIaround.idStore)
            if(userPropertiesFiltered != null) {
                for ((key, value) in userPropertiesFiltered) {
                    data.put("user_properties_" + key, value)
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return data

    }

    private fun cleanPOIRegion(poiList: MutableList<POI>) {
        Thread {

            var regionsPOI = this.db.regionsDAO.regionPOI

            regionsPOI.forEach {
                var regionExist = false
                for(POIadded in poiList) {
                    if (it.identifier.contains("POI_" + POIadded.name)) {
                        regionExist = true
                    }
                }
                if (!regionExist) {
                    //Remove last POI geofence in geofencing manager
                    Woosmap.getInstance().removeGeofence(it.identifier)
                    //Remove last POI geofence in db
                    this.db.regionsDAO.deleteRegionFromId(it.identifier)
                }
            }
        }.start()

    }


    fun getStoreAPIUrl(lat: Double, lng: Double): String? {
        var url = String.format(WoosmapSettings.Urls.SearchAPIUrl, WoosmapSettings.Urls.WoosmapURL, WoosmapSettings.privateKeyWoosmapAPI, lat, lng)
            if (!WoosmapSettings.searchAPIParameters.isEmpty()) {
                val stringBuilder: StringBuilder = StringBuilder(url)

                for ((key, value) in WoosmapSettings.searchAPIParameters) {
                    stringBuilder.append("&" + key + "=" + value)
                }
                url = stringBuilder.toString()
            }
        return url
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
            { response ->
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
                    if (Woosmap.getInstance().airshipSearchAPIReadyListener != null) {
                        Woosmap.getInstance().airshipSearchAPIReadyListener.AirshipSearchAPIReadyCallback(setDataAirshipPOI(POIaround))
                    }
                    if (Woosmap.getInstance().marketingCloudSearchAPIReadyListener != null) {
                        Woosmap.getInstance().marketingCloudSearchAPIReadyListener.MarketingCloudSearchAPIReadyCallback(setDataAirshipPOI(POIaround, true))
                    }
                    if (SFMCCredentials.get("poiEventDefinitionKey") != null) {
                        val SFMC = SFMCAPI(context)
                        SFMC.pushDatatoMC(setDataAirshipPOI(POIaround, true),
                            SFMCCredentials.get("poiEventDefinitionKey").toString()
                        )
                    }

                }.start()
            },
            { error ->
                Log.e(WoosmapSdkTag, error.toString() + " Distance API")
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

        val url = getStoreAPIUrl(lat, lng)
        val req = StringRequest(Request.Method.GET, url,
            { response ->
                Thread {
                    assert(response != null)
                    val jsonObject = JSONObject(response.toString())
                    var POIList: MutableList<POI> = mutableListOf<POI>()
                    if (!jsonObject.has("error_message")) {
                        val features = jsonObject.getJSONArray("features")
                        if (features.length() > 0) {
                            for (i in 0 until features.length()) {
                                val searchAPIResponseItem = SearchAPIResponseItem.fromJSON(
                                    jsonObject.getJSONArray("features").getJSONObject(i)
                                )
                                if(searchAPIResponseItem != null) {
                                    val POIaround = POI()
                                    POIaround.city = searchAPIResponseItem.city
                                    POIaround.zipCode = searchAPIResponseItem.zipCode
                                    POIaround.dateTime = System.currentTimeMillis()
                                    POIaround.distance = searchAPIResponseItem.distance
                                    POIaround.locationId = positionId
                                    POIaround.idStore = searchAPIResponseItem.idstore
                                    POIaround.name = searchAPIResponseItem.name
                                    POIaround.lat = searchAPIResponseItem.geometry.location.lat
                                    POIaround.lng = searchAPIResponseItem.geometry.location.lng
                                    POIaround.radius = searchAPIResponseItem.radius.toInt()
                                    POIaround.address = searchAPIResponseItem.formattedAddress
                                    POIaround.contact = searchAPIResponseItem.contact
                                    POIaround.types =
                                        searchAPIResponseItem.types.joinToString(" - ")
                                    POIaround.tags = searchAPIResponseItem.tags.joinToString(" - ")
                                    POIaround.countryCode = searchAPIResponseItem.countryCode
                                    POIaround.data = response

                                    this.db.poIsDAO.createPOI(POIaround)
                                    if (Woosmap.getInstance().searchAPIReadyListener != null) {
                                        Woosmap.getInstance().searchAPIReadyListener.SearchAPIReadyCallback(
                                            POIaround
                                        )
                                    }

                                    if (Woosmap.getInstance().airshipSearchAPIReadyListener != null) {
                                        Woosmap.getInstance().airshipSearchAPIReadyListener.AirshipSearchAPIReadyCallback(
                                            setDataAirshipPOI(POIaround)
                                        )
                                    }

                                    if (Woosmap.getInstance().marketingCloudSearchAPIReadyListener != null) {
                                        Woosmap.getInstance().marketingCloudSearchAPIReadyListener.MarketingCloudSearchAPIReadyCallback(
                                            setDataAirshipPOI(POIaround,true)
                                        )
                                    }
                                    if (SFMCCredentials.get("poiEventDefinitionKey") != null) {
                                        val SFMC = SFMCAPI(context)
                                        SFMC.pushDatatoMC(setDataAirshipPOI(POIaround, true),
                                            SFMCCredentials["poiEventDefinitionKey"].toString()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }.start()
            },
            { error ->
                Log.e(WoosmapSdkTag, error.toString() + " search API")
            })
        requestQueue?.add(req)
    }

    private fun createPOIRegion(POIid: String, POIradius: Int, latitudePOI: Double, longitudePOI: Double, POIidStore: String) {
        Thread {
            var regionsPOI = this.db.regionsDAO.regionPOI
            var regionExist = false

            regionsPOI.forEach {
                regionExist = it.identifier.contains(POIid)
            }

            if (!regionExist) {
                Woosmap.getInstance().addGeofence(POIid + "_" + POIradius, LatLng(latitudePOI, longitudePOI),
                    POIradius.toFloat(), POIidStore)
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
            { response ->
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
            { error ->
                Log.e(WoosmapSdkTag, error.toString() + " Distance API")
            })
        requestQueue?.add(req)
    }

    private fun createVisit(visit: Visit) {
        this.db.visitsDao.createStaticPosition(visit)
        temporaryCurrentVisits.add(visit)
    }

    private fun finishVisit(visit: Visit) {
        visit.duration = visit.endTime - visit.startTime


        if(visit.duration >= WoosmapSettings.durationVisitFilter) {
            // Refresh zoi on Visit
            val figmmForVisitsCreator = FigmmForVisitsCreator(db)
            figmmForVisitsCreator.figmmForVisit(visit)

            this.db.visitsDao.updateStaticPosition(visit)
            temporaryFinishedVisits.add(visit)
            if (Woosmap.getInstance().visitReadyListener != null) {
                Woosmap.getInstance().visitReadyListener.VisitReadyCallback(visit)
            }

            if (Woosmap.getInstance().airshipVisitReadyListener != null) {
                Woosmap.getInstance().airshipVisitReadyListener.AirshipVisitReadyCallback(setDataAirshipVisit(visit))
            }

            if (Woosmap.getInstance().marketingCloudVisitReadyListener != null) {
                Woosmap.getInstance().marketingCloudVisitReadyListener.MarketingCloudVisitReadyCallback(setDataAirshipVisit(visit,true))
            }

            if (SFMCCredentials.get("visitEventDefinitionKey") != null) {
                val SFMC = SFMCAPI(context)
                SFMC.pushDatatoMC(setDataAirshipVisit(visit, true),
                    SFMCCredentials.get("visitEventDefinitionKey").toString()
                )
            }

        }


    }

    private fun setDataAirshipVisit(visit: Visit, formatDateISO8601: Boolean = false): HashMap<String, Any>? {
        var data = HashMap<String, Any>()
        displayDateFormatISO8601.timeZone = tz
        displayDateFormatAirship.timeZone = tz
        data.put("event", "woos_visit_event")
        if (formatDateISO8601) {
            data.put("arrivalDate", displayDateFormatISO8601.format(visit.startTime))
            data.put("departureDate", displayDateFormatISO8601.format(visit.endTime))
        } else {
            data.put("arrivalDate", displayDateFormatAirship.format(visit.startTime))
            data.put("departureDate", displayDateFormatAirship.format(visit.endTime))
        }
        data.put("id", visit.id)
        data.put("latitude", visit.lat)
        data.put("longitude", visit.lng)

        return data
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

            var regionLog = RegionLog()
            regionLog.identifier = regionDetected.identifier
            regionLog.dateTime = regionDetected.dateTime
            regionLog.didEnter = regionDetected.didEnter
            regionLog.lat = regionDetected.lat
            regionLog.lng = regionDetected.lng
            regionLog.idStore = regionDetected.idStore
            regionLog.radius = regionDetected.radius
            regionLog.isCurrentPositionInside = regionDetected.isCurrentPositionInside


            if(regionDetected.didEnter != regionDetected.isCurrentPositionInside) {

                regionLog.isCurrentPositionInside = regionDetected.didEnter
                regionDetected.isCurrentPositionInside = regionDetected.didEnter

                this.db.regionLogsDAO.createRegionLog(regionLog)

                regionDetected.dateTime = System.currentTimeMillis()
                this.db.regionsDAO.updateRegion(regionDetected)

                if (Woosmap.getInstance().regionLogReadyListener != null) {
                    Woosmap.getInstance().regionLogReadyListener.RegionLogReadyCallback(regionLog)
                }
                if (Woosmap.getInstance().airshipRegionLogReadyListener != null) {
                    Woosmap.getInstance().airshipRegionLogReadyListener.AirshipRegionLogReadyCallback(setDataConnectorRegionLog(regionLog))
                }
                if (Woosmap.getInstance().marketingCloudRegionLogReadyListener != null) {
                    Woosmap.getInstance().marketingCloudRegionLogReadyListener.MarketingCloudRegionLogReadyCallback(setDataConnectorRegionLog(regionLog,true))
                }
                //SFMC connector API
                sendDataToSFMC(regionLog)
            } else {
                this.db.regionLogsDAO.createRegionLog(regionLog)
                regionDetected.dateTime = System.currentTimeMillis()
                this.db.regionsDAO.updateRegion(regionDetected)
            }

        }.start()
    }

    fun removeGeofence(id: String) {
        Thread {
            this.db.regionsDAO.deleteRegionFromId(id)
        }.start()

    }


    @SuppressLint("MissingPermission")
    fun addGeofence(geofenceHelper: GeofenceHelper, geofencingRequest: GeofencingRequest, geofencePendingIntent: PendingIntent, geofencingClient: GeofencingClient, id: String, radius: Float, latitude: Double, longitude: Double, idStore: String)  {
        Thread {
            val region = this.db.regionsDAO.getRegionFromId(id)
            if(region != null) {
                Log.d(WoosmapSdkTag, "Region already exist")
            } else {
                createRegion(id, radius.toDouble(),latitude,longitude,idStore)
            }
        }.start()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                Log.d(WoosmapSdkTag,"onSuccess: Geofence Added...")
            }
            addOnFailureListener {
                val errorMessage = geofenceHelper.getErrorString(exception)
                Log.d(WoosmapSdkTag,"onFailure "+errorMessage)
            }
        }
    }

}