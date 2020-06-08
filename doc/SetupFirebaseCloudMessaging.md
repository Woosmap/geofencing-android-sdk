Sending notification to mobile app means using a dedicated service to do so: Firebase.
Let’s go through the settings associated to Firebase you’ll need to implement in your app.

## Set up Firebase and the FCM SDK

1.  If you haven't already,  [add Firebase to your Android project](https://firebase.google.com/docs/android/setup).
2.  In your project-level  `build.gradle`  file, make sure to include Google's Maven repository in both your  `buildscript`  and  `allprojects`  sections.
3.  Add the dependency for the Cloud Messaging Android library to your module (app-level) Gradle file (usually  `app/build.gradle`):
```
implementation 'com.google.firebase:firebase-messaging:19.0.1'
```

## Create a class Messaging Service

Once the library is installed, you will implement a service that will listen to notification messages sent by Firebase. For that, create a class WoosmapMessagingService.
```java
public class WoosmapMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        WoosmapMessageBuilderMaps messageBuilder = new WoosmapMessageBuilderMaps(this, MainActivity.class);
        WoosmapMessageDatas messageDatas = new WoosmapMessageDatas(remoteMessage.getData());
        if (messageDatas.isLocationRequest () && messageDatas.timestamp != null) {
            messageBuilder.sendWoosmapNotification(messageDatas);
        }
    }
```

## Edit your app manifest

Once created, WoosmapMessagingService has to be added to you manifest to extend the FirebaseMessagingService. Doing so you will be able to handle notifications in the background and the foreground with custom payloads.

```xml
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

`ExampleInstanceIdService` and `ExampleMessagingService` are your own services which have to inherit from `FirebaseInstanceIdService` and `FirebaseMessagingService`.

## Access the device registration token

On initial startup of your app, the FCM SDK generates a registration token for the client app instance. If you want to target single devices or create device groups, you'll need to access this token by extending  [`FirebaseMessagingService`](https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/FirebaseMessagingService)  and overriding  `onNewToken`.

This section describes how to retrieve the token and how to monitor changes to the token. Because the token could be modified after initial startup, you are strongly recommended to retrieve the latest updated registration token.

The registration token may change when:

-   The app deletes Instance ID
-   The app is restored on a new device
-   The user uninstalls/reinstalls the app
-   The user clears app data.
 
```java
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
## Monitor token generation

The `onNewToken` callback fires whenever a new token is generated.
```java
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

## Receive messages

Firebase notifications behave differently depending on the foreground/background state of the receiving app. If you want foregrounded apps to receive notification messages or data messages, you’ll need to write code to handle the `onMessageReceived` callback. For an explanation of the difference between notification and data messages, see [Message types](https://firebase.google.com/docs/cloud-messaging/concept-options).

## Customize notifications

If you want to define the Activity which will be opened we a user clicks on the notification, you first have to set an Uri to this Activity in the Manifest.xml (example for the uri: `sample://notif` on the MainActivity)

```xml
<activity android:name=".MainActivity">
    ...
    <intent-filter>
        ...
        <data android:scheme="sample"
            android:host="notif" />
    </intent-filter>
</activity>
```

Finally you have to define the default Uri which will be opened when the user clicks on the notification in the tag `application` 

```xml
<application ... >
...
    <meta-data android:name="woosmap_notification_defautl_uri" android:value="sample://notif" />
...
</application>
```

### Define the notification's icon
If you want to customize the small icon displayed in the notification, you must add the icon file in the directory `res/drawable` and add the following meta-data in the `AndroidManifest.xml``

```xml
<application ...>
...
    <meta-data
android:name="woosmap.messaging.default_notification_icon"
android:resource="@drawable/your_custom_icon_24dp" />
...
</application>
```
