package com.example.taskmate.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.taskmate.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TaskReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TASK_TITLE = "task_title"
        const val KEY_TASK_ID    = "task_id"
    }

    override suspend fun doWork(): Result {
        val taskTitle = inputData.getString(KEY_TASK_TITLE)
            ?: return Result.failure()
        val taskId = inputData.getString(KEY_TASK_ID)
            ?: return Result.failure()

        NotificationHelper.showTaskReminderNotification(
            applicationContext,
            taskTitle,
            taskId.hashCode()
        )

        android.util.Log.d("TaskReminderWorker",
            "Notification fired for: $taskTitle")

        return Result.success()
    }
}