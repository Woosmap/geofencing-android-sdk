## Foreground Service

Since Android 8.0 (API level 26) limits how frequently an app can retrieve the user's current location while the app is running in the background. Under these conditions, apps can receive location updates only a few times each hour.
This location retrieval behavior is particularly important to keep in mind if your app relies on real-time alerts or motion detection while running in the background.

Consider whether your app's use cases for running in the background cannot succeed at all if your app receives infrequent location updates.

If this is the case, you can retrieve location updates more frequently by performing one of the following actions:

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

Each foreground service must show a status bar notification that has a priority of PRIORITY_LOW. That way, users are actively aware that your app is performing a task in the foreground and is consuming system resources. The notification cannot be dismissed unless the service is either stopped or removed from the foreground.

If an app tries to use a status bar notification whose priority is lower than PRIORITY_LOW, the system adds a message to the notification drawer, alerting the user to the app's use of a foreground service.

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

Set the title of the notification :
```java
WoosmapSettings.updateServiceNotificationTitle =  = "Title of the notification for foreground service"
```

Set the body message of the notification :
```java
WoosmapSettings.updateServiceNotificationText =  = "Text of the notification for foreground service"
```

### Enabling Foreground Service

After enabling Woosmap Geofencing SDK, enable the Foreground Service as follow :
```java
WoosmapSettings.foregroundLocationServiceEnable = true;
```