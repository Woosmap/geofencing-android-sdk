# Notification Service with Location and API calls

Get the location of user on notification to complete the payload with information from APIs.

**image**

##  Enabling Location

Android Q introduces changes to location permissions. These changes are very useful for end users because they have their most transparency and control.

Android Q introduces two changes: (1) separate permission for a background location and (2) background location reminders.

### BACKGROUND LOCATION PERMISSION
Before Android Q, there were two types permissions: ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION. These two authorizations allow access to a rental in foreground and background.

Android Q introduces a third type authorization: ACCESS_BACKGROUND_LOCATION. On Android Q, the user must grant background permissions separately from foreground permissions.

With the authorization request in the background, the user will be able to grant only authorizations in the foreground:

**image**

 - “All the time” — this means an app can access location at any time
-  “While in use” — this means an app can access location only while the app is being used
-  “Deny” — this means an app cannot access location

If a user who previously granted location permissions upgrades to Android Q, they have vested rights when upgrading location permissions in the background, depending on your app settings.
More info on the ACCESS_BACKGROUND_LOCATION in this documentation https://developer.android.com/training/location/receive-location-updates.

Android Q also introduces rental reminders in the background. if the user grants background permissions, Android displays a reminder after a few days, allowing him to modify the parameters:

**image**
 
### Manifest

We first need to add a new permission to our manifest file, this is the ACCESS_BACKGROUND_LOCATION permission. Although this is declared in the manifest, it can still be revoked at any time by the user.
```
<manifest>  
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" /> 
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.location.GPS_ENABLED_CHANGE" />
</manifest>
```

### Request Permission

Note that here we aren’t required to add the foregroundServiceType service type to our service declaration, this is because we don’t need momentary permission to run outside of our app — this background permission already gives our application the ability to do this.
As well as the above, the permission needs to be granted by the user at runtime. So before we try and access the users location from the background, we need to ensure that we have the required permission from the user to do so. We can do this by checking for the ACCESS_BACKGROUND_LOCATION permission. 
Our code for checking the permission may look something like this:

```
private void requestPermissions() {  
    boolean shouldProvideRationale =  
            ActivityCompat.shouldShowRequestPermissionRationale(this,  
  android.Manifest.permission.ACCESS_FINE_LOCATION);  
  
  // Provide an additional rationale to the user. This would happen if the user denied the  
 // request previously, but didn't check the "Don't ask again" checkbox. // Provide an additional rationale to the user. This would happen if the user denied the // request previously, but didn't check the "Don't ask again" checkbox.  if (shouldProvideRationale) {  
        Log.i("WoosmapGeofencing", "Displaying permission rationale to provide additional context.");
  ActivityCompat.requestPermissions(LocationBaseActivity.this,  
 new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},  
  REQUEST_PERMISSIONS_REQUEST_CODE);  
  } else {  
        Log.i("WoosmapGeofencing", "Requesting permission");
  // Request permission. It's possible this can be auto answered if device policy  
 // sets the permission in a given state or the user denied the permission // previously and checked "Never ask again".  ActivityCompat.requestPermissions(LocationBaseActivity.this,  
 new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},  
  REQUEST_PERMISSIONS_REQUEST_CODE);  
  }  
}
```

## Set up a Firebase Cloud Messaging 

### Set up Firebase and the FCM SDK

1.  If you haven't already,  [add Firebase to your Android project](https://firebase.google.com/docs/android/setup).
2.  In your project-level  `build.gradle`  file, make sure to include Google's Maven repository in both your  `buildscript`  and  `allprojects`  sections.
3.  Add the dependency for the Cloud Messaging Android library to your module (app-level) Gradle file (usually  `app/build.gradle`):
```
implementation 'com.google.firebase:firebase-messaging:19.0.1'
 ```

### Create a class Messaging Service

Once the library is installed, we will implement a service that will listen to notification messages sent by Firebase. For that, we create a class WoosmapMessagingService.
```
public class WoosmapMessagingService extends FirebaseMessagingService {  
	 protected Class<?> cls = null;  
	 private WoosmapMessageBuilder messageBuilder;  
	 private WoosmapMessageBuilderMaps messageBuilderMaps;  
  
	 public WoosmapMessagingService(){  
        Log.d("WoosMessage","WoosmapMessagingService");  
  }  
  
    /**  
 * Called when message is received. *  
 *  @param remoteMessage Object representing the message received from Firebase  Cloud Messaging.  
 */  @Override  
  public void onMessageReceived(RemoteMessage remoteMessage) {  
        Log.d("woosmap_mobile_sdk", "onMessageReceived");  
 if (this.cls != null) {  
         this.messageBuilder = new WoosmapMessageBuilder(this, this.cls);  
		 this.messageBuilderMaps = new WoosmapMessageBuilderMaps (this, this.cls);  
  }  
        else {  
            this.messageBuilder = new WoosmapMessageBuilder(this);  
 this.messageBuilderMaps = new WoosmapMessageBuilderMaps (this, this.cls);  
  }  
        WoosmapMessageDatas messageDatas = new WoosmapMessageDatas(remoteMessage.getData());  
	 if (messageDatas.isFromWoosmap()) {  
            this.messageBuilder.sendWoosmapNotification(messageDatas);  
	  } else if (messageDatas.isLocationRequest())  
            this.messageBuilderMaps.sendWoosmapNotification (messageDatas);  
  }  
}
```

### Edit your app manifest

Add the following to your app's manifest:

-   A service that extends  `FirebaseMessagingService`. This is required if you want to do any message handling beyond receiving notifications on apps in the background. To receive notifications in foregrounded apps, to receive data payload, to send upstream messages, and so on, you must extend this service.
```
<service android:name=".ExampleInstanceIdService">
    <intent-filter>
        <action android:name="com.google.firebase.INSTANCE_ID_EVENT" />
    </intent-filter>
</service>
<service android:name=".ExampleMessagingService">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
 ```
 
 `ExampleInstanceIdService` and `ExampleMessagingService` are your own services which have to inherit from `FirebaseInstanceIdService` and `FirebaseMessagingService`

### Access the device registration token
On initial startup of your app, the FCM SDK generates a registration token for the client app instance. If you want to target single devices or create device groups, you'll need to access this token by extending  [`FirebaseMessagingService`](https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/FirebaseMessagingService)  and overriding  `onNewToken`.

This section describes how to retrieve the token and how to monitor changes to the token. Because the token could be rotated after initial startup, you are strongly recommended to retrieve the latest updated registration token.

The registration token may change when:

-   The app deletes Instance ID
-   The app is restored on a new device
-   The user uninstalls/reinstall the app
-   The user clears app data.
 
  ```
  FirebaseInstanceId.getInstance().getInstanceId()  
	  .addOnCompleteListener(new  OnCompleteListener<InstanceIdResult>()  {
	    @Override  
	    public  void onComplete(@NonNull  Task<InstanceIdResult> task)  {
	      if  (!task.isSuccessful())  {  
		      Log.w(TAG,  "getInstanceId failed", task.getException());  
		      return;  
		  }  
		  // Get new Instance ID token  
		  String token = task.getResult().getToken();
		  // Log and toast  
		  String msg = getString(R.string.msg_token_fmt, token);
		  Log.d(TAG, msg);  Toast.makeText(MainActivity.this, msg,  				
		  Toast.LENGTH_SHORT).show();  
		 } 
});
   ```
 
 ### Monitor token generation
 The `onNewToken` callback fires whenever a new token is generated.
   ```
 /**  
 * Called if InstanceID token is updated. This may occur if the security of  
 * the previous token had been compromised. Note that this is called when the InstanceID token  
 * is initially generated so this is where you would retrieve the token.  
 */  
@Override  
public  void onNewToken(String token)  {  
		Log.d(TAG,  "Refreshed token: "  + token);  
		// If you want to send messages to this application instance or  
		// manage 	this apps subscriptions on the server side, send the  
		// Instance ID token to your app server. 
		sendRegistrationToServer(token);  
}   
   ```

#### Receive messages
Firebase notifications behave differently depending on the foreground/background state of the receiving app. If you want foregrounded apps to receive notification messages or data messages, you’ll need to write code to handle the `onMessageReceived` callback. For an explanation of the difference between notification and data messages, see [Message types](https://firebase.google.com/docs/cloud-messaging/concept-options).

#### Handling messages

To receive messages, use a service that extends FirebaseMessagingService. Your service should override the onMessageReceived and onDeletedMessages callbacks. It should handle any message within 20 seconds of receipt (10 seconds on Android Marshmallow). The time window may be shorter depending on OS delays incurred ahead of calling onMessageReceived. After that time, various OS behaviors such as Android O's background execution limits may interfere with your ability to complete your work. For more information see our overview on message priority.

onMessageReceived is provided for most message types, with the following exceptions:

 - **Notification messages delivered when your app is in the background.** In this case, the notification is delivered to the device’s system tray. A user tap on a notification opens the app launcher by default.
 - **Messages with both notification and data payload, when received in the background**. In this case, the notification is delivered to the device’s system tray, and the data payload is delivered in the extras of the intent of your launcher Activity.

#### Override  `onMessageReceived`

By overriding the method `FirebaseMessagingService.onMessageReceived`, you can perform actions based on the received [RemoteMessage](https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/RemoteMessage) object and get the message data:
   ```
public class WoosmapMessagingService extends FirebaseMessagingService {

	    protected Class<?> cls = null;
	    private WoosmapMessageBuilder messageBuilder;
	    private WoosmapMessageBuilderMaps messageBuilderMaps;

	    public WoosmapMessagingService(){
	        Log.d("WoosMessage","WoosmapMessagingService");
	    }

	    /**
	     * Called when message is received.
	     *
	     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
	     */
	    @Override
	    public void onMessageReceived(RemoteMessage remoteMessage) {
	        Log.d("woosmap_mobile_sdk", "onMessageReceived");
	        if (this.cls != null) {
	            this.messageBuilder = new WoosmapMessageBuilder(this, this.cls);
	            this.messageBuilderMaps = new WoosmapMessageBuilderMaps (this, this.cls);
	        }
	        else {
	            this.messageBuilder = new WoosmapMessageBuilder(this);
	            this.messageBuilderMaps = new WoosmapMessageBuilderMaps (this, this.cls);
	        }
	        WoosmapMessageDatas messageDatas = new WoosmapMessageDatas(remoteMessage.getData());
	        if (messageDatas.isFromWoosmap()) {
	            this.messageBuilder.sendWoosmapNotification(messageDatas);
	        } else if (messageDatas.isLocationRequest())
	            this.messageBuilderMaps.sendWoosmapNotification (messageDatas);

	    }
    }
 ```

### Check Location permissions

In the `WoosmapMessageBuilderMaps`, get the last location via the `FusedLocationProviderClient` before that check permission with using the `checkSelfPermission` method of [`ActivityCompat`](https://developer.android.com/reference/android/support/v4/app/ActivityCompat.html) or [`ContextCompat`](https://developer.android.com/reference/android/support/v4/content/ContextCompat.html).

When permission has been granted, continue as usual.

 ```
private void getLatestLocation(Context context, OnSuccessListener<Location> successListener) {  
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {  
        Log.d("WoosmapGeofencingDEBUG", "No permission");
  } else {  
        FusedLocationProviderClient locationProvider = new FusedLocationProviderClient(context);  
  locationProvider.getLastLocation().addOnSuccessListener(successListener);  
  }  
}
 ```

### Check Timestamp of the payload
To verify if the notification is too late between the time send and the time received on the mobile.

Set the out of time delay in second :

```
// delay for outdated notification  
private int outOfTimeDelay = 300;
```

Parse the payload to extract the timestamp server and compare with the timestamp local with a extratime. The timestamp is the time in second in UTC since 1970 :
```
 Long tsMobile = System.currentTimeMillis()/1000;
        if (datas.timestamp != null) {
            Long tsServer = Long.parseLong (datas.timestamp);
            /**
             * Compare Timestamp between Server and Mobile to know if the notifcation is outdated
             */
            if (tsServer + outOfTimeDelay < tsMobile) {
                Log.d("WoosmapGeofencingMessage", "Timestamp is outdated");
                return;
            }
        }
  ```
### APIs request
When the task of get last location are completes successfully, we call a Search API Woosmap to find the POI nearest of the user location. After that, we call the Google API static map to download a jpeg with the user location and the location of POI.

You must call the google api after the result of the first call SearchAPI like the code below :
  ```
getLatestLocation(this.context, new OnSuccessListener<Location> () {
            @Override
            public void onSuccess(final Location location) {
                if (location == null) {
                    Log.d("Location = ", "NULL");
                    return;
                }
                Log.d("Location = ", String.valueOf (location.getLatitude ()));

                final RequestQueue requestQueue = Volley.newRequestQueue(context);

                String urlAPI = getStoreAPIUrl(location.getLatitude, location.getLongitude)
                StringRequest stringRequest = new StringRequest (urlAPI, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Gson gson = new Gson();
                        SearchAPI data = gson.fromJson (response, SearchAPI.class);
                        String city = data.getFeatures ()[0].getProperties ().getAddress ().getCity ();
                        String zipcode = data.getFeatures ()[0].getProperties ().getAddress ().getZipcode ();
                        String distance = String.valueOf (data.getFeatures ()[0].getProperties ().getDistance ());
                        double longitudePOI = data.getFeatures ()[0].getGeometry ().getCoordinates ()[0];
                        double latitudePOI = data.getFeatures ()[0].getGeometry ().getCoordinates ()[1];

                        // Fill body message with informations from API
                        String messageBody = "city = " + city +  "\n zipcode =" + zipcode + "\n distance = " + distance;
                        mBuilder.setContentText(messageBody);
                        mBuilder.setContentTitle(messageBody);

                        // Request Google Maps Static
                        String urlGMPStatic = String.format (WoosmapSettings.Urls.GoogleMapStaticUrl,String.valueOf (location.getLatitude ()),String.valueOf (location.getLongitude ()),String.valueOf (latitudePOI),String.valueOf (longitudePOI),privateKeyGMPStatic);

                        // Retrieves an image specified by the URL, displays it in the UI.
                        ImageRequest request = new ImageRequest (urlGMPStatic,
                                new Response.Listener<Bitmap>() {
                                    @Override
                                    public void onResponse(Bitmap bitmap) {
                                        style[0] = new NotificationCompat.BigPictureStyle().bigPicture(bitmap).setSummaryText(datas.messageBody);
                                        mBuilder.setStyle(style[0]);

                                        mBuilder.setContentIntent(pendingIntent);

                                        Notification notification = mBuilder.build();
                                        mNotificationManager.notify(new Random ().nextInt(20), notification);

                                    }
                                }, 0, 0, null, null,
                                new Response.ErrorListener() {
                                    public void onErrorResponse(VolleyError error) {
                                        Log.d("WoosmapError", error.toString() + " maps.google.com");
                                    }
                                });
                        // Add ImageRequest to the RequestQueue
                        requestQueue.add(request);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Anything you want
                        Log.d("WoosmapError", error.toString() + " search API");
                    }
                });
                requestQueue.add(stringRequest);

            }
        });
   ```
Modify the body, subtitle and attachment of the content handler to show the informations from APIs.
