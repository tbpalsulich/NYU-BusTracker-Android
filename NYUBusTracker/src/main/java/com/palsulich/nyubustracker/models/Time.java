package com.palsulich.nyubustracker.models;

import android.util.Log;

public class Time {
    public enum TimeOfWeek {Weekday, Friday, Weekend}
    private int hour;           // In 24 hour (military) format.
    private int min;
    private boolean AM;
    private String route;
    private TimeOfWeek timeOfWeek;
    private String separation;

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

    public boolean isSeparation(){
        return separation != null;
    }

    public String getTimeOfWeekAsString(){
        switch (timeOfWeek){
            case Weekday:
                return "Weekday";
            case Friday:
                return "Friday";
            case Weekend:
                return "Weekend";
        }
        return "";
    }

    public TimeOfWeek getTimeOfWeek(){
        return timeOfWeek;
    }

    public Time(int mHour, int mMin){       // Input values in normal time (e.g. (4, 15)
        AM = mHour < 12;
        hour = mHour;
        min = mMin;
    }

    public String getRoute(){
        return route;
    }

    public String getViaRoute(){
        return " via Route " + route;
    }

    // Return a nice string saying the difference between this time and the argument.
    public String getTimeAsStringUntil(Time t){
        Time difference = this.getTimeAsTimeUntil(t);
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

    // Return if this time is equal to or before Time t.
    public boolean isBefore(Time t){
        //Log.v("Time Debugging", this.toString() + " is before " + t.toString() + ": " + ((this.hour < t.hour) || (this.hour == t.hour && this.min <= t.min)));
        return (this.hour < t.hour) || (this.hour == t.hour && this.min <= t.min);
    }

    public boolean isStrictlyBefore(Time t){
        //Log.v("Time Debugging", this.toString() + " is strictly before " + t.toString() + ": " + ((this.hour < t.hour) || (this.hour == t.hour && this.min < t.min)));
        return (this.hour < t.hour) || (this.hour == t.hour && this.min < t.min);
    }

    public boolean isAfter(Time t){
        //Log.v("Time Debugging", this.toString() + " is after " + t.toString() + ": " + ((this.hour > t.hour) || (this.hour == t.hour && this.min >= t.min)));
        return (this.hour > t.hour) || (this.hour == t.hour && this.min >= t.min);
    }

    // Return a Time object who represents the difference in time between the two Times.
    public Time getTimeAsTimeUntil(Time t){
        if (t.isAfter(this)){
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
        if (separation == null) return getHourInNormalTime() + ":" + getMinInNormalTime() + " " + getAMorPM() + getViaRoute();
        else return separation;
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

    private String getMinInNormalTime(){
        if (min < 10) return "0" + min;
        else return Integer.toString(min);
    }
}
