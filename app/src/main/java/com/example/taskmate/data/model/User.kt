package com.example.taskmate.data.model

/**
 * User.kt
 * Location: data/model/User.kt
 *
 * Represents a user document stored in Firestore.
 *
 * Firestore path:
 *   users/{userId}
 *
 * IMPORTANT RULES for Firestore data classes:
 *
 * Rule 1 — Every property MUST have a default value.
 *   Firestore uses reflection to deserialize documents.
 *   If a field is missing in the document, it falls back
 *   to the default value instead of crashing.
 *
 * Rule 2 — Use only types Firestore can serialize:
 *   String, Long, Int, Double, Boolean, List, Map
 *   Do NOT use: Date (use Long timestamp instead)
 *
 * Rule 3 — The class must have a no-argument constructor.
 *   Default values on every property automatically
 *   creates a no-arg constructor in Kotlin.
 *
 * Rule 4 — Field names must match Firestore document keys exactly.
 *   userId in Kotlin = "userId" field in Firestore document.
 */
data class User(

    // Firebase Auth UID — used as Firestore document ID
    // Example: "abc123xyz"
    val userId: String = "",

    // Display name entered during registration or from Google profile
    val name: String = "",

    // Email address from Firebase Auth
    val email: String = "",

    // Profile photo URL — populated from Google Sign-In
    // Empty string if user registered with email/password
    val photoUrl: String = "",

    // Unix timestamp (milliseconds) when the account was created
    // System.currentTimeMillis() returns this format
    val createdAt: Long = 0L,

    // Total study minutes accumulated across all sessions
    // Updated every time a Pomodoro session completes
    val totalStudyMinutes: Int = 0,

    // Number of consecutive days the user has studied
    // Reset to 0 if user misses a day
    val currentStreak: Int = 0,

    // Longest streak ever achieved — never resets
    val longestStreak: Int = 0,

    // Unix timestamp of the last day the user studied
    // Used to calculate whether streak should increase or reset
    val lastStudyDate: Long = 0L,

    // Total number of completed Pomodoro sessions
    val totalPomodoros: Int = 0,

    // User's semester or grade — displayed as subtitle on subject screen
    // Example: "Semester 2", "Grade 12", "Year 3"
    val semester: String = "Semester 1",

    // Target daily study hours set by the user
    // Used to calculate daily progress percentage
    val dailyStudyGoalHours: Int = 4,

    // Whether the user has completed onboarding
    val onboardingComplete: Boolean = false

) {
    // ── Computed properties (NOT stored in Firestore) ──────────────────
    // These are calculated from stored values every time they are accessed.
    // Firestore ignores computed properties during serialization.

    /**
     * Returns the user's first name only.
     * "Arjun Mehta" → "Arjun"
     * Used for the greeting on the Dashboard header.
     */
    val firstName: String
        get() = name.split(" ").firstOrNull() ?: name

    /**
     * Returns initials from the user's name.
     * "Arjun Mehta" → "AM"
     * Used for the avatar placeholder when no photo is available.
     */
    val initials: String
        get() {
            val parts = name.trim().split(" ")
            return when {
                parts.size >= 2 -> "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}".uppercase()
                parts.size == 1 -> parts[0].take(2).uppercase()
                else            -> "??"
            }
        }

    /**
     * Returns true if the user has a profile photo URL.
     * Used to decide whether to show the photo or the initials avatar.
     */
    val hasPhoto: Boolean
        get() = photoUrl.isNotBlank()
}