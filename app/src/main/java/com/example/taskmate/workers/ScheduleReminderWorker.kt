package com.example.taskmate.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.taskmate.data.model.ScheduleSlot
import com.example.taskmate.utils.NotificationHelper
import com.example.taskmate.utils.TaskNotificationScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * ScheduleReminderWorker.kt
 * Fires 10 minutes before a scheduled class starts.
 * After firing, re-schedules itself for next week.
 */
@HiltWorker
class ScheduleReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SUBJECT_NAME  = "subject_name"
        const val KEY_START_TIME    = "start_time"
        const val KEY_LOCATION      = "location"
        const val KEY_SLOT_ID       = "slot_id"
        const val KEY_DAY_OF_WEEK   = "day_of_week"
        const val KEY_START_MINUTES = "start_minutes"
    }

    override suspend fun doWork(): Result {
        val subjectName = inputData.getString(KEY_SUBJECT_NAME)
            ?: return Result.failure()
        val startTime   = inputData.getString(KEY_START_TIME) ?: ""
        val location    = inputData.getString(KEY_LOCATION)   ?: ""
        val slotId      = inputData.getString(KEY_SLOT_ID)    ?: return Result.failure()
        val dayOfWeek   = inputData.getInt(KEY_DAY_OF_WEEK, 0)
        val startMins   = inputData.getInt(KEY_START_MINUTES, 0)

        // Show the notification
        NotificationHelper.showScheduleReminder(
            applicationContext,
            subjectName,
            startTime,
            location,
            slotId.hashCode()
        )

        android.util.Log.d("ScheduleReminderWorker",
            "Schedule notification fired: $subjectName at $startTime")

        // Re-schedule for next week automatically
        val nextWeekSlot = ScheduleSlot(
            id                = slotId,
            subjectName       = subjectName,
            location          = location,
            dayOfWeek         = dayOfWeek,
            startTimeMinutes  = startMins
        )

        TaskNotificationScheduler.scheduleSlotReminder(
            applicationContext,
            nextWeekSlot
        )

        return Result.success()
    }
}