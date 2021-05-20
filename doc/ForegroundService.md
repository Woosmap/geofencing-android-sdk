## Foreground Service

Since Android 8.0 (API level 26), Android OS limits how frequently an app can retrieve the user's current location while the app is running in the background. Under these conditions, apps can receive location updates only a few times per hour.
This location retrieval behavior is important to keep in mind if the use cases you want to cover with your app rely on real-time alerts or motion detection while the app is running in the background.

To achieve frequent location collection or geofence detections you have to enable a foreground_service provided by the OS. Here are the different steps to go through to enable it.

### Configuration Foreground Service

#### Manifest
You first need to add a new permission to your manifest file, this is the FOREGROUND_SERVICE permission.
```xml
<manifest>
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.location.GPS_ENABLED_CHANGE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
</manifest>
```

#### Configure the notification foreground service
Foreground services perform operations that are noticeable to the user.

Each foreground service must show a status bar notification that has a priority of PRIORITY_LOW. That way, users are actively aware that your app is performing a task either when acive or in background and is consuming system resources. The notification cannot be dismissed unless the service is either dsiabled or the app is killed.

If an app tries to use a status bar notification whose priority is lower than PRIORITY_LOW, the system adds a message to the notification drawer, alerting the user the app is using a foreground service.

In the manifest declare the icon for the notification :
```xml
<manifest>
...
<meta-data
            android:name="woosmap.messaging.default_notification_icon"
            android:resource="@drawable/ic_android_black_24dp" />
...
</manifest>
```

Set the channel ID of the notification :
```java
WoosmapSettings.WoosmapNotificationChannelID =  = "Location_Channel_ID"
```

Set the channel name  of the notification :
```java
WoosmapSettings.WoosmapNotificationChannelName =  = "Location Channel name"
```

Set the description of the notification :
```java
WoosmapSettings.WoosmapNotificationDescriptionChannel =  = "Description of the channel"
```

Set the title of the notification :
```java
WoosmapSettings.updateServiceNotificationTitle =  = "Title of the notification for foreground service"
```

Set the body message of the notification :
```java
WoosmapSettings.updateServiceNotificationText =  = "Text of the notification for foreground service"
```

Set the notification active at true if you want to prioritize the notification :
```java
WoosmapSettings.WoosmapNotificationActive = true
```

Set the notification active at false if you want a silent notification :
```java
WoosmapSettings.WoosmapNotificationActive = false
```


### Enabling Foreground Service

After enabling Woosmap Geofencing SDK, enable the Foreground Service as follow :
```java
WoosmapSettings.foregroundLocationServiceEnable = true;
```

### Set onDestroy()

Call the onDestroy() method on Wossmap when your app is destroy to stop the service :
```java
@Override
    protected void onDestroy() {
        super.onDestroy();
        woosmap.onDestroy();
    };
```

### Disabling Foreground Service

You have to disbale this service as soon as it's not useful anymore. Keep in minds that it consumes battery and resources. To do so update the conf as follow.
```java
WoosmapSettings.foregroundLocationServiceEnable = false;
```


