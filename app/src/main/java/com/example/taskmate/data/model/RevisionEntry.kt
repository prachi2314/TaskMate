package com.example.taskmate.data.model

/**
 * RevisionEntry.kt
 * Represents one revision session for a subject.
 * Stored in Firestore under:
 *   users/{userId}/subjects/{subjectId}/revisions/{revisionId}
 */
data class RevisionEntry(
    val id: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val chapterNumber: Int = 0,
    val chapterName: String = "",
    val notes: String = "",
    val revisedAt: Long = System.currentTimeMillis(),
    val dateString: String = ""
)