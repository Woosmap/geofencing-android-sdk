## Classification of ZOI

The classification of zones of interest (zois) aims to assign them types, currently "home" (where the user is supposed to live), "work" (where the user is supposed to work) or "other" (zones where the user spent time recurrently but on time periods not corresponding to classic Home or Work presence).
The classification method chosen consists of 3 steps. 

### Determining zois where the user returns recurrently
In order to determine the zois in which the user returns, we calculate for each zoi the number of different weeks that the user has spent there.
A zoi is considered to be recurrent if the number of weeks spent inside is greater than or equal to the average number of the weeks spent in all the zones.

#### Example
A user U, having three zois, z1, z2 and z3.
We consider that the user has spent time in the zones z1, z2 and z3 for a number of weeks w1, w2 and w3 where w1 = 3, w2 = 2, w3 = 4.
The average of the number of weeks spent in the zones is therefore M = 9/3 = 3.  
Zois z1 and z3 are therefore considered to be recurrent.


In order to exclude zones with short visits (passing by more than spending time in it) we also apply a second filter: the time spent in a zoi (sum of multiple visit durations) has to be greater than or equal to 5% of the time spent in all of the zois (sum of all visit durations of all zones). 

### Determination of weekly and daily attendance intervals
On one week of 168 hours, we calculate the number of times the user was within the zoi for each hour of the week. We sum the hours over the weeks for the zoi.
We determine the daily intervals by choosing the one hour time slots above the average.

### Classification of the zoi according to previous intervals
Once the daily intervals are obtained for a zone, we compare them to defined intervals (HOME, WORK, OTHER) in order to qualify them.


its interval defined like this :
```java
        Map<String, Object> firstHomePeriod = new HashMap<>();
        firstHomePeriod.put("start",0);
        firstHomePeriod.put("end",7);
        Map<String, Object> secondHomePeriod = new HashMap<>();
        secondHomePeriod.put("start",21);
        secondHomePeriod.put("end",24);
        HOME_PERIOD.add(firstHomePeriod);
        HOME_PERIOD.add(secondHomePeriod);
        PERIODS.put("HOME_PERIOD", HOME_PERIOD);

        Map<String, Object> workPeriod = new HashMap<>();
        workPeriod.put("start",9);
        workPeriod.put("end",17);
        WORK_PERIOD.add(workPeriod);
        PERIODS.put("WORK_PERIOD", WORK_PERIOD);
```
So if the size of the intersection between the daily intervals and the intervals of a qualification is more than half the size of the intervals of the qualification, we attribute the qualification to the zoi.
This method makes it possible to assign several qualifications to a zone (work and home, for example in the case of remote).
If a zoi is not qualified, we attribute it the qualification "other", since it was still determined as a periodic zoi.

#### Example
We associate the qualification "HOME" with intervals [0 - 7] (7 hours) and [19 - 24] (5 hours) (total duration of 12 hours).

For a user, we have the zoi **z1** of intervals [0 - 4] (4 hours) and [21 - 24] (3 hours) and the zoi **z2** of intervals [4 - 12] (8 hours).

The size of the intersection between "HOME" and z1 is 7 hours. This value is superior of 50% of the length of "HOME" intervals so z1 is qualified as a "HOME"

The size of the intersection between "HOME" and z2 is 3 hours. This values is inferior of 50% of the length of "HOME" intervals so z2 is not qualified as a "HOME"
