package com.example.medialert.utils; // Adjust package name as needed

import java.util.Calendar;
import java.util.TimeZone;

public class AlarmSchedulerUtil {

    /**
     * Calculates the next exact trigger time for a daily alarm based on a time-of-day
     * stored in GMT+08:00 format. If the calculated time for today is in the past,
     * it shifts the trigger time to the next day.
     *
     * @param timeOfDayInMillisGmt08 The alarm time of day in milliseconds,
     * relative to 1970-01-01 00:00:00 GMT+08:00.
     * @return The next valid trigger time in milliseconds since epoch,
     * adjusted for future occurrence if the current day's time has passed.
     */
    public static long calculateNextAlarmTimeMillis(long timeOfDayInMillisGmt08) {
        // Step 1: Get the current time, anchored to GMT+08:00 for consistent comparison
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00"));

        // Step 2: Create a Calendar instance representing the stored alarm time component
        // This is necessary to extract hour and minute correctly from timeOfDayInMillisGmt08
        Calendar storedTime = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00"));
        storedTime.setTimeInMillis(timeOfDayInMillisGmt08);

        // Step 3: Create a Calendar for today's date at the alarm's stored hour and minute, also in GMT+08:00
        Calendar alarmTimeForToday = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00"));
        alarmTimeForToday.set(Calendar.YEAR, now.get(Calendar.YEAR));
        alarmTimeForToday.set(Calendar.MONTH, now.get(Calendar.MONTH));
        alarmTimeForToday.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
        alarmTimeForToday.set(Calendar.HOUR_OF_DAY, storedTime.get(Calendar.HOUR_OF_DAY));
        alarmTimeForToday.set(Calendar.MINUTE, storedTime.get(Calendar.MINUTE));
        alarmTimeForToday.set(Calendar.SECOND, 0);
        alarmTimeForToday.set(Calendar.MILLISECOND, 0);

        // Step 4: Check if the calculated alarm time for today is in the past
        // If it is, schedule it for the next day
        if (alarmTimeForToday.getTimeInMillis() <= now.getTimeInMillis()) {
            alarmTimeForToday.add(Calendar.DAY_OF_MONTH, 1); // Add one day
        }

        // Return the final trigger time
        return alarmTimeForToday.getTimeInMillis();
    }
}