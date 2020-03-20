
No need to collect a location and display a notification if it’s not the right time anymore. Verify first if the notification expedition time is not too different from the notification reception time on the mobile.

## Delay timeout

Set the out of time delay in second :

```java
// delay for outdated notification
static public int outOfTimeDelay = 300;
```

## Compare with Server Timestamp

Parse the notification payload to extract the timestamp provided by the notification server. Then compare it with the mobile local timestamp and the outOfTimeDelay you defined (the timestamp is the time in second in UTC since 1970) thanks to the following code:
```java
 if (datas.timestamp != null) {
    Long tsMobile = System.currentTimeMillis()/1000;
    Long tsServer = Long.parseLong (datas.timestamp);
    if (tsServer + WoosmapSettings.outOfTimeDelay < tsMobile) {
        Log.d(WoosmapSettings.Tags.WoosmapTag, "Timestamp is outdated");
        return;
    }
} else {
    Log.d(WoosmapSettings.Tags.WoosmapTag, "No timestamp is define in the payload");
    return;
}
  ```



