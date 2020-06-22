To enrich notification with location info, you’ll need to check first if the user has authorized your App to access location services.

To do so, check permission using the checkSelfPermission method of [ActivityCompat](https://developer.android.com/reference/android/support/v4/app/ActivityCompat.html) or [ContextCompat](https://developer.android.com/reference/android/support/v4/content/ContextCompat.html).

If permission has been granted, continue as usual and get the last location via the FusedLocationProviderClient in the WoosmapMessageBuilderMaps.
  
```java
private void getLatestLocation(Context context, OnSuccessListener<Location> successListener) {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        Log.d(WoosmapSettings.Tags.WoosmapTag, "No permission");
    } else {
        FusedLocationProviderClient locationProvider = new FusedLocationProviderClient(context);
        locationProvider.getLastLocation().addOnSuccessListener(successListener);
    }
}
 ```

