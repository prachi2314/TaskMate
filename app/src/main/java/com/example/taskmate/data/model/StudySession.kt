package com.example.taskmate.data.model

/**
 * StudySession.kt
 * Location: data/model/StudySession.kt
 *
 * Represents one completed Pomodoro focus session.
 *
 * Firestore path:
 *   users/{userId}/sessions/{sessionId}
 *
 * Sessions are recorded every time the Pomodoro timer
 * completes in FocusFragment. They are used to:
 *   - Calculate today's study hours on the Dashboard
 *   - Build the study calendar heatmap on the Progress screen
 *   - Update the user's streak and totalStudyMinutes
 *   - Track weekly goal progress (study hours ring)
 */
data class StudySession(

    // Firestore document ID
    val id: String = "",

    // Owner's Firebase Auth UID
    val userId: String = "",

    // Subject studied during this session
    val subjectId: String = "",

    // Subject name — stored for display and filtering
    val subjectName: String = "",

    // Duration of this session in minutes
    // Usually 25 for a standard Pomodoro
    // Could be 5 or 15 for break sessions (these are NOT recorded)
    val durationMinutes: Int = 25,

    // Unix timestamp when the session STARTED
    val startedAt: Long = 0L,

    // Unix timestamp when the session ENDED (completed)
    val completedAt: Long = System.currentTimeMillis(),

    // Whether the session was completed fully or interrupted
    val wasCompleted: Boolean = true,

    // Date string in "YYYY-MM-DD" format for easy calendar grouping
    // Example: "2026-04-11"
    // Stored as string so Firestore can query by date without complex math
    val dateString: String = ""

) {
    // ── Computed properties ────────────────────────────────────────────

    /**
     * Returns the study date as a Calendar object.
     * Used to check if this session was today, this week, etc.
     */
    val sessionCalendar: java.util.Calendar
        get() = java.util.Calendar.getInstance().apply {
            timeInMillis = completedAt
        }

    /**
     * Returns true if this session was completed today.
     * Used to calculate today's study hours on the Dashboard.
     */
    val isToday: Boolean
        get() {
            val sessionCal = sessionCalendar
            val todayCal   = java.util.Calendar.getInstance()
            return sessionCal.get(java.util.Calendar.YEAR) ==
                    todayCal.get(java.util.Calendar.YEAR) &&
                    sessionCal.get(java.util.Calendar.DAY_OF_YEAR) ==
                    todayCal.get(java.util.Calendar.DAY_OF_YEAR)
        }

    /**
     * Returns true if this session was completed this week
     * (Monday to Sunday of the current week).
     * Used for the weekly goals progress rings.
     */
    val isThisWeek: Boolean
        get() {
            val sessionCal = sessionCalendar
            val todayCal   = java.util.Calendar.getInstance()
            return sessionCal.get(java.util.Calendar.YEAR) ==
                    todayCal.get(java.util.Calendar.YEAR) &&
                    sessionCal.get(java.util.Calendar.WEEK_OF_YEAR) ==
                    todayCal.get(java.util.Calendar.WEEK_OF_YEAR)
        }

    companion object {
        /**
         * Creates a dateString in "YYYY-MM-DD" format from a timestamp.
         * Call this when creating a new StudySession to set dateString.
         *
         * Example:
         *   val session = StudySession(
         *       dateString = StudySession.formatDate(System.currentTimeMillis()),
         *       ...
         *   )
         */
        fun formatDate(timestamp: Long): String {
            return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date(timestamp))
        }
    }
}