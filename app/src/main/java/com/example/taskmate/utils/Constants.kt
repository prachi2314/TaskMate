package com.example.taskmate.utils

/**
 * Constants.kt
 * Location: utils/Constants.kt
 *
 * All constant values used across the app in one place.
 * If a collection name or field name changes in Firestore,
 * update it here and it updates everywhere automatically.
 */
object Constants {

    // ── Firestore Collection Names ─────────────────────────────────
    const val COLLECTION_USERS    = "users"
    const val COLLECTION_TASKS    = "tasks"
    const val COLLECTION_SUBJECTS = "subjects"
    const val COLLECTION_EXAMS    = "exams"
    const val COLLECTION_SCHEDULE = "schedule"
    const val COLLECTION_SESSIONS = "sessions"

    // ── Firestore Field Names ──────────────────────────────────────
    // User fields
    const val FIELD_USER_ID              = "userId"
    const val FIELD_NAME                 = "name"
    const val FIELD_EMAIL                = "email"
    const val FIELD_PHOTO_URL            = "photoUrl"
    const val FIELD_CREATED_AT           = "createdAt"
    const val FIELD_TOTAL_STUDY_MINUTES  = "totalStudyMinutes"
    const val FIELD_CURRENT_STREAK       = "currentStreak"
    const val FIELD_LONGEST_STREAK       = "longestStreak"
    const val FIELD_LAST_STUDY_DATE      = "lastStudyDate"
    const val FIELD_TOTAL_POMODOROS      = "totalPomodoros"

    // Task fields
    const val FIELD_TITLE       = "title"
    const val FIELD_COMPLETED   = "completed"
    const val FIELD_DUE_DATE    = "dueDate"
    const val FIELD_PRIORITY    = "priority"
    const val FIELD_SUBJECT_ID  = "subjectId"
    const val FIELD_COMPLETED_AT = "completedAt"

    // Subject fields
    const val FIELD_TOTAL_CHAPTERS     = "totalChapters"
    const val FIELD_COMPLETED_CHAPTERS = "completedChapters"
    const val FIELD_COLOR_HEX          = "colorHex"
    const val FIELD_EXAM_DATE          = "examDate"
    const val FIELD_DISPLAY_ORDER      = "displayOrder"

    // Schedule fields
    const val FIELD_DAY_OF_WEEK        = "dayOfWeek"
    const val FIELD_START_TIME_MINUTES = "startTimeMinutes"

    // Session fields
    const val FIELD_DATE_STRING    = "dateString"
    const val FIELD_DURATION_MIN   = "durationMinutes"
    const val FIELD_STARTED_AT     = "startedAt"
    const val FIELD_WAS_COMPLETED  = "wasCompleted"

    // ── Google Sign-In ─────────────────────────────────────────────
    // Copy this from Firebase Console →
    // Authentication → Sign-in method → Google → Web client ID
    const val GOOGLE_WEB_CLIENT_ID = "889765211563-hbkecj0n4dlp67bc4s86o0j5dcgv0630.apps.googleusercontent.com"

    // ── Pomodoro Timer ─────────────────────────────────────────────
    const val POMODORO_FOCUS_MINUTES      = 25
    const val POMODORO_SHORT_BREAK_MINUTES = 5
    const val POMODORO_LONG_BREAK_MINUTES  = 15
    const val POMODORO_SESSIONS_BEFORE_LONG_BREAK = 4

    // ── Splash Screen ──────────────────────────────────────────────
    const val SPLASH_DELAY_MS = 2500L

    // ── SharedPreferences ──────────────────────────────────────────
    const val PREF_NAME             = "studyflow_prefs"
    const val PREF_ONBOARDING_DONE  = "onboarding_complete"
    const val PREF_SELECTED_SUBJECT = "selected_subject_id"

    // ── Navigation Arguments ───────────────────────────────────────
    const val ARG_SUBJECT_ID  = "subjectId"
    const val ARG_TASK_ID     = "taskId"
    const val ARG_EXAM_ID     = "examId"
    const val ARG_SLOT_ID     = "slotId"

    // ── Date Format ────────────────────────────────────────────────
    const val DATE_FORMAT_DISPLAY  = "dd MMM yyyy"
    const val DATE_FORMAT_STORAGE  = "yyyy-MM-dd"
    const val DATE_FORMAT_TIME     = "hh:mm a"
}