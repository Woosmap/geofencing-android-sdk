
## Handling custom messages

  
Adding rich content like location or a mop in a notification is considered has managing custom messages.

To receive custom messages, use a service that extends FirebaseMessagingService. Your service should override the onMessageReceived and onDeletedMessages callbacks.

Messages should be handled within 20 seconds of receipt (10 seconds on Android Marshmallow). Note that the time window may be shorter depending on OS delays incurred ahead of calling onMessageReceived.

If delays is exceeded, various OS behaviors such as Android O's background execution limits may interfere with your ability to complete your work. For more information see the overview on message priority (here [https://firebase.google.com/docs/cloud-messaging/concept-options#setting-the-priority-of-a-message](https://firebase.google.com/docs/cloud-messaging/concept-options#setting-the-priority-of-a-message)).

onMessageReceived is provided for most message types, with the following exceptions:

-   Notification messages delivered when your app is in the background. In this case, the notification is delivered to the device’s system tray. A user tap on a notification opens the app launcher by default.
    
-   Messages with both notification and data payload, when received in the background. In this case, the notification is delivered to the device’s system tray, and the data payload is delivered in the extras of the intent of your launcher Activity.
    

## Override onMessageReceived

By overriding the method FirebaseMessagingService.onMessageReceived, you can perform actions based on the received [RemoteMessage](https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/RemoteMessage) object and get the message data:

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
}
```
