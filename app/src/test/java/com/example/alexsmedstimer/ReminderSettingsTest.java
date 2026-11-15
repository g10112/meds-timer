package com.example.alexsmedstimer;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReminderSettingsTest {
    @Test public void defaultsMatchPublishedSchedule() {
        assertArrayEquals(new long[]{210L, 420L, 630L}, ReminderSettings.getDefaultMinutes());
        assertTrue(ReminderSettings.isValidDelayMinutes(ReminderSettings.getDefaultMinutes()));
    }

    @Test public void rejectsInvalidAndUnorderedDelays() {
        assertFalse(ReminderSettings.isValidDelayMinutes(null));
        assertFalse(ReminderSettings.isValidDelayMinutes(new long[]{210L, 420L}));
        assertFalse(ReminderSettings.isValidDelayMinutes(new long[]{0L, 420L, 630L}));
        assertFalse(ReminderSettings.isValidDelayMinutes(new long[]{-1L, 420L, 630L}));
        assertFalse(ReminderSettings.isValidDelayMinutes(new long[]{420L, 210L, 630L}));
        assertFalse(ReminderSettings.isValidDelayMinutes(new long[]{210L, 210L, 630L}));
        assertFalse(ReminderSettings.isValidDelayMinutes(new long[]{210L, 420L, 2881L}));
    }

    @Test public void acceptsUpperBoundAndFormatsIntervals() {
        assertTrue(ReminderSettings.isValidDelayMinutes(new long[]{1L, 1440L, 2880L}));
        assertEquals("1 minute", ReminderSettings.formatDelayMinutes(1L));
        assertEquals("3 hours 30 minutes", ReminderSettings.formatDelayMinutes(210L));
        assertEquals("7 hours", ReminderSettings.formatDelayMinutes(420L));
    }

    @Test public void acceptsOnlyExplicitClockDurationInput() {
        assertEquals(210L, ReminderSettings.parseClockDuration("3:30"));
        assertEquals(420L, ReminderSettings.parseClockDuration("7:00"));
        assertEquals(2880L, ReminderSettings.parseClockDuration("48:00"));
        assertEquals(-1L, ReminderSettings.parseClockDuration(null));
        assertEquals(-1L, ReminderSettings.parseClockDuration("3h30"));
        assertEquals(-1L, ReminderSettings.parseClockDuration("3:60"));
        assertEquals(-1L, ReminderSettings.parseClockDuration("100:00"));
        assertEquals("10:30", ReminderSettings.formatClockDuration(630L));
    }
}
