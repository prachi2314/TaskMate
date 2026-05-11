package com.example.taskmate.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.taskmate.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ExamReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_EXAM_TITLE = "exam_title"
        const val KEY_EXAM_ID    = "exam_id"
        const val KEY_DAYS_AWAY  = "days_away"
    }

    override suspend fun doWork(): Result {
        val examTitle = inputData.getString(KEY_EXAM_TITLE)
            ?: return Result.failure()
        val examId   = inputData.getString(KEY_EXAM_ID)
            ?: return Result.failure()
        val daysAway = inputData.getInt(KEY_DAYS_AWAY, 1)

        NotificationHelper.showExamNotification(
            applicationContext,
            examTitle,
            daysAway,
            examId.hashCode()
        )

        android.util.Log.d("ExamReminderWorker",
            "Exam notification fired: $examTitle ($daysAway days away)")

        return Result.success()
    }
}