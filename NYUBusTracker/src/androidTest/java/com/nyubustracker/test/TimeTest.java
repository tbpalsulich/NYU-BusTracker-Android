package com.nyubustracker.test;

import com.nyubustracker.models.Time;

import junit.framework.TestCase;

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
    }
}
