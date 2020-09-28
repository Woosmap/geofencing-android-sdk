## Geographic Data Lifecycle and import data from a CSV

### Geographic Data Lifecycle

To be in compliance with GDPR, you must erase all data about locations, visits and ZOIs beyond 30 days. The database is on the app not in the Geofencing SDK,
so its your responsability to manage the data.

In the sample App, you have an example to how manage data lifecycle. In the Settings class of the app, you have a parameter to set about the delay of duration data :
```java
// Set the Delay of Duration data
WoosmapSettings.numberOfDayDataDuration = 30;
```

When the user return to the activity, the app call the onResume() method of the SDK. The SDK check the last update of data lifecycle calling the cleanOldGeographicData method :
```java
AsyncTask.execute(new Runnable() {
    @Override
    public void run() {
        WoosmapDb.getInstance(context, true).cleanOldGeographicData(context);
    }
});
```

The method cleanOldGeographicData() save in the SharedPreferences the date of the last check for cleanning of the database.
If the date of the last check is more than one day, the database is cleanned :
```java
public void cleanOldGeographicData(final Context context) {
    SharedPreferences mPrefs = context.getSharedPreferences("WGSGeofencingPref",MODE_PRIVATE);

    long lastUpdate = mPrefs.getLong("lastUpdate", 0);
    if (lastUpdate != 0) {
        long dateNow = System.currentTimeMillis();
        long timeDiffFromNow = dateNow - lastUpdate;
        //update date if no updating since 1 day
        FigmmForVisitsCreator figmmForVisitsCreator = new FigmmForVisitsCreator(WoosmapDb.getInstance(context, true));
        if (timeDiffFromNow > 86400000) {
            figmmForVisitsCreator.deleteVisitOnZoi(dateNow - WoosmapSettings.dataDurationDelay);
            getVisitsDao().deleteVisitOlderThan(dateNow - WoosmapSettings.dataDurationDelay);
            getMovingPositionsDao().deleteMovingOlderThan(dateNow - WoosmapSettings.dataDurationDelay);
            getPOIsDAO().deletePOIOlderThan(dateNow - WoosmapSettings.dataDurationDelay);
        }
    }
    //Update date
    mPrefs.edit().putLong("lastUpdate", System.currentTimeMillis()).apply();
}
```

### Import data from a CSV

In the package of the app, 2 CSV files are included to simulate location and visits to create ZOI classified.
On the action  "Create ZOI" the app call the task "testZOITask()" to import data from the CSV "Visit_qualif.csv" which contains 637 visits.
In The task, we can import data from the CSV "Locations.csv" which contains locations and visits.