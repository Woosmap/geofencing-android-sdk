
## Overview

Android Q introduces changes to location permissions. These changes are very useful for end users, providing them more control and transparency over location data usages.

Android Q two major changes:
- separate permission for a background location
- background location reminders.

## Background Location Permission
Before Android Q, there were two types of permissions: ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION. These two authorizations allow access to location in foreground and background.

Android Q introduces a third type authorization: ACCESS_BACKGROUND_LOCATION. On Android Q, the user must grant background permissions separately from foreground permissions.

But, when granting access for location in background, the user also automatically grants access to location in the foreground.

Here are the 3 different choices given to users:
-   “All the time” — this means an app can access location at any time
-   “While in use” — this means an app can access location only while the app is being used
-   “Deny” — this means an app cannot access location
    
If a user who previously granted location permissions upgrades to Android Q, some permissions are inherited when upgrading location permissions in the background, depending on your app settings. More info on the ACCESS_BACKGROUND_LOCATION in this documentation [https://developer.android.com/training/location/receive-location-updates](https://developer.android.com/training/location/receive-location-updates).

Android Q also introduces location reminders in the background. If the user grants background permissions, Android displays a reminder after a few days, allowing him to modify the parameters

## Manifest
You first need to add a new permission to your manifest file, this is the ACCESS_BACKGROUND_LOCATION permission. Although this is declared in the manifest, it can still be revoked at any time by the user.
```xml
<manifest>
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.location.GPS_ENABLED_CHANGE" />
</manifest>
```

## Request Permission

Note that there is no need to add ForegroundServiceType to your service declaration. Indeed momentary permission is of no use to run outside of your app — the background permission defined previously already gives your application the ability to do so.

Keep in mind that permission needs to be granted by the user at runtime. So before trying accessing the users location from the background, you need to ensure that you have the required permission from the user to do so. You can do this by checking for the ACCESS_BACKGROUND_LOCATION permission. Your code for checking the permission may look something like this:

```java
private void requestPermissions() {
     boolean shouldProvideRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION);

    // Provide an additional rationale to the user. This would happen if the user denied the
    // request previously, but didn't check the "Don't ask again" checkbox.
    // Provide an additional rationale to the user. This would happen if the user denied the
    // request previously, but didn't check the "Don't ask again" checkbox.
    if (shouldProvideRationale) {
        Log.i("WoosmapGeofencing", "Displaying permission rationale to provide additional context.");
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    } else {
        Log.i("WoosmapGeofencing", "Requesting permission");
        // Request permission. It's possible this can be auto answered if device policy
        // sets the permission in a given state or the user denied the permission
        // previously and checked "Never ask again".
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }
}
```
