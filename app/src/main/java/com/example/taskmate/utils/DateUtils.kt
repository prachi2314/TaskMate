package com.example.taskmate.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * DateUtils.kt
 * Location: utils/DateUtils.kt
 *
 * Utility functions for date calculations used across
 * the Dashboard, Progress, and Timetable screens.
 */
object DateUtils {

    /**
     * Returns the greeting based on the current hour.
     * Used in DashboardFragment header.
     *
     * 0–11  → "Good morning"
     * 12–16 → "Good afternoon"
     * 17–23 → "Good evening"
     */
    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning,"
            hour < 17 -> "Good afternoon,"
            else      -> "Good evening,"
        }
    }

    /**
     * Returns the start of today as a Unix timestamp (ms).
     * Example: if now is 2026-04-13 14:30, returns 2026-04-13 00:00:00
     * Used for querying today's tasks and sessions from Firestore.
     */
    fun startOfToday(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Returns the end of today as a Unix timestamp (ms).
     * Example: if now is 2026-04-13, returns 2026-04-13 23:59:59.999
     */
    fun endOfToday(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    /**
     * Returns the start of the current week (Monday 00:00:00).
     * Used for calculating weekly study hours.
     */
    fun startOfCurrentWeek(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Formats a timestamp as "13 Apr" (day + short month).
     * Used for the date badge in item_exam.xml.
     */
    fun formatDayMonth(timestamp: Long): String {
        if (timestamp == 0L) return ""
        return SimpleDateFormat("dd MMM", Locale.getDefault())
            .format(Date(timestamp))
    }

    /**
     * Formats a timestamp as "Apr 2026" (month + year).
     * Used in the Progress screen header subtitle.
     */
    fun formatMonthYear(timestamp: Long): String {
        return SimpleDateFormat("MMM yyyy", Locale.getDefault())
            .format(Date(timestamp))
    }

    /**
     * Returns how many days from today until a given timestamp.
     * Positive = future date, 0 = today, Negative = past date.
     */
    fun daysUntil(timestamp: Long): Int {
        val diff = timestamp - System.currentTimeMillis()
        return (diff / (1000L * 60 * 60 * 24)).toInt()
    }

    /**
     * Returns the current day of week as an index.
     * Monday = 0, Tuesday = 1, ... Sunday = 6
     * Matches ScheduleSlot.dayOfWeek indexing.
     */
    fun currentDayIndex(): Int {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2 ... Saturday=7
        // Our index:           Monday=0, Tuesday=1 ... Sunday=6
        return (day - 2 + 7) % 7
    }

    /**
     * Returns a formatted week range label.
     * Example: "Week of Apr 7 – 13, 2026"
     * Used in TimetableFragment header subtitle.
     */
    fun currentWeekLabel(): String {
        val cal = Calendar.getInstance()

        // Find Monday
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        val startDay = cal.get(Calendar.DAY_OF_MONTH)
        val month = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)

        // Find Sunday
        cal.add(Calendar.DAY_OF_MONTH, 6)
        val endDay = cal.get(Calendar.DAY_OF_MONTH)
        val year = cal.get(Calendar.YEAR)

        return "Week of $month $startDay – $endDay, $year"
    }

    /**
     * Converts study minutes to a readable label.
     * Example: 260 → "4h 20m", 60 → "1h", 45 → "45m"
     * Used in the Dashboard "Today" stat card.
     */
    fun minutesToHoursLabel(minutes: Int): String {
        return when {
            minutes == 0   -> "0m"
            minutes < 60   -> "${minutes}m"
            minutes % 60 == 0 -> "${minutes / 60}h"
            else           -> "${minutes / 60}h ${minutes % 60}m"
        }
    }
}