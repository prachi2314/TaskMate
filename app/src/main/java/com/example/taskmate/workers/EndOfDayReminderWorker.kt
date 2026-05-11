package com.example.taskmate.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.taskmate.utils.Constants
import com.example.taskmate.utils.DateUtils
import com.example.taskmate.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * EndOfDayReminderWorker.kt
 * Fires at 9 PM daily.
 * Counts incomplete tasks for today and shows reminder.
 * Does NOT need Hilt because it queries Firestore directly.
 */
class EndOfDayReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
                ?: return Result.success() // Not logged in — skip

            val firestore   = FirebaseFirestore.getInstance()
            val todayStart  = DateUtils.startOfToday()
            val todayEnd    = DateUtils.endOfToday()

            // Get today's tasks
            val snapshot = firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_TASKS)
                .whereGreaterThanOrEqualTo("dueDate", todayStart)
                .whereLessThanOrEqualTo("dueDate", todayEnd)
                .get()
                .await()

            // Count incomplete tasks
            val incompleteTasks = snapshot.documents.count { doc ->
                doc.getBoolean("completed") == false
            }

            android.util.Log.d("EndOfDayWorker",
                "Incomplete tasks today: $incompleteTasks")

            if (incompleteTasks > 0) {
                NotificationHelper.showEndOfDayReminder(
                    applicationContext,
                    incompleteTasks
                )
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("EndOfDayWorker", "Error: ${e.message}")
            Result.retry()
        }
    }
}