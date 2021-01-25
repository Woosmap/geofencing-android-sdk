##  Create and monitor geofences
  
Use region monitoring to determine when the user enters or leaves a geographic region.

Region monitoring (also known as geofencing) combines awareness of the user's current location with awareness of the user's proximity to locations that may be of interest. This region is a way for your app to be alerted when the user enters or exits a geographical region. To mark a location of interest, you specify its latitude and longitude. To adjust the proximity for the location, you add a radius. The latitude, longitude, and radius define a geofence, creating a circular area, or fence, around the location of interest.

In Android, regions are monitored by the system, which wakes up your app as needed when the user crosses a defined region boundary. 

Region monitoring is a natural complement to Search requests performed on collected locations. Indeed, Search requests help monitoring the approach to some assets you want to monitor. On every collected location you are aware of the surronding assets (distance to them and even time if using Distance API request). You can then decide to monitor some of those surrounding assets (e.g. the closest ones). Region monitoring is designed to do so.

<p align="center">
  <img alt="POI Region" src="https://github.com/woosmap/woosmap-geofencing-android-sdk/raw/master/assets/POIPenetration.png" width="50%">
</p>

### Set up for geofence monitoring

The first step in requesting geofence monitoring is to set `RegionReadyListener`,  this should be done as early as possible in your `mainActivity` on the method `onCreate`.

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Set Keys
    WoosmapSettings.privateKeyWoosmapAPI = "";
    WoosmapSettings.privateKeyGMPStatic = "";

    // Instanciate woosmap object
    this.woosmap = Woosmap.getInstance().initializeWoosmap(this);


    this.woosmap.setLocationReadyListener(new WoosLocationReadyListener());
    this.woosmap.setSearchAPIReadyListener (new WoosSearchAPIReadyListener ());
    this.woosmap.setDistanceAPIReadyListener (new WoosDistanceAPIReadyListener ());
    this.woosmap.setVisitReadyListener (new WoosVisitReadyListener ());
    this.woosmap.setRegionReadyListener( new WoosRegionReadyListener() );

    // Visit Detection Enable
    this.woosmap.setVisitEnable (true);

    // For android version >= 8 you have to create a channel or use the woosmap's channel
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        this.woosmap.createWoosmapNotifChannel();
    }
}
@Override
public void onResume() {
    super.onResume();
    if (checkPermissions()) {
        Log.d("WoosmapGeofencing", "Permission OK");
        this.woosmap.onResume();
    } else {
        Log.d("WoosmapGeofencing", "Permission NOK");
        requestPermissions();
    }
}

@Override
public void onPause(){
    super.onPause();
    Log.d("WoosmapGeofencing", "BackGround");
    if (checkPermissions()) {
        this.woosmap.onPause();
    }
}
``` 


### Retrieve Region events

In your `mainActivity`, create a listener connected to the interface `Woosmap.SearchAPIReadyListener` and set a callback to retrieve POIs from the Search API request.
```java
public class WoosRegionReadyListener implements Woosmap.RegionReadyListener {  
    public void RegionReadyCallback(Region region) {  
        onRegionCallback(region);  
  }  
}  
  
private void onRegionCallback(Region region) {  
    //get Region event  
}
```

Whenever the user crosses the boundary of one of your app's registered regions, the system notifies your app.

Boundary crossing notifications are delivered to your region delegate object. Specifically, `onRegionCallback` methods.

By experience, the region enter/exit event from android is triggered on new position inside the region or outside. Indeed the events are not triggered on the crossing limit of the region. So its important to have a minimum radius of the region should be set between 100 - 150 meters.

Regions have an associated identifier, which this method uses to look up information related to the region and perform the associated action.

Regions creation is enabled on the nearest result of the Search API request . The closest POI is so used to create 3 regions around it (100 m, 200m, and 300 m). If the automatic region monitoring is not necessary for your use cases, settings of the SDK can be modified as follow:
```java
WoosmapSettings.searchAPICreationRegionEnable = true;
```

You can change the radius of POI regions by settings the SDK as follow :
```java
WoosmapSettings.firstSearchAPIRegionRadius = 150;  
WoosmapSettings.secondSearchAPIRegionRadius = 300;  
WoosmapSettings.thirdSearchAPIRegionRadius = 450;
```

You can recover the regions created in the database of SDK as follow : 
```java
Region[] regionList = WoosmapDb.getInstance(mContext, true).getRegionsDAO().getAllRegions();
```

You can recover all the logs of region event in the database of SDK as follow : 
```java
RegionLog[] regionLogList = WoosmapDb.getInstance(mContext, true).getRegionLogsDAO().getAllRegionLogs();
```

### Create a custom region

A region is a circular area centered on a geographic coordinate. You can define one using a `LatLng` object. The radius of the region object defines its boundary. You define the regions you want to monitor and register them with the system by calling the `addGeofence(center: LatLng, radius: float radius)` method of `Woosmap.getInstance()`. The system monitors your regions until you explicitly ask it to stop.

```swift
Woosmap.getInstance().addGeofence( id, latLng, 100);
```

The limit of numbers of regions monitored can not been exceed. Indeed, regions are shared resources that rely on specific hardware capabilities. To ensure that all apps can participate in region monitoring, android prevents any single app from monitoring more than 100 regions simultaneously. 

To work around this limitation, monitor only regions that are close to the user’s current location. As the user moves, update the list based on the user’s new location.

Be Careful, to have a monitoring regions the user must allow permissions to share the position all time.

### Remove regions

To remove all regions created, you can use this method  : 
```java
Woosmap.getInstance().removeGeofences()
```

To remove a specific region, you can use this method with the id of the region  :  
```java
Woosmap.getInstance().removeGeofences(String id)
```
