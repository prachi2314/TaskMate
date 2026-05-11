package com.example.taskmate.data.model

/**
 * Exam.kt
 * Location: data/model/Exam.kt
 *
 * Represents an upcoming exam or test.
 *
 * Firestore path:
 *   users/{userId}/exams/{examId}
 *
 * Exams appear on:
 *   - Dashboard screen → "Upcoming exams" card
 *   - Progress screen → subtitle "Exam in X days"
 *   - Timetable screen → highlighted exam day
 *
 * Urgency levels (computed from daysUntilExam):
 *   URGENT  = 0–3 days  → red badge
 *   SOON    = 4–7 days  → amber badge
 *   OK      = 8+ days   → teal badge
 *   PAST    = already happened
 */
data class Exam(

    // Firestore document ID
    val id: String = "",

    // Owner's Firebase Auth UID
    val userId: String = "",

    // Exam name entered by the user
    // Example: "Physics — Unit Test", "Mathematics Mid-term"
    val title: String = "",

    // Subject this exam belongs to (links to Subject document)
    val subjectId: String = "",

    // Subject name stored for display without extra lookup
    val subjectName: String = "",

    // Subject color hex — used for the date badge background
    val subjectColorHex: String = "#7059D0",

    // Exam date as Unix timestamp (start of day, midnight)
    val examDate: Long = 0L,

    // Exam location — room number or online
    // Example: "Room 204", "Online — Google Meet"
    val location: String = "",

    // Duration in minutes
    // Example: 90 for a 90-minute exam
    val durationMinutes: Int = 0,

    // Optional preparation notes
    // Example: "Focus on chapters 5-8, practice past papers"
    val notes: String = "",

    // Whether the user has marked this exam as done
    val isDone: Boolean = false,

    // Unix timestamp when this exam was added
    val createdAt: Long = System.currentTimeMillis()

) {
    // ── Computed properties (NOT stored in Firestore) ──────────────────

    /**
     * Returns the number of days until the exam from today.
     *
     * Positive → exam is in the future
     * Zero     → exam is today
     * Negative → exam has already passed
     */
    val daysUntilExam: Int
        get() {
            val now = System.currentTimeMillis()
            val diff = examDate - now
            return (diff / (1000 * 60 * 60 * 24)).toInt()
        }

    /**
     * Returns the urgency level of this exam.
     * Used by ExamAdapter to set badge color and text.
     */
    val urgency: UrgencyLevel
        get() = when {
            isDone             -> UrgencyLevel.DONE
            daysUntilExam < 0  -> UrgencyLevel.PAST
            daysUntilExam <= 3 -> UrgencyLevel.URGENT
            daysUntilExam <= 7 -> UrgencyLevel.SOON
            else               -> UrgencyLevel.OK
        }

    /**
     * Returns the badge label shown on item_exam.xml.
     * Examples: "Today", "Tomorrow", "3 days", "Soon", "OK"
     */
    val urgencyLabel: String
        get() = when {
            isDone             -> "Done"
            daysUntilExam < 0  -> "Past"
            daysUntilExam == 0 -> "Today"
            daysUntilExam == 1 -> "Tomorrow"
            daysUntilExam <= 3 -> "Urgent"
            daysUntilExam <= 7 -> "Soon"
            else               -> "OK"
        }

    /**
     * Returns the "days away" label shown under the exam title.
     * Examples:
     *   "Today"
     *   "Tomorrow"
     *   "3 days away"
     *   "11 days away"
     */
    val daysAwayLabel: String
        get() = when {
            isDone             -> "Completed"
            daysUntilExam < 0  -> "${-daysUntilExam} days ago"
            daysUntilExam == 0 -> "Today"
            daysUntilExam == 1 -> "Tomorrow"
            else               -> "$daysUntilExam days away"
        }

    /**
     * Returns the day number for the date badge.
     * Example: examDate for April 14 → "14"
     */
    val dayNumber: String
        get() {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = examDate
            return cal.get(java.util.Calendar.DAY_OF_MONTH).toString()
        }

    /**
     * Returns the 3-letter month abbreviation for the date badge.
     * Example: examDate for April 14 → "APR"
     */
    val monthAbbreviation: String
        get() {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = examDate
            return java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
                .format(cal.time)
                .uppercase()
        }

    /**
     * Returns true if this exam is today.
     */
    val isToday: Boolean
        get() = daysUntilExam == 0

    /**
     * Returns true if the exam date has not passed and not done.
     */
    val isUpcoming: Boolean
        get() = !isDone && daysUntilExam >= 0

    /**
     * Returns the duration as a readable label.
     * Example: 90 → "90 min", 120 → "2 hrs"
     */
    val durationLabel: String
        get() = when {
            durationMinutes == 0   -> ""
            durationMinutes < 60   -> "$durationMinutes min"
            durationMinutes == 60  -> "1 hr"
            durationMinutes % 60 == 0 -> "${durationMinutes / 60} hrs"
            else -> "${durationMinutes / 60} hr ${durationMinutes % 60} min"
        }
}

/**
 * UrgencyLevel
 * Enum used by ExamAdapter to set the correct badge
 * color and background drawable on each exam row.
 */
enum class UrgencyLevel {
    URGENT,   // 0–3 days → red badge  → bg_badge_urgent
    SOON,     // 4–7 days → amber badge → bg_badge_soon
    OK,       // 8+ days  → teal badge  → bg_badge_ok
    PAST,     // already happened → gray badge
    DONE      // user marked complete → green badge
}