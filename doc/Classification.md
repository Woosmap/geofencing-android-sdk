## Classification of ZOI

The classification of areas of interest (zois) aims to assign them types, such as "home", "work", ...
The classification method chosen takes place in three stages. 

### Determining zois where the user returns recurring manner
In order to determine the zois in which the user returns weekly, we will calculate for each zoi the number of different weeks that the user has spent there.
A zoi is considered to be recurrent if the number of weeks spent indoors is greater than or equal to the average of the weeks spent in the zones.

#### Example
A user U, having three zois, z1, z2 and z3.
We consider that the user has spent a number of weeks w1, w2 and w3 in (z1, z2 and z3) where w1 = 3, w2 = 2, w3 = 4.
The average of the weeks spent in the zones is therefore M = 9/3 = 3
Zois z1 and z3 are therefore considered to be recurrent.


In order not to take zois that would be recurrent because of a passage more than a real presence, we will also apply a second filter which requires that the time spent in a zoi be greater than or equal to 5% of the time spent in all of the zois. 

### Determination of weekly and daily attendance intervals
On one week of 168 hours, we calculate the number of hours spent for each hour of the week [1,168]. We sum the hours over each week of the zoi.
So to get a daily interval by making a new sum and a new average.


### Classification of the zoi according to previous intervals
Once the daily intervals are obtained for a zone, we compare them to defined intervals (HOME, WORK ....) in order to qualify them.

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
We associate the qualification "HOME" intervals [0 - 7] (7 hours) and [19 - 24] (5 hours). So the the length is 12 hours.

For a user we have the zoi **z1** of intervals [0 - 4] (4 hours) and [21 - 24] (3 hours) and the zoi **z2** of intervals [4 - 12] (8 hours).

The size of the intersection between "HOME" and z1 is 7 hours. This value is superior of 50% of the length of "HOME" intervals so z1 is qualified as a "HOME"

The size of the intersection between "HOME" and z2 is 3 hours. This values is inferior of 50% of the length of "HOME" intervals so z2 is not qualified as a "HOME"