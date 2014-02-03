package com.palsulich.nyubustracker.models;

import com.palsulich.nyubustracker.helpers.BusManager;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Locale;

public class Time {
    public enum TimeOfWeek {Weekday, Friday, Weekend}

    private int hour;           // In 24 hour (military) format.
    private int min;
    private boolean AM;         // Used for parsing the input string ("8:04 PM") => 20:04, AM = true
    private String route;       // What route this time corresponds to.
    private TimeOfWeek timeOfWeek;  // Either Weekday, Friday, Weekend.

    // compare is used to sort the list of times being checked for the "nextBusTime" in MainActivity.
    public static Comparator<Time> compare = new Comparator<Time>() {
        // Return a negative number if Time1 is before, positive number if time2 is before, and 0 otherwise.
        @Override
        public int compare(Time time1, Time time2) {
            // timeOfWeek is an enum. ordinal() returns the rank of the given TimeOfWeek.
            if (time1.getTimeOfWeek().ordinal() == time2.getTimeOfWeek().ordinal()) {    // Times at the same time in the week.
                if (time1.isStrictlyBefore(time2)) {     // Checks hour and minute. Returns false if they're equal or time2 is before.
                    return -1;
                }
                if (time2.isStrictlyBefore(time1)) {
                    return 1;
                }
                // Same exact time (hour, minute, and timeOfWeek). So, check if we're looking at the current time.
                if (time1.getRoute() == null) {
                    return -1;
                }
                if (time2.getRoute() == null) {
                    return 1;
                }
                // Times are the same, but we aren't comparing the current time.
                return 0;
            }
            else if (time1.getTimeOfWeek().ordinal() < time2.getTimeOfWeek().ordinal()) {    // Time1 is an earlier day.
                return -1;
            }
            else {       // Time2 is an earlier day.
                return 1;
            }
        }
    };

    public Time(String time, TimeOfWeek mTimeOfWeek, String mRoute) {           // Input a string like "8:04 PM".
        AM = time.contains("AM");       // Automatically accounts for AM/PM with military time.
        hour = Integer.parseInt(time.substring(0, time.indexOf(":")).trim());
        min = Integer.parseInt(time.substring(time.indexOf(":") + 1, time.indexOf(" ")).trim());
        if (AM && hour == 12) {      // It's 12:xx AM
            hour = 0;
        }
        if (!AM && hour != 12) {     // Its x:xx PM, but not 12:xx PM.
            hour += 12;
        }
        timeOfWeek = mTimeOfWeek;
        route = mRoute;
    }

    // Returns a String representation of the time of week this Time is in.
    public String getTimeOfWeekAsString() {
        switch (timeOfWeek) {
            case Weekday:
                return "Weekday";
            case Friday:
                return "Friday";
            case Weekend:
                return "Weekend";
        }
        //Log.e("Time Debugging", "Invalid timeOfWeek");
        return "";      // Should never reach here.
    }

    public TimeOfWeek getTimeOfWeek() {
        return timeOfWeek;
    }

    // Create a new Time given a military hour and minute.
    public Time(int mHour, int mMin) {
        AM = mHour < 12;
        hour = mHour;
        min = mMin;
        timeOfWeek = getCurrentTimeOfWeek();
    }

    private TimeOfWeek getCurrentTimeOfWeek() {
        Calendar rightNow = Calendar.getInstance();
        String dayOfWeek = rightNow.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
        TimeOfWeek timeOfWeek = TimeOfWeek.Weekday;
        if (dayOfWeek.equals("Saturday") || dayOfWeek.equals("Sunday"))
            timeOfWeek = TimeOfWeek.Weekend;
        else if (dayOfWeek.equals("Friday")) timeOfWeek = TimeOfWeek.Friday;
        return timeOfWeek;
    }

    public static Time getCurrentTime() {
        Calendar rightNow = Calendar.getInstance();
        return new Time(rightNow.get(Calendar.HOUR_OF_DAY), rightNow.get(Calendar.MINUTE));
    }

    public String getRoute() {
        return route;
    }

    // Return a nice string saying the difference between this time and the argument.
    public String getTimeAsStringUntil(Time t) {
        Time difference = this.getTimeAsTimeUntil(t);
        //Log.v("Time Debugging", "this: " + this.toString() + " | that: " + t.toString());
        //Log.v("Time Debugging", "Difference: " + difference.hour + ":" + difference.min);
        if (difference != null) {
            if (this.getTimeOfWeek() != t.getTimeOfWeek()) {
                BusManager.getBusManager().setOnline(false);
                return "Bus currently offline.";
            }
            if (difference.hour >= 3) {
                BusManager.getBusManager().setOnline(false);
                return "Bus currently offline.";
            }
            if (difference.hour == 0 && difference.min == 0) {
                BusManager.getBusManager().setOnline(true);
                return "Less than 1 minute";
            }
            if (difference.hour == 0 && difference.min == 1) {
                BusManager.getBusManager().setOnline(true);
                return "1 minute";
            }
            if (difference.hour == 0 && difference.min > 1) {
                BusManager.getBusManager().setOnline(true);
                return difference.min + " minutes";
            }
            if (difference.hour > 1 && difference.min == 0) {
                BusManager.getBusManager().setOnline(true);
                return difference.hour + " hours";
            }
            if (difference.hour == 1 && difference.min == 0) {
                BusManager.getBusManager().setOnline(true);
                return difference.hour + " hour";
            }
            if (difference.hour > 1 && difference.min == 1) {
                BusManager.getBusManager().setOnline(true);
                return difference.hour + " hours and " + difference.min + " minute";
            }
            if (difference.hour > 1 && difference.min > 1) {
                BusManager.getBusManager().setOnline(true);
                return difference.hour + " hours and " + difference.min + " minutes";
            }
            if (difference.hour == 1 && difference.min == 1) {
                BusManager.getBusManager().setOnline(true);
                return difference.hour + " hour and " + difference.min + " minute";
            }
            if (difference.hour == 1 && difference.min > 1) {
                BusManager.getBusManager().setOnline(true);
                return difference.hour + " hour and " + difference.min + " minutes";
            }
        }
        return "";
    }

    // isStrictlyBefore(t) returns false if the times are equal or this is after t.
    public boolean isStrictlyBefore(Time t) {
        //Log.v("Time Debugging", this.toString() + " is strictly before " + t.toString() + ": " + ((this.hour < t.hour) || (this.hour == t.hour && this.min < t.min)));
        return (this.hour < t.hour) || (this.hour == t.hour && this.min < t.min);
    }

    // Return a Time object who represents the difference in time between the two Times.
    public Time getTimeAsTimeUntil(Time t) {
        if (this.isStrictlyBefore(t)) {
            //Log.v("Time Debugging", this + " is strictly before " + t);
            int hourDifference = t.hour - this.hour;
            int minDifference = t.min - this.min;
            if (minDifference < 0) {
                hourDifference--;
                minDifference += 60;
            }
            return new Time(hourDifference, minDifference);
        }
        else {
            //Log.v("Time Debugging", t.toString() + " isn't after " + this.toString());
            return new Time(0, 0);
        }
    }

    public String toString() {
        return getHourInNormalTime() + ":" + getMinInNormalTime() + " " + getAMorPM();
    }

    private String getAMorPM() {
        return AM ? "AM" : "PM";
    }

    // Return this Time in 12-hour format.
    private int getHourInNormalTime() {
        if (hour == 0 && AM) return 12;
        if (hour > 0 && AM) return hour;
        if (hour > 12 && !AM) return hour - 12;
        if (hour <= 12 && !AM) return hour;
        return hour;
    }

    // Ensure the minute string is 2 digits long.
    private String getMinInNormalTime() {
        if (min < 10) return "0" + min;
        else return Integer.toString(min);
    }
}
