# Salesforce Marketing Cloud Integration
  
Generate contextual events from Geofencing SDK data using different event types: Geofences, POI, Visits and ZOI.

Whenever location events are generated, the Geofencing SDK will send custom events and properties to your App via a listener method. Your App can then pass them to the Salesforce Marketing Cloud.

## Retrieve Marketing Cloud events
In your class delegate, retrieve custom events data.

### Enter/Exit a monitored region

Enter eventName: `woos_geofence_entered_event`

Exit eventName: `woos_geofence_exited_event`

**Event data specification**

| Field name                       | Type   | Only if the region is a POI |
| -------------------------------- | ------ | --------------------------- |
| date                             | Datetime   |                             |
| id                               | String |                             |
| latitude                         | Double |                             |
| longitude                        | Double |                             |
| radius                           | Double |                             |
| name                             | String | X                           |
| idStore                          | String | X                           |
| city                             | String | X                           |
| zipCode                          | String | X                           |
| distance                         | String | X                           |
| country\_code                    | String | X                           |
| address                          | String | X                           |
| tags                             | String | X                           |
| types                            | String | X                           |
| user\_properties.\[field\_name\] | String | X                           |

**Callback implementation**
``` java
public class WoosMarketingCloudRegionLogReadyListener implements Woosmap.MarketingCloudRegionLogReadyListener {
        public void MarketingCloudRegionLogReadyCallback(HashMap<String, Object> dataEvent) {
            // here you can modify your event name and add your data in the dictonnary
            sendMarketingCloudEvent( dataEvent );
        }
    }
```

### POI

eventName: `woos_POI_event`

**Event data specification**

| Field name                       | Type   |
| -------------------------------- | ------ |
| date                             | Datetime   |
| name                             | String |
| idStore                          | String |
| city                             | String |
| zipCode                          | String |
| distance                         | String |
| country\_code                    | String |
| address                          | String |
| tags                             | String |
| types                            | String |
| user\_properties.\[field\_name\] | String |

**Callback implementation**
``` java
public class WoosSearchAPIReadyListener implements Woosmap.SearchAPIReadyListener {
        public void SearchAPIReadyCallback(POI poi) {
            // here you can modify your event name and add your data in the dictonnary
            sendMarketingCloudEvent( poi );
        }
    }
```

### Visits detection event

eventName: `woos_Visit_event`

**Event data specification**

| Field name    | Type     |
| ------------- | -------- |
| date          | Datetime |
| arrivalDate   | Datetime |
| departureDate | Datetime |
| id            | String   |
| latitude      | Double   |
| longitude     | Double   |

**Callback implementation**
``` java
public class WoosMarketingCloudVisitReadyListener implements Woosmap.MarketingCloudVisitReadyListener {
        public void MarketingCloudVisitReadyCallback(HashMap<String, Object> dataEvent) {
            // here you can modify your event name and add your data in the dictonnary
            sendMarketingCloudEvent( dataEvent );
        }
    }
```

## Initialize the Marketing Cloud connector
The SDK needs some input like credentials and object key to perform the API call to Salesforce Marketing Cloud API.

**Input to initialize the SFMC connector**<br/>

| Parameters                             | Description                                                                                               | Required |
| -------------------------------------- | --------------------------------------------------------------------------------------------------------- | -------- |
| authenticationBaseURI                  | Authentication Base URI                                                                                   | Required |
| restBaseURI                            | REST Base URI                                                                                             | Required |
| client\_id                             | client\_id (journey\_read and list\_and\_subscribers\_read rights are required)                           | Required |
| client\_secret                         | client\_secret (journey\_read and list\_and\_subscribers\_read rights are required)                       | Required |
| contactKey                             | The ID that uniquely identifies a subscriber/contact                                                      | Required |
| regionEnteredEventDefinitionKey        | Set the EventDefinitionKey that you want to use for the Woosmap event `woos_geofence_entered_event`       |          |
| regionExitedEventDefinitionKey         | Set the EventDefinitionKey that you want to use for the Woosmap event `woos_geofence_exited_event`        |          |
| poiEventDefinitionKey                  | Set the EventDefinitionKey that you want to use for the Woosmap event `woos_POI_event`                    |          |
| zoiClassifiedEnteredEventDefinitionKey | Set the EventDefinitionKey that you want to use for the Woosmap event `woos_zoi_classified_entered_event` |          |
| zoiClassifiedExitedEventDefinitionKey  | Set the EventDefinitionKey that you want to use for the Woosmap event `woos_zoi_classified_exited_event`  |          |
| visitEventDefinitionKey                | Set the EventDefinitionKey that you want to use for the Woosmap event `woos_Visit_event`                  |

**Initialize the connector implementation**
``` java
    HashMap<String, String> SFMCInfo = new HashMap<String, String>();

    SFMCInfo.put("authenticationBaseURI","https://mcdmfc5rbyc0pxgr4nlpqqy0j-x1.auth.marketingcloudapis.com");
    SFMCInfo.put("restBaseURI","https://mcdmfc5rbyc0pxgr4nlpqqy0j-x1.rest.marketingcloudapis.com");
    SFMCInfo.put("client_id", "xxxxxxxxxxxxxxx");
    SFMCInfo.put("client_secret", "xxxxxxxxxxxxxxx");
    SFMCInfo.put("contactKey","ID001");
    SFMCInfo.put("regionEnteredEventDefinitionKey","APIEvent-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx");
    SFMCInfo.put("regionExitedEventDefinitionKey","APIEvent-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx");

    WoosmapSettings.SFMCCredentials = SFMCInfo;
```
