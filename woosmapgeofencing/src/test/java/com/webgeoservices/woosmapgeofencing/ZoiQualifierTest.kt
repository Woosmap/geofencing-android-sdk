package com.webgeoservices.woosmapgeofencing

import org.hamcrest.core.IsEqual
import org.junit.Assert
import org.junit.Test
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class ZoiQualifierTests{
    val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SS")

    val weekly_density_test_interval: IntArray = intArrayOf(
        0, 1, 10, 10, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 12, 12, 12, 25, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 2, 1, 2, 1, 1, 12, 18, 21, 1, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 10, 16, 1, 0)

    var EXIT_TYPE = "exit"
    var ENTRY_TYPE = "entry"

    //Classification Time ZOI
    var zoiQualifiers = ZOIQualifiers()

    @Test
    fun test_when_update_weekly_density_with_basics_date_then_update_correctly_density(){
        val mCalendar: Calendar = GregorianCalendar()
        val mTimeZone = mCalendar.timeZone
        val mGMTOffset = mTimeZone.rawOffset
        val offset = TimeUnit.HOURS.convert(mGMTOffset.toLong(), TimeUnit.MILLISECONDS)

        // Monday 12:10am
        var start_date_string = "2019-03-25 12:10:35" + "+" + "%02d".format(offset)
        // Thuesday 09:50am
        var end_date_string = "2019-03-26 09:50:35" + "+" + "%02d".format(offset)

        val start_date = formatter.parse(start_date_string).time
        val end_date = formatter.parse(end_date_string).time

        val point = LoadedVisit(2.2386777435903, 48.8323083708807, 20.0, "1", start_date, end_date)
        val zoiToTest = HashMap<String, Any>()

        val weekly_density: IntArray = IntArray(7 * 24)
        zoiToTest["weekly_density"] = weekly_density

        zoiQualifiers.update_weekly_density(point,zoiToTest)

        val expected_weekly_density: IntArray = IntArray(7 * 24)
        for (i in 0..12) {
            expected_weekly_density[i] = 0
        }
        for (i in 12..33) {
            expected_weekly_density[i] = 1
        }

        Assert.assertThat(expected_weekly_density, IsEqual.equalTo(zoiToTest["weekly_density"]))
    }
    @Test
    fun test_when_update_weekly_density_with_larges_date_then_update_correctly_density(){
        val mCalendar: Calendar = GregorianCalendar()
        val mTimeZone = mCalendar.timeZone
        val mGMTOffset = mTimeZone.rawOffset
        val offset = TimeUnit.HOURS.convert(mGMTOffset.toLong(), TimeUnit.MILLISECONDS)

        // Monday 12:10am
        var start_date_string = "2019-04-01 12:10:35" + "+" + "%02d".format(offset)
        // Thuesday 09:50am
        var end_date_string = "2019-04-09 09:50:35" + "+" + "%02d".format(offset)

        // Monday 12:10am
        val start_date = formatter.parse(start_date_string).time
        // Thuesday 09:50am
        val end_date = formatter.parse(end_date_string).time

        val point = LoadedVisit(2.2386777435903, 48.8323083708807, 20.0, "1", start_date, end_date)
        val zoiToTest = HashMap<String, Any>()

        val weekly_density: IntArray = IntArray(7 * 24)
        zoiToTest["weekly_density"] = weekly_density

        zoiQualifiers.update_weekly_density(point,zoiToTest)

        val expected_weekly_density: IntArray = IntArray(7 * 24)

        for (i in 0..12) {
            expected_weekly_density[i] = 1
        }
        for (i in 12..33) {
            expected_weekly_density[i] = 2
        }
        for (i in 34..expected_weekly_density.lastIndex) {
            expected_weekly_density[i] = 1
        }

        Assert.assertThat(expected_weekly_density, IsEqual.equalTo(zoiToTest["weekly_density"]))
    }

    @Test
    fun test_when_extract_time_and_weeks_from_interval_then_return_time_and_weeks_spent_in_interval() {
        val mCalendar: Calendar = GregorianCalendar()
        val mTimeZone = mCalendar.timeZone
        val mGMTOffset = mTimeZone.rawOffset
        val offset = TimeUnit.HOURS.convert(mGMTOffset.toLong(), TimeUnit.MILLISECONDS)

        // Monday 12:10am
        var start_date_string = "2019-04-01 12:10:35" + "+" + "%02d".format(offset)
        // Thuesday 09:50am
        var end_date_string = "2019-04-16 09:50:35" + "+" + "%02d".format(offset)

        // Monday 12:10am
        val start_date = formatter.parse(start_date_string).time
        // Thuesday 09:50am
        val end_date = formatter.parse(end_date_string).time

        val point = LoadedVisit(2.2386777435903, 48.8323083708807, 20.0, "1", start_date, end_date)
        val zoiToTest = HashMap<String, Any>()
        zoiToTest["duration"] = 0
        val weeks_on_zoi: List<Int> = ArrayList()
        zoiToTest["weeks_on_zoi"] = weeks_on_zoi

        zoiQualifiers.extract_time_and_weeks_from_interval(point,zoiToTest)

        val expected_weeks_on_zoi: MutableList<Int> = mutableListOf<Int>(14,15)

        Assert.assertThat(end_date - start_date, IsEqual.equalTo(zoiToTest["duration"]))
        Assert.assertThat(expected_weeks_on_zoi, IsEqual.equalTo(zoiToTest["weeks_on_zoi"]))
    }

    @Test
    fun test_when_get_periods_length_then_return_length(){

        val test_period: MutableList<Map<*, *>> = ArrayList()

        val firstPeriod: MutableMap<String, Any> = HashMap()
        firstPeriod["start"] = 6
        firstPeriod["end"] = 11
        val secondPeriod: MutableMap<String, Any> = HashMap()
        secondPeriod["start"] = 13
        secondPeriod["end"] = 16
        test_period.add(firstPeriod)
        test_period.add(secondPeriod)

        var period_length: Int = zoiQualifiers.get_periods_length(test_period)

        Assert.assertThat(period_length, IsEqual.equalTo(8))
    }

    @Test
    fun test_when_run_intervals_intersection_length_then_return_intersection(){
        var length = zoiQualifiers.intervals_intersection_length( 2,  8,  6,  12)
        Assert.assertThat(length, IsEqual.equalTo(2L))

        length = zoiQualifiers.intervals_intersection_length( 2,  8,  9,  10)
        Assert.assertThat(length, IsEqual.equalTo(0L))

        length = zoiQualifiers.intervals_intersection_length( 2,  8,  3,  6)
        Assert.assertThat(length, IsEqual.equalTo(3L))
    }

    @Test
    fun test_when_get_time_on_period_then_return_time(){
        val test_period: MutableList<Map<*, *>> = ArrayList()
        val test_interval: MutableList<Map<*, *>> = ArrayList()

        val firstInterval: MutableMap<String, Any> = HashMap()
        firstInterval["hour"] = 5
        firstInterval["type"] = ENTRY_TYPE
        val secondInterval: MutableMap<String, Any> = HashMap()
        secondInterval["hour"] = 10
        secondInterval["type"] = EXIT_TYPE
        val thirdInterval: MutableMap<String, Any> = HashMap()
        thirdInterval["hour"] = 12
        thirdInterval["type"] = ENTRY_TYPE
        val fourthInterval: MutableMap<String, Any> = HashMap()
        fourthInterval["hour"] = 14
        fourthInterval["type"] = EXIT_TYPE

        test_interval.add(firstInterval)
        test_interval.add(secondInterval)
        test_interval.add(thirdInterval)
        test_interval.add(fourthInterval)

        val firstPeriod: MutableMap<String, Any> = HashMap()
        firstPeriod["start"] = 6
        firstPeriod["end"] = 11
        val secondPeriod: MutableMap<String, Any> = HashMap()
        secondPeriod["start"] = 13
        secondPeriod["end"] = 16
        test_period.add(firstPeriod)
        test_period.add(secondPeriod)

        var time_on_period: Long = zoiQualifiers.get_time_on_period(test_period,test_interval)
        Assert.assertThat(time_on_period, IsEqual.equalTo(5L))
    }

    @Test
    fun test_when_add_first_entry_and_last_exit_to_intervals_if_needed_then_add_it(){
        val test_interval: MutableList<Map<*, *>> = ArrayList()

        val firstInterval: MutableMap<String, Any> = HashMap()
        firstInterval["hour"] = 22
        firstInterval["type"] = ENTRY_TYPE

        test_interval.add(firstInterval)

        zoiQualifiers.add_first_entry_and_last_exit_to_intervals_if_needed(test_interval)
        Assert.assertThat(EXIT_TYPE, IsEqual.equalTo(test_interval.last()["type"]))
        Assert.assertThat(24, IsEqual.equalTo(test_interval.last()["hour"]))

        test_interval.clear()
        firstInterval["hour"] = 22
        firstInterval["type"] = EXIT_TYPE
        test_interval.add(firstInterval)
        zoiQualifiers.add_first_entry_and_last_exit_to_intervals_if_needed(test_interval)
        Assert.assertThat(ENTRY_TYPE, IsEqual.equalTo(test_interval.first()["type"]))
        Assert.assertThat(0, IsEqual.equalTo(test_interval.first()["hour"]))

        test_interval.clear()
        zoiQualifiers.add_first_entry_and_last_exit_to_intervals_if_needed(test_interval)
        Assert.assertThat(true, IsEqual.equalTo(test_interval.isEmpty()))

        firstInterval["hour"] = 2
        firstInterval["type"] = EXIT_TYPE
        test_interval.add(firstInterval)
        val secondInterval: MutableMap<String, Any> = HashMap()
        secondInterval["hour"] = 22
        secondInterval["type"] = ENTRY_TYPE
        test_interval.add(secondInterval)
        zoiQualifiers.add_first_entry_and_last_exit_to_intervals_if_needed(test_interval)
        Assert.assertThat(EXIT_TYPE, IsEqual.equalTo(test_interval.last()["type"]))
        Assert.assertThat(24, IsEqual.equalTo(test_interval.last()["hour"]))
        Assert.assertThat(ENTRY_TYPE, IsEqual.equalTo(test_interval.first()["type"]))
        Assert.assertThat(0, IsEqual.equalTo(test_interval.first()["hour"]))
    }

    @Test
    fun test_when_extract_daily_presence_intervals_from_weekly_density_then_return_daily_intervals(){
        val daily_presence_intervals = zoiQualifiers.extract_daily_presence_intervals_from_weekly_density(weekly_density_test_interval)
        val sortedMap = daily_presence_intervals.toSortedMap(compareBy { it }).keys
        val expected_daily_presence_intervals: MutableSet<String> = mutableSetOf("1","2","4","5","7")
        Assert.assertThat(sortedMap, IsEqual.equalTo(expected_daily_presence_intervals))

        var list_daily_presence: MutableList<Map<*, *>> = ArrayList()

        list_daily_presence = daily_presence_intervals["1"] as MutableList<Map<*, *>>
        Assert.assertThat(ENTRY_TYPE, IsEqual.equalTo(list_daily_presence.get(0)["type"]))
        Assert.assertThat(2, IsEqual.equalTo(list_daily_presence.get(0)["hour"]))
        Assert.assertThat(EXIT_TYPE, IsEqual.equalTo(list_daily_presence.get(1)["type"]))
        Assert.assertThat(5, IsEqual.equalTo(list_daily_presence.get(1)["hour"]))

        list_daily_presence = daily_presence_intervals["2"] as MutableList<Map<*, *>>
        Assert.assertThat(ENTRY_TYPE, IsEqual.equalTo(list_daily_presence.get(0)["type"]))
        Assert.assertThat(0, IsEqual.equalTo(list_daily_presence.get(0)["hour"]))
        Assert.assertThat(EXIT_TYPE, IsEqual.equalTo(list_daily_presence.get(1)["type"]))
        Assert.assertThat(4, IsEqual.equalTo(list_daily_presence.get(1)["hour"]))

        list_daily_presence = daily_presence_intervals["4"] as MutableList<Map<*, *>>
        Assert.assertThat(ENTRY_TYPE, IsEqual.equalTo(list_daily_presence.get(0)["type"]))
        Assert.assertThat(22, IsEqual.equalTo(list_daily_presence.get(0)["hour"]))
        Assert.assertThat(EXIT_TYPE, IsEqual.equalTo(list_daily_presence.get(1)["type"]))
        Assert.assertThat(23, IsEqual.equalTo(list_daily_presence.get(1)["hour"]))


        list_daily_presence = daily_presence_intervals["5"] as MutableList<Map<*, *>>
        Assert.assertThat(ENTRY_TYPE, IsEqual.equalTo(list_daily_presence.get(0)["type"]))
        Assert.assertThat(0, IsEqual.equalTo(list_daily_presence.get(0)["hour"]))
        Assert.assertThat(EXIT_TYPE, IsEqual.equalTo(list_daily_presence.get(1)["type"]))
        Assert.assertThat(1, IsEqual.equalTo(list_daily_presence.get(1)["hour"]))
        Assert.assertThat(ENTRY_TYPE, IsEqual.equalTo(list_daily_presence.get(2)["type"]))
        Assert.assertThat(3, IsEqual.equalTo(list_daily_presence.get(2)["hour"]))
        Assert.assertThat(EXIT_TYPE, IsEqual.equalTo(list_daily_presence.get(3)["type"]))
        Assert.assertThat(6, IsEqual.equalTo(list_daily_presence.get(3)["hour"]))

        list_daily_presence = daily_presence_intervals["7"] as MutableList<Map<*, *>>
        Assert.assertThat(ENTRY_TYPE, IsEqual.equalTo(list_daily_presence.get(0)["type"]))
        Assert.assertThat(20, IsEqual.equalTo(list_daily_presence.get(0)["hour"]))
        Assert.assertThat(EXIT_TYPE, IsEqual.equalTo(list_daily_presence.get(1)["type"]))
        Assert.assertThat(22, IsEqual.equalTo(list_daily_presence.get(1)["hour"]))
    }

    @Test
    fun test_when_update_recurrent_zois_status_then_return_new_qualifications_and_updated_zois_id(){
        val listZoiToTest: MutableList<Map<*, *>> = ArrayList()
        val zoiHomeToTest = HashMap<String, Any>()
        val weekly_density_home_test: IntArray = intArrayOf(
                1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

        val weeks_on_zoi_home: MutableList<Int> = mutableListOf<Int>(40)
        val time_spent_on_zoi_home = 24 * 3600
        val visitPoint: List<LoadedVisit> = ArrayList()

        zoiHomeToTest["visitPoint"] = visitPoint
        zoiHomeToTest["weekly_density"] = weekly_density_home_test
        zoiHomeToTest["weeks_on_zoi"] = weeks_on_zoi_home
        zoiHomeToTest["duration"] = time_spent_on_zoi_home
        listZoiToTest.add(zoiHomeToTest)

        zoiQualifiers.updateZoisQualifications(listZoiToTest)

        Assert.assertThat("HOME_PERIOD", IsEqual.equalTo(zoiHomeToTest["period"]))

        val zoiWorkToTest = HashMap<String, Any>()
        val weekly_density_work_test: IntArray = intArrayOf(
                0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 1, 1, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val weeks_on_zoi_work: MutableList<Int> = mutableListOf<Int>(40)
        val time_spent_on_zoi_work = 11 * 3600
        zoiWorkToTest["visitPoint"] = visitPoint
        zoiWorkToTest["weekly_density"] = weekly_density_work_test
        zoiWorkToTest["weeks_on_zoi"] = weeks_on_zoi_work
        zoiWorkToTest["duration"] = time_spent_on_zoi_work
        listZoiToTest.clear()
        listZoiToTest.add(zoiWorkToTest)

        zoiQualifiers.updateZoisQualifications(listZoiToTest)

        Assert.assertThat("WORK_PERIOD", IsEqual.equalTo(zoiWorkToTest["period"]))

        val zoiOtherToTest = HashMap<String, Any>()
        val weekly_density_other_test: IntArray = intArrayOf(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

        val weeks_on_zoi_other: MutableList<Int> = mutableListOf<Int>(40)
        val time_spent_on_zoi_other = 28 * 3600

        zoiOtherToTest["visitPoint"] = visitPoint
        zoiOtherToTest["weekly_density"] = weekly_density_other_test
        zoiOtherToTest["weeks_on_zoi"] = weeks_on_zoi_other
        zoiOtherToTest["duration"] = time_spent_on_zoi_other
        listZoiToTest.clear()
        listZoiToTest.add(zoiOtherToTest)

        zoiQualifiers.updateZoisQualifications(listZoiToTest)

        Assert.assertThat("OTHER", IsEqual.equalTo(zoiOtherToTest["period"]))


    }

}
