package com.palsulich.nyubustracker.models;

import android.util.Log;

import java.util.Comparator;

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
            if (time1.getTimeOfWeek().ordinal() == time2.getTimeOfWeek().ordinal()){    // Times at the same time in the week.
                if (time1.isStrictlyBefore(time2)){     // Checks hour and minute. Returns false if they're equal or time2 is before.
                    return -1;
                }
                if (time2.isStrictlyBefore(time1)){
                    return 1;
                }
                return 0;       // Same exact time (hour, minute, and timeOfWeek).
            }
            else if (time1.getTimeOfWeek().ordinal() < time2.getTimeOfWeek().ordinal()){    // Time1 is an earlier day.
                return -1;
            }
            else{       // Time2 is an earlier day.
                return 1;
            }
        }
    };

    public Time(String time, TimeOfWeek mTimeOfWeek, String mRoute){           // Input a string like "8:04 PM".
        AM = time.contains("AM");       // Automatically accounts for AM/PM with military time.
        hour = Integer.parseInt(time.substring(0, time.indexOf(":")).trim());
        min = Integer.parseInt(time.substring(time.indexOf(":") + 1, time.indexOf(" ")).trim());
        if (AM && hour == 12){      // It's 12:xx AM
            hour = 0;
        }
        if (!AM && hour != 12){     // Its x:xx PM, but not 12:xx PM.
            hour += 12;
        }
        timeOfWeek = mTimeOfWeek;
        route = mRoute;
    }

    // Returns a String representation of the time of week this Time is in.
    public String getTimeOfWeekAsString(){
        switch (timeOfWeek){
            case Weekday:
                return "Weekday";
            case Friday:
                return "Friday";
            case Weekend:
                return "Weekend";
        }
        Log.e("Time Debugging", "Invalid timeOfWeek");
        return "";      // Should never reach here.
    }

    public TimeOfWeek getTimeOfWeek(){
        return timeOfWeek;
    }

    // Create a new Time given a military hour and minute.
    public Time(int mHour, int mMin){
        AM = mHour < 12;
        hour = mHour;
        min = mMin;
    }

    public String getRoute(){
        return route;
    }

    // Return a nice string saying the difference between this time and the argument.
    public String getTimeAsStringUntil(Time t){
        Time difference = this.getTimeAsTimeUntil(t);
        Log.v("Time Debugging", "this: " + this.toString() + " | that: " + t.toString());
        Log.v("Time Debugging", "Difference: " + difference.hour + ":" + difference.min);
        String result = "I don't know when the next bus is!";
        if (difference != null){
            if (difference.hour == 0 && difference.min == 0)
                result = "Next bus is right now!";
            if (difference.hour == 0 && difference.min > 0)
                result = "Next bus is in " + difference.min + " minutes.";
            if (difference.hour > 0 && difference.min == 0)
                result = "Next bus is in " + difference.hour + " hours.";
            if (difference.hour > 0 && difference.min > 0)
                result = "Next bus is in " + difference.hour + " hours and " + difference.min + " minutes.";
        }
        return result;
    }

    // isStrictlyBefore(t) returns false if the times are equal or this is after t.
    public boolean isStrictlyBefore(Time t){
        //Log.v("Time Debugging", this.toString() + " is strictly before " + t.toString() + ": " + ((this.hour < t.hour) || (this.hour == t.hour && this.min < t.min)));
        return (this.hour < t.hour) || (this.hour == t.hour && this.min < t.min);
    }

    // Return a Time object who represents the difference in time between the two Times.
    public Time getTimeAsTimeUntil(Time t){
        if (this.isStrictlyBefore(t)){
            int hourDifference = t.hour - this.hour;
            int minDifference = t.min - this.min;
            if (minDifference < 0){
                hourDifference--;
                minDifference += 60;
            }
            return new Time(hourDifference, minDifference);
        }
        else{
            Log.v("Time", t.toString() + " isn't after " + this.toString());
            return new Time(0, 0);
        }
    }

    public String toString(){
        return getHourInNormalTime() + ":" + getMinInNormalTime() + " " + getAMorPM() + " via " + route;
    }

    private String getAMorPM(){
        return AM ? "AM" : "PM";
    }

    // Return this Time in 12-hour format.
    private int getHourInNormalTime(){
        if (hour == 0 && AM) return 12;
        if (hour > 0 && AM)  return hour;
        if (hour > 12 && !AM) return hour - 12;
        if (hour <= 12 && !AM) return hour;
        return hour;
    }

    // Ensure the minute string is 2 digits long.
    private String getMinInNormalTime(){
        if (min < 10) return "0" + min;
        else return Integer.toString(min);
    }
}
