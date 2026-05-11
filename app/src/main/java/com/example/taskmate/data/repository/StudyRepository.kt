package com.example.taskmate.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.taskmate.data.model.Exam
import com.example.taskmate.data.model.ScheduleSlot
import com.example.taskmate.data.model.StudySession
import com.example.taskmate.data.model.Subject
import com.example.taskmate.data.model.Task
import com.example.taskmate.utils.Constants
import com.example.taskmate.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.java

/**
 * StudyRepository.kt
 * Location: data/repository/StudyRepository.kt
 *
 * All Firestore read/write operations for:
 *   Tasks, Subjects, Exams, Schedule, Sessions
 *
 * KEY PATTERN — Real-time listeners use Kotlin Flow:
 *
 * callbackFlow { } wraps Firestore's addSnapshotListener inside
 * a coroutine Flow. Every time Firestore data changes, the new
 * data automatically flows to the ViewModel, which pushes it to
 * LiveData, which the Fragment observes.
 *
 * The chain is:
 * Firestore → onSnapshot → callbackFlow → ViewModel → LiveData → Fragment
 *
 * awaitClose { listener.remove() } removes the Firestore listener
 * when the coroutine is cancelled (Fragment destroyed) to prevent
 * memory leaks.
 */
@Singleton
class StudyRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // ══════════════════════════════════════════════════════════════
    //  TASKS
    // ══════════════════════════════════════════════════════════════

    /**
     * Returns a real-time Flow of all tasks for a user.
     * Ordered by due date ascending (earliest first).
     *
     * The Flow emits a new value every time any task is added,
     * updated, or deleted in Firestore — no manual refresh needed.
     */

    
    fun getTasksForUser(userId: String): Flow<Resource<List<Task>>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = firestore
            .collection(Constants.COLLECTION_USERS)
            .document(userId)
            .collection(Constants.COLLECTION_TASKS)
            .orderBy(Constants.FIELD_DUE_DATE, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to load tasks"))
                    return@addSnapshotListener
                }
                val tasks = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Task::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(Resource.Success(tasks))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Returns today's tasks only.
     * Filters by dueDate between start and end of today.
     */
    fun getTodayTasksForUser(
        userId: String,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<Resource<List<Task>>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = firestore
            .collection(Constants.COLLECTION_USERS)
            .document(userId)
            .collection(Constants.COLLECTION_TASKS)
            .whereGreaterThanOrEqualTo(Constants.FIELD_DUE_DATE, startOfDay)
            .whereLessThanOrEqualTo(Constants.FIELD_DUE_DATE, endOfDay)
            .orderBy(Constants.FIELD_DUE_DATE, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to load tasks"))
                    return@addSnapshotListener
                }
                val tasks = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Task::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(Resource.Success(tasks))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Adds a new task to Firestore.
     * Firestore auto-generates the document ID.
     * The generated ID is copied back into the task before saving.
     */
    suspend fun addTask(userId: String, task: Task): Resource<Unit> {
        return try {
            val collectionRef = firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_TASKS)

            // Generate the document ID first
            val docRef = collectionRef.document()

            // Save the task with the generated ID stored inside it
            collectionRef.document(docRef.id)
                .set(task.copy(id = docRef.id, userId = userId))
                .await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add task.")
        }
    }

    /**
     * Toggles the completed field on a task.
     * Uses .update() to patch only that one field — not
     * the whole document — so other fields are not affected.
     */
    suspend fun toggleTaskComplete(
        userId: String,
        taskId: String,
        completed: Boolean
    ): Resource<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                Constants.FIELD_COMPLETED to completed
            )
            if (completed) {
                updates[Constants.FIELD_COMPLETED_AT] = System.currentTimeMillis()
            }
            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_TASKS)
                .document(taskId)
                .update(updates)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update task.")
        }
    }

    /**
     * Updates specific fields of an existing task.
     */
    suspend fun updateTask(
        userId: String,
        taskId: String,
        updates: Map<String, Any>
    ): Resource<Unit> {
        return try {
            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_TASKS)
                .document(taskId)
                .update(updates)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update task.")
        }
    }

    /**
     * Permanently deletes a task document.
     */
    suspend fun deleteTask(userId: String, taskId: String): Resource<Unit> {
        return try {
            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_TASKS)
                .document(taskId)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete task.")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SUBJECTS
    // ══════════════════════════════════════════════════════════════

    fun getSubjectsForUser(userId: String): Flow<Resource<List<Subject>>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = firestore
            .collection(Constants.COLLECTION_USERS)
            .document(userId)
            .collection(Constants.COLLECTION_SUBJECTS)
            .orderBy(Constants.FIELD_DISPLAY_ORDER, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to load subjects"))
                    return@addSnapshotListener
                }
                val subjects = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Subject::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(Resource.Success(subjects))
            }

        awaitClose { listener.remove() }
    }

    suspend fun addSubject(userId: String, subject: Subject): Resource<Unit> {
        return try {
            val collectionRef = firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_SUBJECTS)

            val docRef = collectionRef.document()
            collectionRef.document(docRef.id)
                .set(subject.copy(id = docRef.id, userId = userId))
                .await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add subject.")
        }
    }

    /**
     * Updates the completedChapters field only.
     * Called when user increments chapter progress.
     */
    suspend fun updateSubjectProgress(
        userId: String,
        subjectId: String,
        completedChapters: Int
    ): Resource<Unit> {
        return try {
            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_SUBJECTS)
                .document(subjectId)
                .update(Constants.FIELD_COMPLETED_CHAPTERS, completedChapters)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update progress.")
        }
    }

    suspend fun updateSubject(
        userId: String,
        subjectId: String,
        updates: Map<String, Any>
    ): Resource<Unit> {
        return try {
            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_SUBJECTS)
                .document(subjectId)
                .update(updates)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update subject.")
        }
    }

    suspend fun deleteSubject(userId: String, subjectId: String): Resource<Unit> {
        return try {
            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_SUBJECTS)
                .document(subjectId)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete subject.")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  EXAMS
    // ══════════════════════════════════════════════════════════════

    fun getExamsForUser(userId: String): Flow<Resource<List<Exam>>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = firestore
            .collection(Constants.COLLECTION_USERS)
            .document(userId)
            .collection(Constants.COLLECTION_EXAMS)
            .orderBy(Constants.FIELD_EXAM_DATE, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to load exams"))
                    return@addSnapshotListener
                }
                val exams = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Exam::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(Resource.Success(exams))
            }

        awaitClose { listener.remove() }
    }

    suspend fun addExam(userId: String, exam: Exam): Resource<Exam> {
        return try {
            val collectionRef = firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_EXAMS)

            val docRef = collectionRef.document()

            // Save exam with the generated ID inside it
            val examWithId = exam.copy(id = docRef.id, userId = userId)
            collectionRef.document(docRef.id)
                .set(examWithId)
                .await()

            // Return exam WITH the ID so caller can schedule notifications
            Resource.Success(examWithId)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add exam.")
        }
    }

    suspend fun deleteExam(userId: String, examId: String): Resource<Unit> {
        return try {
            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_EXAMS)
                .document(examId)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete exam.")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SCHEDULE SLOTS
    // ══════════════════════════════════════════════════════════════

    fun getSlotsForDay(
        userId: String,
        dayOfWeek: Int
    ): Flow<Resource<List<ScheduleSlot>>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = firestore
            .collection(Constants.COLLECTION_USERS)
            .document(userId)
            .collection(Constants.COLLECTION_SCHEDULE)
            .whereEqualTo(Constants.FIELD_DAY_OF_WEEK, dayOfWeek)
            // Removed orderBy — no composite index needed
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to load schedule"))
                    return@addSnapshotListener
                }
                val slots = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ScheduleSlot::class.java)?.copy(id = doc.id)
                }
                    // Sort in memory instead of Firestore
                    ?.sortedBy { it.startTimeMinutes }
                    ?: emptyList()
                trySend(Resource.Success(slots))
            }

        awaitClose { listener.remove() }
    }

    suspend fun addScheduleSlot(userId: String, slot: ScheduleSlot): Resource<ScheduleSlot> {
        return try {
            val collectionRef = firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_SCHEDULE)

            val docRef    = collectionRef.document()
            val slotWithId = slot.copy(id = docRef.id, userId = userId)
            collectionRef.document(docRef.id)
                .set(slotWithId)
                .await()

            Resource.Success(slotWithId)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add schedule slot.")
        }
    }

    suspend fun deleteScheduleSlot(userId: String, slotId: String): Resource<Unit> {
        return try {
            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_SCHEDULE)
                .document(slotId)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete slot.")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  STUDY SESSIONS
    // ══════════════════════════════════════════════════════════════

    /**
     * Records a completed Pomodoro session.
     * Called from FocusViewModel when the timer reaches zero.
     */
    suspend fun recordSession(
        userId: String,
        session: StudySession
    ): Resource<Unit> {
        return try {
            val collectionRef = firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_SESSIONS)

            val docRef = collectionRef.document()
            collectionRef.document(docRef.id)
                .set(session.copy(id = docRef.id, userId = userId))
                .await()

            // Also update the user's total study minutes and pomodoro count
            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update(
                    mapOf(
                        Constants.FIELD_TOTAL_STUDY_MINUTES to
                                com.google.firebase.firestore.FieldValue
                                    .increment(session.durationMinutes.toLong()),
                        Constants.FIELD_TOTAL_POMODOROS to
                                com.google.firebase.firestore.FieldValue.increment(1)
                    )
                )
                .await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to record session.")
        }
    }

    suspend fun updateStreak(userId: String) {
        try {
            val userDoc = firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            if (!userDoc.exists()) return

            // Read individual fields directly — avoids User import issues
            val currentStreak  = userDoc.getLong("currentStreak")?.toInt()  ?: 0
            val longestStreak  = userDoc.getLong("longestStreak")?.toInt()  ?: 0
            val lastStudyDate  = userDoc.getLong("lastStudyDate")            ?: 0L

            val todayStart = com.example.taskmate.utils.DateUtils.startOfToday()
            val yesterday  = todayStart - 86400000L // 24 hours in ms

            val newStreak = when {
                // Already studied today — no change needed
                lastStudyDate >= todayStart -> currentStreak

                // Studied yesterday — increment streak
                lastStudyDate >= yesterday  -> currentStreak + 1

                // Missed one or more days — reset to 1
                else                        -> 1
            }

            val newLongest = maxOf(newStreak, longestStreak)
            val now        = System.currentTimeMillis()

            // Only update Firestore if streak value actually changed
            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update(
                    mapOf(
                        "currentStreak" to newStreak,
                        "longestStreak" to newLongest,
                        "lastStudyDate" to now
                    )
                )
                .await()

            android.util.Log.d(
                "StudyRepository",
                "Streak updated: $currentStreak → $newStreak (longest: $newLongest)"
            )

        } catch (e: Exception) {
            android.util.Log.e("StudyRepository", "Streak update failed: ${e.message}")
        }
    }

    /**
     * Returns all sessions for the current week.
     * Used to calculate weekly goal progress.
     */
    suspend fun getSessionsThisWeek(
        userId: String,
        weekStartMs: Long
    ): Resource<List<StudySession>> {
        return try {
            val snapshot = firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_SESSIONS)
                .whereGreaterThanOrEqualTo(Constants.FIELD_COMPLETED_AT, weekStartMs)
                .get()
                .await()

            val sessions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(StudySession::class.java)?.copy(id = doc.id)
            }
            Resource.Success(sessions)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load sessions.")
        }
    }

    /**
     * Returns sessions grouped by dateString for the calendar heatmap.
     * Returns a Map<String, Int> where:
     *   key   = "YYYY-MM-DD"
     *   value = total study minutes that day
     */
    suspend fun getSessionsByDate(
        userId: String,
        month: String // format: "2026-04"
    ): Resource<Map<String, Int>> {
        return try {
            val snapshot = firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_SESSIONS)
                .whereGreaterThanOrEqualTo(Constants.FIELD_DATE_STRING, "${month}-01")
                .whereLessThanOrEqualTo(Constants.FIELD_DATE_STRING, "${month}-31")
                .get()
                .await()

            val sessions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(StudySession::class.java)
            }

            // Group by date and sum minutes
            val grouped = sessions
                .groupBy { it.dateString }
                .mapValues { entry ->
                    entry.value.sumOf { it.durationMinutes }
                }

            Resource.Success(grouped)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load session data.")
        }
    }

    suspend fun logRevision(
        userId: String,
        subjectId: String,
        entry: com.example.taskmate.data.model.RevisionEntry
    ): Resource<Unit> {
        return try {
            val collRef = firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_SUBJECTS)
                .document(subjectId)
                .collection("revisions")

            val docRef = collRef.document()
            collRef.document(docRef.id)
                .set(entry.copy(id = docRef.id))
                .await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to log revision")
        }
    }

    suspend fun markChapterRevised(
        userId: String,
        subjectId: String,
        chapterIndex: Int
    ): Resource<Unit> {
        return try {
            val docRef = firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_SUBJECTS)
                .document(subjectId)

            val doc = docRef.get().await()
            val currentRevised = doc.get("revisedChapters") as? List<Long>
                ?: emptyList()

            // Add index if not already present
            if (!currentRevised.contains(chapterIndex.toLong())) {
                val updated = currentRevised + chapterIndex.toLong()
                docRef.update("revisedChapters", updated).await()
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark chapter revised")
        }
    }
}