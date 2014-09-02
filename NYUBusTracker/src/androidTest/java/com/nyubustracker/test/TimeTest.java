package com.nyubustracker.test;

import com.nyubustracker.models.Time;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TimeTest extends TestCase{
    public void testAMConstructor() {
        Time t = new Time("12:10 AM", Time.TimeOfWeek.Weekday, "Route T");
        assertEquals("Route should be set", "Route T", t.getRoute());
        assertEquals("TimeOfWeek should be set", Time.TimeOfWeek.Weekday, t.getTimeOfWeek());
        assertEquals("Hour should be set", 0, t.getHour());
        assertEquals("Minute should be set", 10, t.getMinute());
        assertTrue("AM/PM should be set", t.isAM());

        t = new Time("1:46 AM", Time.TimeOfWeek.Weekday, "Route T");
        assertEquals("Hour should be set", 1, t.getHour());
        assertEquals("Minute should be set", 46, t.getMinute());
        assertTrue("AM/PM should be set", t.isAM());
    }

    public void testPMConstructor() {
        Time t = new Time("12:01 PM", Time.TimeOfWeek.Weekday, "Route T");
        assertEquals("Hour should be set", 12, t.getHour());
        assertEquals("Minute should be set", 1, t.getMinute());
        assertFalse("AM/PM should be set", t.isAM());

        t = new Time("6:21 PM", Time.TimeOfWeek.Weekday, "Route T");
        assertEquals("Hour should be set", 18, t.getHour());
        assertEquals("Minute should be set", 21, t.getMinute());
        assertFalse("AM/PM should be set", t.isAM());
    }

    public void testBadConstructorArgs() {
        Time t = new Time("12:01PM", Time.TimeOfWeek.Weekday, "Route T");
        assertEquals("Bad format: Hour should be set", 12, t.getHour());
        assertEquals("Bad format: Minute should be set", 1, t.getMinute());
        assertFalse("Bad format: AM/PM should be set", t.isAM());

        t = new Time("6:21       PM", Time.TimeOfWeek.Weekday, "Route T");
        assertEquals("Bad format: Hour should be set", 18, t.getHour());
        assertEquals("Bad format: Minute should be set", 21, t.getMinute());
        assertFalse("Bad format: AM/PM should be set", t.isAM());

        t = new Time("06:21       PM", Time.TimeOfWeek.Weekday, "Route T");
        assertEquals("Bad format: Hour should be set", 18, t.getHour());
        assertEquals("Bad format: Minute should be set", 21, t.getMinute());
        assertFalse("Bad format: AM/PM should be set", t.isAM());

        t = new Time("6:21       pm   ", Time.TimeOfWeek.Weekday, "Route T");
        assertEquals("Bad format: Hour should be set", 18, t.getHour());
        assertEquals("Bad format: Minute should be set", 21, t.getMinute());
        assertFalse("Bad format: AM/PM should be set", t.isAM());

        t = new Time(" 6 : 21       Pm   ", Time.TimeOfWeek.Weekday, "Route T");
        assertEquals("Bad format: Hour should be set", 18, t.getHour());
        assertEquals("Bad format: Minute should be set", 21, t.getMinute());
        assertFalse("Bad format: AM/PM should be set", t.isAM());
    }

    public void testAltConstructor() {
        Time t = new Time(0, 0);
        assertEquals("Hour should be set", 0, t.getHour());
        assertEquals("Minute should be set", 0, t.getMinute());
        assertTrue("AM/PM should be set", t.isAM());

        t = new Time(12, 0);
        assertEquals("Hour should be set", 12, t.getHour());
        assertEquals("Minute should be set", 0, t.getMinute());
        assertFalse("AM/PM should be set", t.isAM());
    }

    public void testComparatorSameDay() {
        Time t0 = new Time("12:00 AM", Time.TimeOfWeek.Weekday, "Route T");
        Time t1 = new Time("12:01 AM", Time.TimeOfWeek.Weekday, "Route T");
        Time t2 = new Time( "1:00 AM", Time.TimeOfWeek.Weekday, "Route T");
        Time t3 = new Time( "1:00 PM", Time.TimeOfWeek.Weekday, "Route T");
        assertTrue(Time.compare.compare(t0, t1) < 0);
        assertTrue(Time.compare.compare(t0, t2) < 0);
        assertTrue(Time.compare.compare(t2, t1) > 0);
        assertTrue(Time.compare.compare(t3, t1) > 0);
    }

    public void testComparatorDifferentDay() {
        Time t0 = new Time("12:00 AM", Time.TimeOfWeek.Weekday, "Route T");
        Time t1 = new Time("12:01 AM", Time.TimeOfWeek.Weekday, "Route T");
        Time t2 = new Time( "1:00 AM", Time.TimeOfWeek.Friday, "Route T");
        Time t3 = new Time( "1:00 PM", Time.TimeOfWeek.Weekend, "Route T");
        assertTrue(Time.compare.compare(t0, t1) < 0);
        assertTrue(Time.compare.compare(t0, t2) < 0);
        assertTrue(Time.compare.compare(t2, t1) > 0);
        assertTrue(Time.compare.compare(t3, t1) > 0);
        assertTrue(Time.compare.compare(t3, t2) > 0);
        assertTrue(Time.compare.compare(t2, t3) < 0);
    }

    public void testComparatorEquals() {
        Time t0 = new Time( "1:00 PM", Time.TimeOfWeek.Weekend, "Route T");
        Time t1 = new Time( "1:00 PM", Time.TimeOfWeek.Weekend, "Route T");
        assertTrue(Time.compare.compare(t0, t1) == 0);
        assertEquals(t0, t1);
    }
}
