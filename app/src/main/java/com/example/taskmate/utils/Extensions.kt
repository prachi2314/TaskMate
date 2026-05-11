package com.example.taskmate.utils

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Extensions.kt
 * Location: utils/Extensions.kt
 *
 * Kotlin extension functions used across the app.
 * These extend existing classes with new functionality
 * without inheriting from them.
 */

// ══════════════════════════════════════════════════════════════════
//  VIEW EXTENSIONS
// ══════════════════════════════════════════════════════════════════

/**
 * Shows this view (sets visibility to VISIBLE).
 * Usage: binding.progressBar.show()
 */
fun View.show() {
    visibility = View.VISIBLE
}

/**
 * Hides this view (sets visibility to GONE).
 * GONE removes the view from layout flow — use INVISIBLE
 * if you want to keep the space.
 * Usage: binding.progressBar.hide()
 */
fun View.hide() {
    visibility = View.GONE
}

/**
 * Makes this view invisible but keeps its space in the layout.
 * Usage: binding.tvError.invisible()
 */
fun View.invisible() {
    visibility = View.INVISIBLE
}

/**
 * Shows or hides this view based on a boolean condition.
 * Usage: binding.tvNoTasks.visibleIf(tasks.isEmpty())
 */
fun View.visibleIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

/**
 * Hides the soft keyboard.
 * Usage: binding.etEmail.hideKeyboard()
 */
fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

// ══════════════════════════════════════════════════════════════════
//  CONTEXT / FRAGMENT EXTENSIONS
// ══════════════════════════════════════════════════════════════════

/**
 * Shows a short Toast from an Activity or Fragment's Context.
 * Usage: context.toast("Task added!")
 */
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * Shows a long Toast from an Activity or Fragment's Context.
 * Usage: context.longToast("Registration failed. Please try again.")
 */
fun Context.longToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

/**
 * Shows a short Toast from a Fragment.
 * Usage (inside Fragment): toast("Task deleted!")
 */
fun Fragment.toast(message: String) {
    requireContext().toast(message)
}

// ══════════════════════════════════════════════════════════════════
//  STRING EXTENSIONS
// ══════════════════════════════════════════════════════════════════

/**
 * Returns true if this string is a valid email address.
 * Usage: if (email.isValidEmail()) { ... }
 */
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

/**
 * Returns true if this string is blank or null-safe empty.
 * Usage: if (name.isNullOrBlank()) { ... }
 */
fun String?.isNullOrBlankSafe(): Boolean {
    return isNullOrBlank()
}

/**
 * Converts a hex color string to a Color int.
 * Returns the fallback color if parsing fails.
 * Usage: val color = "#7059D0".toColorInt(Color.GRAY)
 */
fun String.toColorIntSafe(fallback: Int = Color.GRAY): Int {
    return try {
        Color.parseColor(this)
    } catch (e: IllegalArgumentException) {
        fallback
    }
}

// ══════════════════════════════════════════════════════════════════
//  LONG (TIMESTAMP) EXTENSIONS
// ══════════════════════════════════════════════════════════════════

/**
 * Formats a Unix timestamp (ms) as a display date string.
 * Example: 1712966400000L → "13 Apr 2026"
 * Usage: task.dueDate.toDisplayDate()
 */
fun Long.toDisplayDate(): String {
    if (this == 0L) return ""
    return SimpleDateFormat(Constants.DATE_FORMAT_DISPLAY, Locale.getDefault())
        .format(Date(this))
}

/**
 * Formats a Unix timestamp (ms) as a time string.
 * Example: 1712966400000L → "08:00 AM"
 * Usage: slot.startTime.toDisplayTime()
 */
fun Long.toDisplayTime(): String {
    if (this == 0L) return ""
    return SimpleDateFormat(Constants.DATE_FORMAT_TIME, Locale.getDefault())
        .format(Date(this))
}

/**
 * Formats a Unix timestamp (ms) as "YYYY-MM-DD" storage string.
 * Example: 1712966400000L → "2026-04-13"
 * Used when creating StudySession.dateString.
 */
fun Long.toStorageDate(): String {
    return SimpleDateFormat(Constants.DATE_FORMAT_STORAGE, Locale.getDefault())
        .format(Date(this))
}

/**
 * Returns true if this timestamp represents today's date.
 * Usage: if (task.dueDate.isToday()) { ... }
 */
fun Long.isToday(): Boolean {
    val taskCal  = Calendar.getInstance().apply { timeInMillis = this@isToday }
    val todayCal = Calendar.getInstance()
    return taskCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            taskCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
}

/**
 * Returns the number of days between this timestamp and today.
 * Positive = future, Negative = past, 0 = today.
 */
fun Long.daysFromToday(): Int {
    val diff = this - System.currentTimeMillis()
    return (diff / (1000 * 60 * 60 * 24)).toInt()
}

// ══════════════════════════════════════════════════════════════════
//  INT EXTENSIONS
// ══════════════════════════════════════════════════════════════════

/**
 * Converts minutes from midnight to a display time string.
 * Example: 480 → "8:00 AM", 810 → "1:30 PM"
 * Used for ScheduleSlot.startTimeMinutes display.
 */
fun Int.minutesToTimeLabel(): String {
    val hours   = this / 60
    val minutes = this % 60
    val amPm    = if (hours < 12) "AM" else "PM"
    val displayHour = when {
        hours == 0  -> 12
        hours <= 12 -> hours
        else        -> hours - 12
    }
    return if (minutes == 0) "$displayHour $amPm"
    else "$displayHour:${minutes.toString().padStart(2, '0')} $amPm"
}

/**
 * Converts total minutes to a readable duration string.
 * Example: 90 → "90 min", 120 → "2 hrs", 150 → "2 hr 30 min"
 */
fun Int.toDurationLabel(): String {
    return when {
        this == 0         -> ""
        this < 60         -> "$this min"
        this == 60        -> "1 hr"
        this % 60 == 0    -> "${this / 60} hrs"
        else              -> "${this / 60} hr ${this % 60} min"
    }
}