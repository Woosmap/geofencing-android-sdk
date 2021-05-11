  
## Overview  

### Localtion on Android

Location management by Android varied a lot in the successive previous versions. Goals remain the same: bring more control and knowledge to users on behaviour of app with their locations data.  

### Location updates in Android Q  
  
Android Q introduced changes to location permissions. These changes are very useful for end users, providing them more control and transparency over location data usages.  
  
Android Q provides two major changes:  
- separate permission for a background location  
- background location reminders.  
  
#### Background Location Permission  
Before Android Q, there were two types of permissions: ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION. These two authorizations allow access to location in foreground and background.  
  
Android Q introduces a third type authorization: ACCESS_BACKGROUND_LOCATION. On Android Q, the user must grant background permissions separately from foreground permissions.  
  
But, when granting access for location in background, the user also automatically grants access to location in the foreground.  
  
Here are the 3 different choices given to users:  
-   “All the time” — this means an app can access location at any time  
-   “While in use” — this means an app can access location only while the app is being used  
-   “Deny” — this means an app cannot access location  
      
If a user who previously granted location permissions upgrades to Android Q, some permissions are inherited when upgrading location permissions in the background, depending on your app settings. More info on the ACCESS_BACKGROUND_LOCATION in this documentation [https://developer.android.com/training/location/receive-location-updates](https://developer.android.com/training/location/receive-location-updates).  
  
Android Q also introduced location reminders in the background. If the user grants background permissions, Android displays a reminder after a few days, allowing him to modify the parameters  
  
### Location updates in Android 11  
 
Android 11 evolves even more on requesting for background location and restricts a bit its usage.
  
#### One-time access  
The system permissions dialog includes an option giving users more control over when an app can access location information.  
  
<p align="center">  
  <img alt="PermissionOneTime" src="https://github.com/woosmap/woosmap-geofencing-android-sdk/raw/master/assets/PermissionOneTime.png" width="30%">  
</p>  
  
Your app can then access the related data for a period of time that depends on your app's behavior and the user's actions:  
  
While your app's activity is visible, your app can access the data.  
-   If the user sends your app to the background, your app can continue to access the data for a short period of time.  
-   If you launch a foreground service while the activity is visible, and the user then moves your app to the background, your app can continue to access the data until that foreground service stops.  
-   If the user revokes the one-time permission, such as in system settings, your app cannot access the data, regardless of whether you launched a foreground service. As with any permission, if the user revokes your app's one-time permission, your app's process terminates.  
When the user next opens your app and a feature in your app requests access to location, the user is prompted for the permission again.  

#### Request background location
When a feature in your app requests background location on a device that runs Android 10 (API level 29), the system permissions dialog includes an option named  **Allow all the time**. If the user selects this option, the feature in your app gains background location access.

On Android 11 (API level 30) and higher, however, the system dialog doesn't include the  **Allow all the time**  option. Instead, users must enable background location on the settings page.

<p align="center">  
  <img alt="Settings" src="https://github.com/woosmap/woosmap-geofencing-android-sdk/raw/master/assets/Settings.png" width="30%">  
</p>  

As long as the user does not enable the background location in the settings, your app will not access to sufficient location to build geographic behaviour profiles (no Visits or ZOI processed by the Geofencing SDK). If you want to exploit the full scope of the SDK you'll need to be sure to well inform your users about enabling background collection and what is the added value for them (e.g. fraud detection in banking apps, special offers based on their location for retail, ...).  
In addition to this, you'll need to give them the opportunity from time to time to enable background collection if they did not consent the first time. This last point means checking regularly location permissions when the app is in use.

  
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

### Android 11 Manage the permissions result

You can help users navigate to this settings page when requesting the background location permission. You can show a SnackBar to navigate to the Settings page of the app :

<p align="center">  
  <img alt="SnackBarSettings" src="https://github.com/woosmap/woosmap-geofencing-android-sdk/raw/master/assets/SnackBarSettings.png" width="30%">  
</p>  

The following code snippet shows how to show the SnackBar in the permissions Result:

```java  
@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                       @NonNull int[] grantResults) {
    Log.i("WoosmapGeofencing", "onRequestPermissionResult");
    if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
        if (grantResults.length <= 0) {
            // If user interaction was interrupted, the permission request is cancelled and you
            // receive empty arrays.
            Log.i("WoosmapGeofencing", "User interaction was cancelled.");
        } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i("WoosmapGeofencing", "Permission granted, updates requested, starting location updates");
        } else {
            // Permission denied.

            // Notify the user via a SnackBar that they have rejected a core permission for the
            // app, which makes the Activity useless. In a real app, core permissions would
            // typically be best requested during a welcome-screen flow.

            // Additionally, it is important to remember that a permission might have been
            // rejected without asking the user for permission (device policy or "Never ask
            // again" prompts). Therefore, a user interface affordance is typically implemented
            // when permissions are denied. Otherwise, your app could appear unresponsive to
            // touches or interactions which have required permissions.
            showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",
                                    BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    });
        }
    }
}
```

## Refresh Location with high frequency

Some use cases or situations are more demanding on position collection or geofence detection. To complete standard mode the SDK provides a high frequency location mode. This mode allows to retrieve a maximum of location when activated. It also assures a more accurate detection of geofence crossings. This mode can be set with:
```java
WoosmapSettings.modeHighFrequencyLocation = true
```

When activated, locations are updated every seconds but for battery and performance optimisation reasons, no POI detection, Distance calculations or Zone Classification are done over the collected locations.
This mode can be used in background or even when the app has been killed. Nevetheless, due to limitation since Android 8.0, if you want to maintain a high location collection frequency in background we recommend to use a [Foreground Service](https://github.com/woosmap/woosmap-geofencing-android-sdk/blob/master/doc/ForegroundService.md). 
Because the High Frequency Location mode generates high battery consumption, it should be used for proper use cases, when user is well aware of the tracking and be turned off as soon as high frequent positioning is not usefull anymore.
