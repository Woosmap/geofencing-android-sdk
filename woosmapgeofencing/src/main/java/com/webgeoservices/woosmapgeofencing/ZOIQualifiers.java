package com.webgeoservices.woosmapgeofencing;

import android.util.ArraySet;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class ZOIQualifiers {
    List<Map> list_zois = new ArrayList<>();

    String EXIT_TYPE = "exit";
    String ENTRY_TYPE = "entry";

    Map<String, Object> PERIODS = new HashMap<>();
    List<Map> HOME_PERIOD = new ArrayList<>();
    List<Map> WORK_PERIOD = new ArrayList<>();


    void updateZoisQualifications(List<Map> zois){
        if (WoosmapSettings.classificationEnable == false) {
            return;
        }
        list_zois = zois;

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

        update_zoi_time_info();
        update_recurrent_zois_status();
    }

    private void update_recurrent_zois_status() {
        Map<String, Object> zois_qualifications = new HashMap<>();
        List<Map> list_zois_to_update = new ArrayList<>();
        Set<Integer> total_weeks_on_zois = new HashSet<>();
        long total_time_on_zois = 0;
        List<Integer> number_of_weeks_by_zois = new ArrayList<>();

        for (Iterator<Map> iter = list_zois.iterator(); iter.hasNext(); ) {
            Map<String, Object> zois_gmm_info = iter.next();
            List<Integer> weeks_zoi_list = (List<Integer>) zois_gmm_info.get("weeks_on_zoi");
            for (Iterator<Integer> weekIterator = weeks_zoi_list.iterator(); weekIterator.hasNext(); ) {
                int week = weekIterator.next();
                total_weeks_on_zois.add(week);
            }
            total_time_on_zois += ((Number) zois_gmm_info.get("duration")).longValue();
            number_of_weeks_by_zois.add(weeks_zoi_list.size());
        }

        Double number_of_weeks_on_all_zois = Double.valueOf(total_weeks_on_zois.size());
        Double[] weeks_presence_ratio = new Double[number_of_weeks_by_zois.size()];

        for (int i = 0; i < number_of_weeks_by_zois.size(); i++) {
            weeks_presence_ratio[i] = number_of_weeks_by_zois.get(i) / number_of_weeks_on_all_zois;
        }

        Double mean_weeks_presence_ratio = mean(weeks_presence_ratio);

        for (int indexZOI = 0; indexZOI < list_zois.size(); indexZOI++) {
            long time_spent_on_zoi = ((Number) list_zois.get(indexZOI).get("duration")).longValue();
            if (weeks_presence_ratio[indexZOI] >= mean_weeks_presence_ratio && (time_spent_on_zoi >= total_time_on_zois * 0.05)) {
                list_zois_to_update.add(list_zois.get(indexZOI));
                List<String> zoi_periods = qualify_recurrent_zoi(list_zois.get(indexZOI));
                for(int indexKeyPeriod = 0; indexKeyPeriod < zoi_periods.size(); indexKeyPeriod++){
                    List<Map> list_zois_in_period = (List<Map>) zois_qualifications.get(zoi_periods.get(indexKeyPeriod));
                    if(list_zois_in_period == null) {
                        list_zois_in_period = new ArrayList<>();
                    }
                    list_zois_in_period.add(list_zois.get(indexZOI));
                    zois_qualifications.put(zoi_periods.get(indexKeyPeriod),list_zois_in_period);
                }
            }
        }
    }

    private List<String> qualify_recurrent_zoi(Map<String, Object> zois_gmm_info) {
        int[] weekly_density = (int[]) zois_gmm_info.get("weekly_density");
        get_average_presence_intervals(weekly_density, zois_gmm_info);

        List<String> zoi_periods = new ArrayList<>();

        for (String key : PERIODS.keySet()) {
            long time_on_period = get_time_on_period((List<Map>) PERIODS.get(key),(List<Map>)zois_gmm_info.get("average_intervals"));
            int period_length = get_periods_length((List<Map>) PERIODS.get(key));

            // A zoi is classify as a period's type
            // if the time spent on the period is greater thant 50% or more of the total period length
            if (time_on_period >= period_length * 0.5) {
                zois_gmm_info.put("period",key);
                zoi_periods.add(key);
            }
        }

        if (zoi_periods.isEmpty()){
            zois_gmm_info.put("period","OTHER");
            zoi_periods.add("OTHER");
        }
        return zoi_periods;
    }

    public int get_periods_length(List<Map> period_segments) {
        int periods_length = 0;
        for (Iterator<Map> iter = period_segments.iterator(); iter.hasNext(); ) {
            Map<String, Object> period_segment = iter.next();
            periods_length += (int) period_segment.get("end") - (int) period_segment.get("start");
        }
        return periods_length;
    }

    public long get_time_on_period(List<Map> period_segments, List<Map>  average_intervals) {
        long time_spent_on_periods = 0;
        List<List> compact_intervals = new ArrayList<>();
        for(int index = 0; index < average_intervals.size(); index++) {
            if(index % 2 == 0){
                List<Integer> val = new ArrayList<>();
                val.add((Integer) average_intervals.get(index).get("hour"));
                val.add((Integer) average_intervals.get(index+1).get("hour"));
                compact_intervals.add(val);

            }
        }

        for (Iterator<Map> iter = period_segments.iterator(); iter.hasNext(); ) {
            Map<String, Object> period_segment = iter.next();
            for(int index = 0; index < compact_intervals.size(); index++) {
                int interval2_start = (int) ((List<Integer>) compact_intervals.get(index)).get(0);
                int interval2_end = (int) ((List<Integer>) compact_intervals.get(index)).get(1);
                time_spent_on_periods += intervals_intersection_length((int) period_segment.get("start"), (int) period_segment.get("end"),interval2_start,interval2_end);
            }
        }
        return time_spent_on_periods;
    }

    public long intervals_intersection_length(int interval1_start, int interval1_end, int interval2_start, int interval2_end) {
        // We check for intersection
        if((interval1_start <= interval2_start &&  interval2_start <= interval1_end) ||
                (interval1_start <= interval2_end && interval2_end <= interval1_end) ||
                (interval2_start <= interval1_start && interval1_start <= interval2_end) ||
                (interval2_start <= interval1_end && interval1_end <= interval2_end)) {
            return Math.min(interval1_end, interval2_end) - Math.max(interval1_start, interval2_start);
        } else {
            return 0;
        }
    }

    private void get_average_presence_intervals(int[] weekly_density, Map<String, Object> zois_gmm_info) {
        Map<String, Object> daily_presence_intervals = extract_daily_presence_intervals_from_weekly_density(weekly_density);

        if (daily_presence_intervals.size() == 0)
            return;
        int [] daily_density = new int[24];

        List<Map> last_daily_presence_interval = (List<Map>) daily_presence_intervals.values().toArray()[daily_presence_intervals.size()-1];
        Map<String, Object> previous_interval = last_daily_presence_interval.get(last_daily_presence_interval.size()-1);

        for (String key : daily_presence_intervals.keySet()) {
            List<Map> current_daily_presence_interval = (List<Map>) daily_presence_intervals.get(key);
            for (Iterator<Map> iter = current_daily_presence_interval.iterator(); iter.hasNext(); ) {
                Map<String, Object> interval = iter.next();
                if(interval.get("type") == EXIT_TYPE) {
                    int start = (int) previous_interval.get("hour");
                    int end = (int) interval.get("hour");
                    int hour = start;
                    while (hour != end) {
                        daily_density[hour] += 1;
                        hour = (hour + 1) % 24;
                    }
                }
                previous_interval = interval;
            }
        }

        Double density_mean = mean(daily_density);

        List<Map> average_intervals = new ArrayList<>();

        for(int hour = 0; hour < daily_density.length; hour++){
            boolean previous_density_status = false;
            if(hour == 0) {
                if( daily_density[daily_density.length - 1] >= density_mean) {
                    previous_density_status = true;
                }
            } else {
                if( daily_density[hour - 1] >= density_mean) {
                    previous_density_status = true;
                }
            }

            boolean current_status = (daily_density[hour] >= density_mean) ? true : false;

            if (previous_density_status != current_status) {
                String event_type = EXIT_TYPE;
                if(!previous_density_status){
                    event_type = ENTRY_TYPE;
                }
                Map<String, Object> daily_interval = new HashMap<>();
                daily_interval.put("type", event_type);
                daily_interval.put("hour", hour);
                average_intervals.add(daily_interval);
            }
        }

        for (String key : daily_presence_intervals.keySet()) {
            List<Map> current_daily_presence_interval = (List<Map>) daily_presence_intervals.get(key);
            add_first_entry_and_last_exit_to_intervals_if_needed(current_daily_presence_interval);
        }

        add_first_entry_and_last_exit_to_intervals_if_needed(average_intervals);

        zois_gmm_info.put("daily_presence_intervals", daily_presence_intervals);
        zois_gmm_info.put("average_intervals",average_intervals);

    }

    public void add_first_entry_and_last_exit_to_intervals_if_needed(List<Map> current_daily_presence_interval) {
        if(!current_daily_presence_interval.isEmpty()) {
            Map<String, Object> first_interval = current_daily_presence_interval.get(0);
            if(first_interval.get("type") == EXIT_TYPE) {
                Map<String, Object> begin_interval = new HashMap<>();
                begin_interval.put("type", ENTRY_TYPE);
                begin_interval.put("hour", 0);
                current_daily_presence_interval.add(0,begin_interval);
            }

            Map<String, Object> last_interval = current_daily_presence_interval.get(current_daily_presence_interval.size() - 1);
            if(last_interval.get("type") == ENTRY_TYPE) {
                Map<String, Object> end_interval = new HashMap<>();
                end_interval.put("type", EXIT_TYPE);
                end_interval.put("hour", 24);
                current_daily_presence_interval.add(end_interval);
            }
        }
    }

    public  Map<String, Object> extract_daily_presence_intervals_from_weekly_density(int[] weekly_density) {
        Double weekly_density_mean = mean(weekly_density);

        Map<String, Object> daily_presence_intervals = new HashMap<>();

        for (int hour = 0; hour < weekly_density.length; hour++) {
            boolean previous_density_status = false;
            if(hour == 0) {
                if( weekly_density[weekly_density.length - 1] >= weekly_density_mean) {
                    previous_density_status = true;
                }
            } else {
                if( weekly_density[hour - 1] >= weekly_density_mean) {
                    previous_density_status = true;
                }
            }

            boolean current_status = (weekly_density[hour] >= weekly_density_mean) ? true : false;

            if (previous_density_status != current_status) {

                String day_key = String.valueOf((hour - hour % 24) / 24 + 1);

                if(daily_presence_intervals.get(day_key) == null) {
                    List<Map> list_daily_presence = new ArrayList<>();
                    daily_presence_intervals.put(day_key,list_daily_presence);
                }

                String event_type = EXIT_TYPE;
                if(!previous_density_status){
                    event_type = ENTRY_TYPE;
                }

                List<Map> list_daily_presence = (List<Map>) daily_presence_intervals.get(day_key);

                Map<String, Object> daily_presence = new HashMap<>();
                daily_presence.put("type", event_type);
                daily_presence.put("hour", hour % 24);

                list_daily_presence.add(daily_presence);

                daily_presence_intervals.put(day_key,list_daily_presence);

            }
        }
        return  daily_presence_intervals;
    }

    public static Double mean(Double[] m) {
        Double sum = 0.0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i];
        }
        return sum / m.length;
    }

    public static Double mean(int[] m) {
        Double sum = 0.0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i];
        }
        return sum / m.length;
    }

    private void update_zoi_time_info() {
        for (Iterator<Map> iter = list_zois.iterator(); iter.hasNext(); ) {
            Map<String, Object> zois_gmm_info = iter.next();
            List<LoadedVisit> listVisit = (List<LoadedVisit>) zois_gmm_info.get("visitPoint");
            // Update time and weeks spent on zoi
            for (Iterator<LoadedVisit> iterVisit = listVisit.iterator(); iterVisit.hasNext(); ) {
                LoadedVisit visitPoint = iterVisit.next();
                extract_time_and_weeks_from_interval(visitPoint,zois_gmm_info);
                update_weekly_density(visitPoint,zois_gmm_info);
            }
        }
    }

    public void update_weekly_density(LoadedVisit visitPoint, Map<String, Object> zois_gmm_info) {

        long start_time = visitPoint.startime;
        long end_time = visitPoint.endtime;

        Calendar cal = GregorianCalendar.getInstance(TimeZone.getDefault());
        cal.setTimeInMillis(start_time);

        int[] weekly_density = (int[]) zois_gmm_info.get("weekly_density");

        while (start_time < end_time) {
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int day = cal.get(Calendar.DAY_OF_WEEK);
            if(day == 1) {
                day =  6;
            } else {
                day -= 2;
            }

            int current_hour = hour + day*24;

            weekly_density[current_hour] += 1;
            start_time = start_time + 3600000; //1hours
            cal.setTimeInMillis(start_time);
        }
        zois_gmm_info.put("weekly_density",weekly_density);
    }

    public void extract_time_and_weeks_from_interval(LoadedVisit visitPoint, Map zoi_gmminfo) {
        zoi_gmminfo.put("duration", ((Number) zoi_gmminfo.get("duration")).longValue() + visitPoint.endtime - visitPoint.startime);

        Date startDateTime = new Date(visitPoint.startime);
        Date endDateTime = new Date(visitPoint.endtime);

        Calendar cal = Calendar.getInstance();
        cal.setTime(startDateTime);
        int startWeekOfYear = cal.get(Calendar.WEEK_OF_YEAR);
        cal.setTime(endDateTime);
        int endWeekOfYear = cal.get(Calendar.WEEK_OF_YEAR);

        List<Integer> weeks_on_zoi = (List<Integer>) zoi_gmminfo.get("weeks_on_zoi");

        if(!weeks_on_zoi.contains(startWeekOfYear)){
            weeks_on_zoi.add(startWeekOfYear);
        }

        if(!weeks_on_zoi.contains(endWeekOfYear)){
            weeks_on_zoi.add(endWeekOfYear);
        }

        zoi_gmminfo.put("weeks_on_zoi",weeks_on_zoi);
    }
}
