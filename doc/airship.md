

  
## Airship Integration
  
Generate contextual events from Geofencing SDK data using their different event types: Geofences, POI, Visits and ZOI.

Whenever location events are generated, the SDK geofencing will send custom events and properties to Airship via a delegate protocol. This data can then be used with the Custom Event trigger in the Automation and Journey composers.


### Configure Airship Integration in your app

To configure your app with the Airship SDK follow the instruction on the Airship web site :
https://docs.airship.com/platform/android/getting-started/

In the sample App, uncomment following line in the manifest of the app to enable Airship Push notification : 
```java
<meta-data android:name="com.urbanairship.autopilot"  
 android:value="com.webgeoservices.sample.SampleAutopilot"/>
``` 
Again in the manifest of the app, comment the following line to disable enrich  notification with location managing by the SDK.

```java
<!-- <service android:name="com.webgeoservices.woosmapgeofencing.WoosmapInstanceIDService">  
 <intent-filter> <action android:name="com.google.firebase.INSTANCE_ID_EVENT" />  
 </intent-filter></service>  
<service android:name="com.webgeoservices.woosmapgeofencing.WoosmapMessagingService">  
 <intent-filter> <action android:name="com.google.firebase.MESSAGING_EVENT" />  
 </intent-filter></service> -->
``` 

If you want to have notification enrich with location and Airship notification, you must modify the `WoosmapMessagingService` class in the Geofencing SDK like this : 

```java
@Override
public void onNewToken(String token) {
   AirshipFirebaseIntegration.processNewToken(getApplicationContext());
}

/**
 * Called when message is received.
 *
 * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
 */
@Override
public void onMessageReceived(RemoteMessage remoteMessage) {
    Log.d("woosmap_mobile_sdk", "onMessageReceived");
    WoosmapMessageBuilderMaps messageBuilderMaps;
    if (this.cls != null) {
        messageBuilderMaps = new WoosmapMessageBuilderMaps(this, this.cls);
    } else {
        messageBuilderMaps = new WoosmapMessageBuilderMaps(this);
    }
    WoosmapMessageDatas messageDatas = new WoosmapMessageDatas(remoteMessage.getData());
    if (messageDatas.isLocationRequest() && messageDatas.timestamp != null) {
        messageBuilderMaps.sendWoosmapNotification(messageDatas);
    } else {
	AirshipFirebaseIntegration.processMessageSync(getApplicationContext(), message);
    }
}
  ``` 
  



### Set up Airship events 
Custom events let you track user activities and key conversions in your application, and tie them back to corresponding push messaging campaigns. Custom events require analytics to be enabled. If disabled, any event that is added to analytics will be ignored. For a more detailed explanation on custom events and possible use cases, see the  [Custom Events](https://docs.airship.com/guides/messaging/user-guide/data/custom-events/)  topic guide.

Convenient templates are provided to create custom events for common account, media or retail related events. For more details see the  [Custom Event Templates](https://docs.airship.com/reference/integration/custom-event-templates/)  topic guide.

#### Create and name an event

```java
CustomEvent.Builder eventBuilder = new CustomEvent.Builder("event_name");
  ``` 

#### Set custom event properties on the builder
```java
eventBuilder.addProperty("date", displayDateFormatAirship.format(poi.dateTime));  
eventBuilder.addProperty("name", poi.name);
eventBuilder.addProperty("idStore", poi.idStore);  
eventBuilder.addProperty("city", poi.city);  
eventBuilder.addProperty("distance", poi.distance);
  ``` 
  
#### Record event

```java
CustomEvent event = eventBuilder.build();  
event.track();
  ``` 

### Send Airship events

#### Send POI event
In your `mainActivity`, create a listener connected to the interface `Woosmap.SearchAPIReadyListener` and set a callback to retrieve POIs from the Search API request. Then send your POI event like this : 

```java
public class WoosSearchAPIReadyListener implements Woosmap.SearchAPIReadyListener
{
    public void SearchAPIReadyCallback(POI poi)
    {
        onPOICallback(poi);
    }
}

private void onPOICallback(POI poi) {
    // get POI
    if(AIRSHIP) {
	// Create and name an event
	CustomEvent.Builder eventBuilder = new CustomEvent.Builder("poi_event");

	// Set custom event properties on the builder
	eventBuilder.addProperty("date", displayDateFormatAirship.format(poi.dateTime));
	eventBuilder.addProperty("name", poi.name);
	eventBuilder.addProperty("idStore", poi.idStore);
	eventBuilder.addProperty("city", poi.city);
	eventBuilder.addProperty("distance", poi.distance);

	Gson gson = new Gson();
	SearchAPI data = gson.fromJson( poi.data,SearchAPI.class );

	Object[] tagsArr = data.getFeatures()[0].getProperties().getTags();
	if( tagsArr.length != 0) {
	    eventBuilder.addProperty("tag", Arrays.toString(tagsArr));
	}

	Object[] typesArr = data.getFeatures()[0].getProperties().getTypes();
	if( typesArr.length != 0) {
	    eventBuilder.addProperty("type", Arrays.toString(typesArr));
	}

	// Then record it
	CustomEvent event = eventBuilder.build();
	    event.track();
    }
}
```

#### Send Visit event
In your `mainActivity`, create a listener connected to the interface `Woosmap.VisitReadyListener` and set a callback to retrieve Visit. Then send your visit event like this : 

```java
	public class WoosVisitReadyListener implements Woosmap.VisitReadyListener {
        public void VisitReadyCallback(Visit visit) {
            onVisitCallback(visit);
        }
    }

    private void onVisitCallback(Visit visit) {
        if(AIRSHIP) {
            // Create and name an event
            CustomEvent.Builder eventBuilder = new CustomEvent.Builder("visit_event");

            // Set custom event properties on the builder
            eventBuilder.addProperty("arrivalDate", displayDateFormatAirship.format(visit.startTime));
            eventBuilder.addProperty("departureDate",displayDateFormatAirship.format(visit.endTime));
            eventBuilder.addProperty("id", visit.id);
            eventBuilder.addProperty("latitude", visit.lat);
            eventBuilder.addProperty("longitude", visit.lng);

            // Then record it
            CustomEvent event = eventBuilder.build();
            event.track();
        }
    }
```


#### Send Region Geofence event
In your `mainActivity`, create a listener connected to the interface `Woosmap.RegionLogReadyListener` and set a callback to retrieve Region Log (enter, exit geofence). Then send your region log event like this : 

```java
public class WoosRegionLogReadyListener implements Woosmap.RegionLogReadyListener {
    public void RegionLogReadyCallback(RegionLog regionLog) {
        onRegionLogCallback(regionLog);
    }
}

private void onRegionLogCallback(RegionLog regionLog) {
    if(AIRSHIP) {
        // Create and name an event
        CustomEvent.Builder eventBuilder = new CustomEvent.Builder(regionLog.didEnter ? "geofence_entered_event" : "geofence_exited_event");

        // Set custom event properties on the builder
        eventBuilder.addProperty("date", displayDateFormatAirship.format(regionLog.dateTime));
        eventBuilder.addProperty("id",regionLog.id);
        eventBuilder.addProperty("radius", regionLog.radius);
        eventBuilder.addProperty("latitude", regionLog.lat);
        eventBuilder.addProperty("longitude", regionLog.lng);

        // Then record it
        CustomEvent event = eventBuilder.build();
        event.track();
    }
}
```

#### Send ZOI classified event
In your `mainActivity`, create a listener connected to the interface `Woosmap.RegionLogReadyListener` and set a callback to retrieve Region Log (enter, exit) of ZOI classified (HOME or WORK). Then send your region log event like this :

```java
public class WoosRegionLogReadyListener implements Woosmap.RegionLogReadyListener {
    public void RegionLogReadyCallback(RegionLog regionLog) {
        onRegionLogCallback(regionLog);
    }
}

private void onRegionLogCallback(RegionLog regionLog) {
    if(AIRSHIP) {
        String eventName = "";

        if (regionLog.identifier.contains("HOME") || regionLog.identifier.contains("WORK")) {
            eventName = "zoi_classified_";
        } else {
            eventName = "geofence_";
        }

        // Create and name an event
        CustomEvent.Builder eventBuilder = new CustomEvent.Builder(regionLog.didEnter ? eventName + "entered_event" : eventName + "exited_event");

        // Set custom event properties on the builder
        eventBuilder.addProperty("date", displayDateFormatAirship.format(regionLog.dateTime));
        eventBuilder.addProperty("id",regionLog.id);
        eventBuilder.addProperty("radius", regionLog.radius);
        eventBuilder.addProperty("latitude", regionLog.lat);
        eventBuilder.addProperty("longitude", regionLog.lng);

        // Then record it
        CustomEvent event = eventBuilder.build();
        event.track();
    }
}
```
##  Events and Properties

### Geofences

**geofence_entered_event**

date: String  
id: String  
lattitude: Double  
longitude: Double
radius: Double

**geofence_exited_event**

date: String  
id: String  
lattitude: Double  
longitude: Double
radius: Double

**zoi_classified_entered_event**

date: String
id: String
lattitude: Double
longitude: Double
radius: Double

**zoi_classified_exited_event**

date: String
id: String
lattitude: Double
longitude: Double
radius: Double

### POI

**POI_event**

date: String  
name: String  
idStore: String  
city: String  
distance: String  
tag: String  
type: String  

### Visit

**Visit_event**
date: String  
arrivalDate: String  
departureDate: String  
id: String  
lattitude: Double  
longitude: Double
