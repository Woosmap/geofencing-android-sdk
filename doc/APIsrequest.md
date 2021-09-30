
## How to use SearchAPI 

To obtain on demand the closest POI from a location, you must instanciate the class `PositionsManager` call the method `searchAPI` and get the result on the callback `SearchAPIReadyCallback` or get the POI in the database inside the SDK. 

In your activity, instanciate Woosmap, set keys, and set a listener to monitor result of the SearchAPI request :

```java
public class MainActivity extends AppCompatActivity {
  private Woosmap woosmap;

  public class WoosSearchAPIReadyListener implements Woosmap.SearchAPIReadyListener {
        public void SearchAPIReadyCallback(POI poi) {
            onPOICallback(poi);
        }
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
       // Instanciate woosmap object
       this.woosmap = Woosmap.getInstance().initializeWoosmap(this);
       
       // Set Keys
        WoosmapSettings.privateKeyWoosmapAPI = "";
       
       // Set the Search API listener 
       this.woosmap.setSearchAPIReadyListener(new WoosSearchAPIReadyListener());
       
       ...
       
  }
```
On a refresh location event or ever you want :

```java
  PositionsManager mPositionsManager = new PositionsManager(getContext(), WoosmapDb.getInstance(getContext(), true));
  
  // set latitude and longitude of the location, and the id of the location if you want to update a location in database of the SDK or you can set to 0 for the id location.
  mPositionsManager.searchAPI( location.getLatitude(), location.getLongitude(), location.getLocationId() );
```


Get the result of the request Search API in the callback `onPOICallback` definie in your activity below :

```java
  private void onPOICallback(POI poi) {
    // Get data of the POI
  }
```

All result of request SerachAPI are strored in the SDK database, you can retrieve data from the SDK database like this : 
```java
  POI[] poiList = WoosmapDb.getInstance(mContext, true).getPOIsDAO().getAllPOIs();
```
Important :  all retrieve from the database must be launch in a asynchronous task. 

Informations about the search API : https://developers.woosmap.com/products/search-api/get-started/

## How to use DistanceAPI 

To obtain on demand a distance and duration between an origin and destinations, you must instanciate the class `PositionsManager` call the method `DistanceAPI` and get the result on the callback `DistanceAPIReadyCallback` or get the POI completed with informations about distance and duration in the database inside the SDK. 

In your activity, instanciate Woosmap, set keys, specifies the mode of transport to use when calculating distance and set a listener to monitor result of the DistanceAPI request :
```java
public class MainActivity extends AppCompatActivity {
  private Woosmap woosmap;

  public class WoosDistanceAPIReadyListener implements Woosmap.DistanceAPIReadyListener {
        public void DistanceAPIReadyCallback(DistanceAPI distanceAPIData) {
            onDistanceAPICallback(distanceAPIData);
        }
   }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
       // Instanciate woosmap object
       this.woosmap = Woosmap.getInstance().initializeWoosmap(this);
       
       // Set Keys
       WoosmapSettings.privateKeyWoosmapAPI = "";
       
       //Specifies the mode of transport to use when calculating distance. Valid values are "driving", "cycling", "walking". (if not specified default is driving)
       WoosmapSettings.modeDistance = "driving";
       
       // Set the Distance API listener 
       this.woosmap.setDistanceAPIReadyListener(new WoosDistanceAPIReadyListener());
       
       ...
       
  }
```

Launch request DistanceAPI :

```java
  PositionsManager mPositionsManager = new PositionsManager(getContext(), WoosmapDb.getInstance(getContext(), true));
  
       // set latitude and longitude of the origin, 
       // set the latitude and longitude of the destinations,
       // and the id of the location if you want to update a POI in database of the SDK or you can set to 0 for the id location.
      List<Pair<Double, Double>> listDestinationPoint = new ArrayList<>();
      listDestinationPoint.add(new Pair(place.getLatitude(), place.getLongitude()));
      Double latOrigin = currentPosition.getLatitude();
      Double lngOrigin = currentPosition.getLongitude();
      mPositionsManager.distanceAPI(latOrigin,lngOrigin,listDestinationPoint,place.getLocationId());
```

Get the result of the request Distance API in the callback `onDistanceAPICallback` definie in your activity below :

```java
   private void onDistanceAPICallback(DistanceAPI distanceAPIData) {
       //Get the data of the reponse distanceAPI
   }
```


Informations about the Distance API :https://developers.woosmap.com/products/distance-api/get-started/

## Find the Nearest POIs

After receiving the user location, the SDK makes a request to the searchAPI to get the nearest POI. We can modify the request and add other filters to improve your results in the method `searchAPIRequest`: 

 ```java
fun requestSearchAPI(positon: MovingPosition) {
    if (requestQueue == null) {
        requestQueue = Volley.newRequestQueue(this.context)
    }

    if(WoosmapSettings.privateKeyWoosmapAPI.isEmpty()){
        return
    }

    val url = getStoreAPIUrl(positon.lat,positon.lng)
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
                        if(WoosmapSettings.distanceAPIEnable) {
                            requestDistanceAPI(POIaround,positon)
                        }else {
                            this.db.poIsDAO.createPOI(POIaround)
                            if (Woosmap.getInstance().searchAPIReadyListener != null) {
                                Woosmap.getInstance().searchAPIReadyListener.SearchAPIReadyCallback(POIaround)
                            }
                            if (Woosmap.getInstance().airshipSearchAPIReadyListener != null) {
                                Woosmap.getInstance().airshipSearchAPIReadyListener.AirshipSearchAPIReadyCallback(setDataAirshipPOI(POIaround))
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
```

Modify the body, subtitle and attachment of the content handler to show the informations from APIs.

### Radius of POI
When you create a geofence around a POI, manually define the radius value: 
```java
WoosmapSettings.poiRadius = 500;
```
or choose the user_properties subfield that corresponds to radius value of the geofence:
```java
WoosmapSettings.poiRadiusNameFromResponse = "radiusPOI";
```
