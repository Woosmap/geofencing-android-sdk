
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

## Find the Closest POIs and Display a Result Map

Location of the mobile is the first step but you may need to contextualize this location. Answering questions like “Where might be the user?”, “What could he be visiting?” is one step further to provide value to your users.

In the code below a call to the Woosmap Search API is performed to find what is the closest POI from the user location.

Don’t forget to load the proper POIs you want to monitor in Woosmap first (your stores/restaurants/services, your competitors, etc). You’ll find anything about how to do this in our [Woosmap Developer Documentation](https://developers.woosmap.com/get-started).

 
Once the location obtained and the closest store identified, plot all of this on a Static Map from Google Maps API. Thanks to the code below you’ll obtain a jpeg file to display in your notification.

Obviously, you must wait for the result of the first API call before calling the Google Maps API.

 ```java
getLatestLocation(this.context, new OnSuccessListener<Location> () {
    @Override
    public void onSuccess(final Location location) {
        if (location == null) {
            Log.d(WoosmapSettings.Tags.WoosmapTag, "Can't get user Location");
            return;
        }

        final RequestQueue requestQueue = Volley.newRequestQueue(context);

        String urlAPI = String.format(WoosmapSettings.Urls.SearchAPIUrl, WoosmapSettings.privateKeySearchAPI, location.getLatitude(), location.getLongitude ());
        StringRequest stringRequest = new StringRequest (urlAPI, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Gson gson = new Gson();
                SearchAPI data = gson.fromJson (response, SearchAPI.class);
                Feature featureSearch = data.getFeatures ()[0];
                String city = featureSearch.getProperties ().getAddress ().getCity ();
                String zipcode = featureSearch.getProperties ().getAddress ().getZipcode ();
                String distance = String.valueOf (featureSearch.getProperties ().getDistance ());
                double longitudePOI = featureSearch.getGeometry ().getCoordinates ()[0];
                double latitudePOI = featureSearch.getGeometry ().getCoordinates ()[1];

                // Fill body message with informations from API
                final String messageBody = "city = " + city +  "\n zipcode = " + zipcode + "\n distance = " + distance;
                mBuilder.setContentText (messageBody);
                mBuilder.setContentTitle("Location Notification");

                // Request Google Maps Static
                String urlGMPStatic = String.format (WoosmapSettings.Urls.GoogleMapStaticUrl,String.valueOf (location.getLatitude ()),String.valueOf (location.getLongitude ()),String.valueOf (latitudePOI),String.valueOf (longitudePOI),WoosmapSettings.privateKeyGMPStatic);

                // Retrieves an image specified by the URL, displays it in the UI.
                ImageRequest request = new ImageRequest (urlGMPStatic,
                        new Response.Listener<Bitmap>() {
                            @Override
                            public void onResponse(Bitmap bitmap) {
                                mStyle[0] = new NotificationCompat.BigPictureStyle().bigPicture(bitmap);
                                mBuilder.setLargeIcon (bitmap);
                                mBuilder.setStyle(mStyle[0]);

                                mBuilder.setContentIntent(mPendingIntent);

                                Notification notification = mBuilder.build();
                                mNotificationManager.notify(new Random ().nextInt(20), notification);

                            }
                        }, 0, 0, null, null,
                        new Response.ErrorListener() {
                            public void onErrorResponse(VolleyError error) {
                                Log.e (WoosmapSettings.Tags.WoosmapTag, error.toString() + " maps.google.com");
                            }
                        });
                // Add ImageRequest to the RequestQueue
                requestQueue.add(request);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Anything you want
                Log.e (WoosmapSettings.Tags.WoosmapTag, error.toString() + " search API");
            }
        });
        requestQueue.add(stringRequest);

    }
}); 
```

Modify the body, subtitle and attachment of the content handler to show the informations from APIs.
