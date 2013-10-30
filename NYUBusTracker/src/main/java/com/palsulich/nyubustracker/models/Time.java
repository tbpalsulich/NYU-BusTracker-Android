package com.palsulich.nyubustracker.models;

/**
 * Created by tyler on 10/30/13.
 */
public class Time {
    int hour;
    int min;
    boolean AM;

    public Time(int mHour, int mMin, boolean mAM){
        hour = mHour;
        min = mMin;
        AM = mAM;
        adjustForAM(AM);
    }
    public Time(String time){
        boolean AM = time.contains("AM");
        hour = Integer.parseInt(time.substring(0, time.indexOf(":")).trim());
        min = Integer.parseInt(time.substring(time.indexOf(":") + 1, time.indexOf(" ")).trim());
        adjustForAM(AM);
    }

    private void adjustForAM(boolean AM){
        if (AM && hour == 12){
            hour = 0;
        }
        if (!AM && hour > 12){
            hour += 12;
        }
    }

    public String getTimeAsStringUntil(Time t){
        Time difference = this.getTimeAsTimeUntil(t);
        if (difference != null){
            String result = "";
            if (difference.hour == 0 && difference.min == 0)
                result = "Next bus is right now!";
            if (difference.hour == 0 && difference.min > 0)
                result = "Next bus is in " + difference.min + " minutes.";
            if (difference.hour > 0 && difference.min == 0)
                result = "Next bus is in " + difference.hour + " hours.";
            if (difference.hour > 0 && difference.min > 0)
                result = "Next bus is in " + difference.hour + " hours and " + difference.min + " minutes.";

            return result;
        }
        else return "";
    }

    private boolean isBefore(Time t){
        if (this.hour < t.hour) return true;
        if (this.hour == t.hour && this.min < t.min) return true;
        return false;   // if (hour > t.hour)
    }

    private Time getTimeAsTimeUntil(Time t){
        if (t.isBefore(this)){
            int hourDifference = t.hour - this.hour;
            int minDifference = t.min - this.min;
            return new Time(hourDifference, minDifference, true);
        }
        else return null;
    }

    public String toString(){
        return this.hour + ":" + 
    }
}
