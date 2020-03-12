
## Woosmap Geofencing

Location intelligence is one of the next revolutions to improve and "smoothen" user experience on mobile. 
Mobile operating systems use and provide multiple location services that might be tricky to handle or tune to achieve advanced location based services on mobile. And users are more and more aware of the capabilities of their mobile devices.
During the last two years, we analysed, exploited and followed the evolution of those location services, changes that occurred either on tech side or regulation side.

We are convinced that location is an effective way for App makers to propose tailor made and locally contextualised interactions with mobile users.
But knowing the location of a user is not enough. Knowing from what a user is close to or what he is visiting is the important part. So we decided to share our findings and tricks for location collection on mobile to help you focus on this real value of location. 

This repository is designed to share samples of codes on Android to take the best of location in your mobile apps and go a step further in Location Intelligence.
Woosmap Geofencing code samples should help you build Rich Push Notifications (highlighted with a Location context), analyse your mobile users surroundings (search for proximity to your assets, competitors, etc) and much more on Android.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
Thank you for your suggestions!

## License
Woosmap Geofencing is released under the MIT License. See LICENSE file for details.

## Links
The official site for the library is at https://community.woosmap.com/geolocation-push-notification/.

##  Overview

### Get user location 

Get the location of user with most optimization battery and search the nearest POI. In this sample, we call a search API Woosmap to get the POIs nearrest of the location of the user with use less battery.

<p align="center">
  <img alt="WoosmapGeofencing" src="/assets/WoosmapGeofencing1.png" width="30%">
</p>

### Enrich Notification with user location and POIs (Points of Interest)
Get the location of a user on notification reception, to complete the payload with local information from third parties APIs.  
In this sample, location fetched is then used to perform a request to the Woosmap Search API to get the closest POIs (Points of Interest) to the location of the user. In addition, a call to Google Static Map is performed to enrich the notification with a map displaying the user location and the closest POI.

<p align="center">
  <img alt="Notification Location" src="/assets/2Markers.png" width="50%">
</p>

### Visit Detection 
Get the location of the user when he stay in a place. You can know how much time he spends in a location.

<p align="center">
  <img alt="Visit" src="/assets/visit.png" width="50%">
</p>

##  Pre-requisites
-   Android SDK 29
-   Android Build Tools
-   Android Support Repository
-   Firebase Credentials

### Installation
* This sample uses the Gradle build system. To build this project, use the "gradlew build" command or use "Import Project" in Android Studio.
* Get config file for your Android app
* Compile and install the mobile app onto your mobile device.
* Download Firebase config file :
	1.  Sign in to Firebase, then open your project.
	2.  Click <img src="https://storage.googleapis.com/support-kms-prod/vMSwtm9y2uvHQAg2OfjmWpsBMtG4xwSIPWxh" alt="the Settings icon" width="2%">, then select  **Project settings**.
	3.  In the  **Your apps**  card, select the package name of the app for which you need a config file.
	4.  Click  ![](https://lh3.googleusercontent.com/F_l_k73LFMmhZzlG3uUxR85785RlZFMYIszJFNl6Xq4k_xMLdgotg_O95JGyk8bSlQ=w24) **google-services.json**, then add it to your app.

### Get Keys
* Get the token in the log debug.
* If you want a map in the notification and a map your app, get Google Maps API Key for requesting a static map (see [Google documentation](https://developers.google.com/maps/documentation/maps-static/get-api-key))
<p align="center">
 	<img src="./assets/GmapStatic.png" alt="Google map Static" width="50%">
</p>

<p align="center">
 	<img src="./assets/appMap.png" alt="Google map in app" width="50%">
</p>


* If you want to retrieve the closest of your store from the user location, load your assets in a Woosmap Project and get a Woosmap Key API (see [Woosmap developer documentation](https://developers.woosmap.com/get-started).)
<p align="center">
	<img src="./assets/SearchAPIList.png" alt="Search API" width="50%">
</p>
<p align="center">
	<img src="./assets/SearchAPIonly.png" alt="Search API" width="50%">
</p>

* If you don't use any third party API and don’t define API keys, the notification and the app will only display the location (lat/long) of the user.
<p align="center">
	<img src="./assets/userLocation.png" alt="User Location" width="50%">
</p>

<p align="center">
	<img src="./assets/userLocationApp.png" alt="User Location" width="50%">
</p>

## Usage 
The first step that should always be done each time your app is launched (in Foreground AND Background) is to set your Woosmap Private key Search API. This should be done as early as possible in your `mainAcitivity` on the method `onCreate`.

### Import and instanciate Woosmap library

Instanciate Woosmap and set keys : 
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
	
	// Set Keys  
	WoosmapSettings.privateKeySearchAPI = "";  
	WoosmapSettings.privateKeyGMPStatic = "";

    // Instanciate woosmap object
    this.woosmap = Woosmap.getInstance().initializeWoosmap(this);
      
	
    this.woosmap.setLocationReadyListener(new WoosLocationReadyListener());
    this.woosmap.setSearchAPIReadyListener (new WoosSearchAPIReadyListener ());
    this.woosmap.setVisitReadyListener (new WoosVisitReadyListener ());

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

For the google map in the app, add your GMP Key SDK android key in the manifest : 
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.webgeoservices.sample">

    <uses-permission android:name="android.permission.INTERNET" />
    ...
	...

    <application
      ...
      ...

    	<meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="GMP_KEY"/>

    </application>

</manifest>
```

To work properly, you have to instanciate the Woosmap object in the onCreate function and call Woosmap's onResume and onPause functions.

### Configure filters of refresh location and active visit detection

You can make filters on :
* Time to refresh user location in seconds :
```java 
static public int currentLocationTimeFilter = 0;  
 ```
 * Distance to refresh user location in meter :
```java 
static public int currentLocationDistanceFilter = 0;  
 ```
 * Time to request Search API in seconds :
```java 
static public int searchAPITimeFilter = 0;  
 ```  
 * Distance to request Search API  in meter : 
```java  
static public int searchAPIDistanceFilter = 0;  
 ``` 
 * Accuracy of the location in meter :
```java   
static public int accuracyFilter = 0;  
 ```
  * Distance detection threshold for visits :
```java   
static public double distanceDetectionThresholdVisits = 25.0;
 ```
 * Delay for outdated notification in seconds : 
```java   
static public int outOfTimeDelay = 300;
 ```
To apply filter,  set the filter in singleton `WoosmapSettings` like this :
```java   
WoosmapSettings.currentLocationTimeFilter = 30; 
```

The default parameters are defined by tests in order to obtain the best data while consuming the least amount of battery, you can modify them you can modify them according to the use cases.
### Retrieve User Location

In your `mainAcitivity` , create a Listener connect to the interface `Woosmap.LocationReadyListener` and set a callback to retrieve user current location.

```java
public class WoosLocationReadyListener implements Woosmap.LocationReadyListener
{
    public void LocationReadyCallback(Location location)
    {
        onLocationCallback(location);
    }
}

private void onLocationCallback(Location currentLocation) {
    ...
}
```

### Retrieve POI from Search API

In your `mainAcitivity` , create a Listener connect to the interface `Woosmap.SearchAPIReadyListener` and set a callback to retrieve POI from the request on Search API.
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
}
```

### Retrieve visit detection 

In your `mainAcitivity` , create a Listener connect to the interface `Woosmap.VisitReadyListener` and set a callback to retrieve visit.
```java
private void onPOICallback(POI poi) {  
    new POITask(getApplicationContext (),this).execute();  
}  
  
public class WoosVisitReadyListener implements Woosmap.VisitReadyListener  
{  
    public void VisitReadyCallback(Visit visit)  
    {  
        onVisitCallback(visit);  
  }  
}  
  
private void onVisitCallback(Visit visit) {  
    // get visit  
}
```

### Enable location after a device reboot
#### Create the BroadcasReceiver
To collect location after a device reboot without having to relaunch the application, you have to create a Broadcast which launches the jobInstantService `WoosmapRebootJobService` when it receives the BOOT_COMPLETED event.
```java
package com.webgeoservices.sample;  
  
import android.content.BroadcastReceiver;  
import android.content.Context;  
import android.content.Intent;  
  
import com.webgeoservices.woosmapGeofencing.WoosmapRebootJobService;  
  
public class RunOnStartup extends BroadcastReceiver {  
  
    public void onReceive(Context context, Intent intent) {  
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {  
            WoosmapRebootJobService.enqueueWork(context, new Intent());  
  }  
    }  
}
```

#### Add the BroadcastReceiver to the Manifest
Add the permission `android.permission.RECEIVE_BOOT_COMPLETED`
```
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```
Then, just declare your receiver in the Manifest.xml in the application bloc

```
<receiver android:name=".RunOnStartup">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED"/>
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</receiver>
```

## Simulate Notification
-   Get the notification token in the log debug or on the main screen of the demo app.
    
-   Install the app PushNotification from the github : [https://github.com/onmyway133/PushNotifications](https://github.com/onmyway133/PushNotifications). This desktop app will help you simulate notification sending if you do not have any other Notification Solutions.
    
-   Enter your server key : [https://github.com/onmyway133/PushNotifications#android-server-key](https://github.com/onmyway133/PushNotifications#android-server-key)
    
-   Enter a message in json format like this "{"location":"1","timestamp":"1589288354"}". The object "location" allows to have a location (lat/long) displayed in the notification. The "timestamp" object validates the delay between the server time and the mobile time to check if the retrieved location is not outdated (if difference between server and mobile time is greater than 300 sec, notification will not be displayed).
    
-   If you want to send notification directly from an Android app, you can use this project : [https://github.com/megamendhie/Notify](https://github.com/megamendhie/Notify). Change the code to enter the server key and the notification token of the app to target.



## Additional Documentation

* [Enabling Location](./doc/EnablingLocation.md) : To use location, first thing is enabling associated services on the user device. Find out here how to do it and more importantly what are the different permissions and consequences of choices made by the users.

* [Set up a Firebase Cloud Messaging](./doc/EnablingLocation.md) : Find out how to add Firebase Messaging to your App and regularly check if your notification token is up to date.

* [Handling messages](./doc/HandlingMessages.md) :If you are here, it’s because you want custom notifications. Find out here how to handle those.

* [Check Location permissions](./doc/CheckLocationpermissions.md) : Because differences may occur between sending time and reception time, you may need to check it before retrieving a location.

* [Check Timestamp of the payload](./doc/CheckTimeStamp.md) : Retrieve location from the OS location services to enrich your notification.

* [APIs request](./doc/APIsrequest.md) : Location of the mobile is one thing but knowing from what the mobile is close to is another thing. Find out here how to use Woosmap Search API to “geo contextualize” the location of your users.
