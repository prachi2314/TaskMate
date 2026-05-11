package com.example.taskmate.data.model

/**
 * ScheduleSlot.kt
 * Location: data/model/ScheduleSlot.kt
 *
 * Represents one time slot in the weekly timetable.
 *
 * Firestore path:
 *   users/{userId}/schedule/{slotId}
 *
 * Each slot has:
 *   - A day of week (0=Monday to 6=Sunday)
 *   - A start time and duration
 *   - A subject link
 *   - A slot type (class / self-study / break / exam)
 */
data class ScheduleSlot(

    // Firestore document ID
    val id: String = "",

    // Owner's Firebase Auth UID
    val userId: String = "",

    // Day of week: 0=Monday, 1=Tuesday ... 6=Sunday
    // Matches the index of the day chip in TimetableFragment
    val dayOfWeek: Int = 0,

    // Start time stored as minutes from midnight
    // Example: 8:00 AM = 480, 1:30 PM = 810
    // Using minutes makes sorting and comparison simple
    val startTimeMinutes: Int = 0,

    // Duration of this slot in minutes
    // Example: 60 for a 1-hour class, 90 for a lab session
    val durationMinutes: Int = 60,

    // Subject this slot belongs to
    val subjectId: String = "",

    // Subject name — stored for display without extra lookup
    val subjectName: String = "",

    // Subject color hex — used for the left accent bar
    // and card background tint in item_schedule_slot.xml
    val subjectColorHex: String = "#7059D0",

    // Room or location
    // Example: "Room 204", "Lab A", "Online"
    val location: String = "",

    // Slot type — drives the card style in the adapter
    // Options: CLASS, SELF_STUDY, BREAK, EXAM
    val type: SlotType = SlotType.CLASS,

    // Unix timestamp when this slot was added
    val createdAt: Long = System.currentTimeMillis()

) {
    // ── Computed properties (NOT stored in Firestore) ──────────────────

    /**
     * Returns the start time as a formatted string.
     * Examples: 480 → "8:00 AM", 810 → "1:30 PM", 900 → "3:00 PM"
     * Used as the time label in item_schedule_slot.xml.
     */
    val startTimeLabel: String
        get() {
            val hours   = startTimeMinutes / 60
            val minutes = startTimeMinutes % 60
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
     * Returns the end time as a formatted string.
     * Calculated from startTimeMinutes + durationMinutes.
     * Example: start=480, duration=90 → "9:30 AM"
     */
    val endTimeLabel: String
        get() {
            val endMinutes = startTimeMinutes + durationMinutes
            val hours      = endMinutes / 60
            val minutes    = endMinutes % 60
            val amPm       = if (hours < 12) "AM" else "PM"
            val displayHour = when {
                hours == 0  -> 12
                hours <= 12 -> hours
                else        -> hours - 12
            }
            return if (minutes == 0) "$displayHour $amPm"
            else "$displayHour:${minutes.toString().padStart(2, '0')} $amPm"
        }

    /**
     * Returns the slot detail shown under the subject name.
     * Example: "Lab B · 90 min" or "Room 204 · 60 min"
     * Used as tv_slot_detail text in item_schedule_slot.xml.
     */
    val detailLabel: String
        get() {
            val locationPart = if (location.isNotBlank()) "$location · " else ""
            return "${locationPart}$durationMinutes min"
        }

    /**
     * Returns the day name for this slot.
     * Example: dayOfWeek=0 → "Monday"
     */
    val dayName: String
        get() = when (dayOfWeek) {
            0 -> "Monday"
            1 -> "Tuesday"
            2 -> "Wednesday"
            3 -> "Thursday"
            4 -> "Friday"
            5 -> "Saturday"
            6 -> "Sunday"
            else -> ""
        }
}

/**
 * SlotType
 * Enum that controls how each schedule slot is styled.
 *
 * CLASS      → subject color background (default)
 * SELF_STUDY → light gray background "Free period / Self study"
 * BREAK      → very light background, no subject
 * EXAM       → red-tinted background
 */
enum class SlotType {
    CLASS,
    SELF_STUDY,
    BREAK,
    EXAM
}