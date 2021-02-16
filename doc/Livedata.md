

  
## Using Room Database with LiveData to retrieve data
  

[LiveData](https://developer.android.com/reference/android/arch/lifecycle/LiveData.html) is an observable data holder class. Unlike a regular observable, LiveData is lifecycle-aware, meaning it respects the lifecycle of other app components, such as activities, fragments, or services. This awareness ensures LiveData only updates app component observers that are in an active lifecycle state.


### Get Live Data MovingPositions

We will receive the object of`LiveData<MovingPosition []>` when you are calling the `getLiveDataMovingPositions(int limitOfPositions)` method from DAO.

Attach an observer to `getLiveDataMovingPositions` so we can observe the changes in the database.

```java
final LiveData<MovingPosition[]> movingPositionList = WoosmapDb.getInstance(getApplicationContext()).getMovingPositionsDao().getLiveDataMovingPositions(-1);  
movingPositionList.observe(this, new Observer<MovingPosition[]>() {  
    @Override  
  public void onChanged(MovingPosition[] movingPositions) {
  ...
  }
});
 
``` 

### Get Live Data Visits

We will receive the object of`LiveData<Visit []>` when you are calling the `getAllLiveStaticPositions()` method from DAO.

Attach an observer to `getAllLiveStaticPositions` so we can observe the changes in the database.

```java
final LiveData<Visit[]> VisitList = WoosmapDb.getInstance(getApplicationContext()).getVisitsDao().getAllLiveStaticPositions();  
VisitList.observe(this, new Observer<Visit[]>() {  
    @Override  
  public void onChanged(Visit[] visits) {
  ...
  }
});
 
``` 

### Get Live Data POI

We will receive the object of`LiveData<POI []>` when you are calling the `getAllLivePOIs()` method from DAO.

Attach an observer to `getAllLivePOIs` so we can observe the changes in the database.

```java
final LiveData<POI[]> POIList = WoosmapDb.getInstance(getApplicationContext()).getPOIsDAO().getAllLivePOIs();  
POIList.observe(this, new Observer<POI[]>() {  
    @Override  
  public void onChanged(POI[] pois) {  
        POIData = pois;  
  }  
});
``` 

### Get Live Data Region

We will receive the object of`LiveData<Region []>` when you are calling the `getAllLiveRegions()` method from DAO.

Attach an observer to `getAllLiveRegions` so we can observe the changes in the database.

```java
final LiveData<Region[]> regionList = WoosmapDb.getInstance( getApplicationContext() ).getRegionsDAO().getAllLiveRegions();  
regionList.observe( this, new Observer<Region[]>() {  
    @Override  
  public void onChanged(Region[] regions) {  
      ...
} );
``` 

### Get Live Data ZOI

We will receive the object of`LiveData<ZOI []>` when you are calling the `getAllLiveZois()` method from DAO.

Attach an observer to `getAllLiveZois` so we can observe the changes in the database.

```java
final LiveData<ZOI[]> zoiList = WoosmapDb.getInstance( getApplicationContext() ).getZOIsDAO().getAllLiveZois();  
zoiList.observe( this, new Observer<ZOI[]>() {  
    @Override  
  public void onChanged(ZOI[] zois) {  
	...
} );
``` 

### Get Live Data Region Logs

We will receive the object of`LiveData<RegionLog []>` when you are calling the `getAllLiveRegionLogs()` method from DAO.

Attach an observer to `getAllLiveRegionLogs` so we can observe the changes in the database.

```java
final LiveData<RegionLog[]> regionLogList = WoosmapDb.getInstance( getApplicationContext() ).getRegionLogsDAO().getAllLiveRegionLogs();  
regionLogList.observe( this, new Observer<RegionLog[]>() {  
    @Override  
  public void onChanged(RegionLog[] regionLogs) {  
       ...
} );
``` 
