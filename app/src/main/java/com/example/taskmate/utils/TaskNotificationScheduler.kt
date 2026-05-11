package com.example.taskmate.utils

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.taskmate.data.model.Exam
import com.example.taskmate.data.model.ScheduleSlot
import com.example.taskmate.data.model.Task
import com.example.taskmate.workers.EndOfDayReminderWorker
import com.example.taskmate.workers.ExamReminderWorker
import com.example.taskmate.workers.ScheduleReminderWorker
import com.example.taskmate.workers.TaskReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object TaskNotificationScheduler {

    // ══════════════════════════════════════════════════════════════
    //  TASK REMINDERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Schedules a notification 1 hour before task due time.
     * Only schedules if the reminder time is in the future.
     */
    fun scheduleTaskReminder(context: Context, task: Task) {
        if (task.dueDate == 0L || task.id.isBlank()) return

        val now           = System.currentTimeMillis()
        val oneHourBefore = task.dueDate - (60 * 60 * 1000L)

        if (oneHourBefore <= now) {
            android.util.Log.d("Scheduler",
                "Task reminder skipped — time already passed")
            return
        }

        val delay = oneHourBefore - now

        val data = Data.Builder()
            .putString(TaskReminderWorker.KEY_TASK_TITLE, task.title)
            .putString(TaskReminderWorker.KEY_TASK_ID, task.id)
            .build()

        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("task_${task.id}")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "task_${task.id}",
                ExistingWorkPolicy.REPLACE,
                request
            )

        android.util.Log.d("Scheduler",
            "Task reminder scheduled: ${task.title} " +
                    "in ${delay / 60000} minutes")
    }

    fun cancelTaskReminder(context: Context, taskId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("task_$taskId")
    }

    // ══════════════════════════════════════════════════════════════
    //  EXAM REMINDERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Schedules exam reminders at:
     *  - 7 days before
     *  - 3 days before
     *  - 1 day before
     *  - Day of (8 AM)
     *
     * IMPORTANT: Call this AFTER the exam is saved to Firestore
     * so exam.id is not empty.
     */
    fun scheduleExamReminders(
        context: Context,
        exam: Exam
    ) {
        if (exam.id.isBlank()) {
            android.util.Log.e("Scheduler",
                "Cannot schedule exam reminder — exam ID is empty")
            return
        }
        if (exam.examDate == 0L) return

        val now = System.currentTimeMillis()

        // Define reminder times
        val reminders = listOf(
            Triple("7days", exam.examDate - (7 * 24 * 60 * 60 * 1000L), 7),
            Triple("3days", exam.examDate - (3 * 24 * 60 * 60 * 1000L), 3),
            Triple("1day",  exam.examDate - (1 * 24 * 60 * 60 * 1000L), 1),
            Triple("today", getEightAmOnDay(exam.examDate), 0)
        )

        reminders.forEach { (suffix, triggerTime, daysAway) ->
            if (triggerTime > now) {
                val delay = triggerTime - now

                val data = Data.Builder()
                    .putString(ExamReminderWorker.KEY_EXAM_TITLE, exam.title)
                    .putString(ExamReminderWorker.KEY_EXAM_ID, exam.id)
                    .putInt(ExamReminderWorker.KEY_DAYS_AWAY, daysAway)
                    .build()

                val request = OneTimeWorkRequestBuilder<ExamReminderWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .addTag("exam_${exam.id}_$suffix")
                    .build()

                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        "exam_${exam.id}_$suffix",
                        ExistingWorkPolicy.REPLACE,
                        request
                    )

                android.util.Log.d("Scheduler",
                    "Exam reminder scheduled: ${exam.title} " +
                            "($daysAway days away) in ${delay / 3600000}h")
            }
        }
    }

    fun cancelExamReminders(context: Context, examId: String) {
        listOf("7days", "3days", "1day", "today").forEach { suffix ->
            WorkManager.getInstance(context)
                .cancelUniqueWork("exam_${examId}_$suffix")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SCHEDULE SLOT REMINDERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Schedules a notification 10 minutes before each class slot.
     * Since slots repeat weekly, this schedules the NEXT occurrence
     * of the slot and re-schedules itself after firing.
     */
    fun scheduleSlotReminder(context: Context, slot: ScheduleSlot) {
        if (slot.id.isBlank()) return

        val triggerTime = getNextOccurrence(slot.dayOfWeek, slot.startTimeMinutes)
        val now         = System.currentTimeMillis()
        val tenMinsBefore = triggerTime - (10 * 60 * 1000L)

        if (tenMinsBefore <= now) {
            android.util.Log.d("Scheduler",
                "Slot reminder: next week for ${slot.subjectName}")
            // Already passed this week — schedule for next week
            val nextWeek = triggerTime + (7 * 24 * 60 * 60 * 1000L)
            scheduleSlotAt(context, slot, nextWeek - (10 * 60 * 1000L))
            return
        }

        scheduleSlotAt(context, slot, tenMinsBefore)
    }

    private fun scheduleSlotAt(
        context: Context,
        slot: ScheduleSlot,
        triggerTimeMs: Long
    ) {
        val delay = triggerTimeMs - System.currentTimeMillis()
        if (delay <= 0) return

        val data = Data.Builder()
            .putString(ScheduleReminderWorker.KEY_SUBJECT_NAME, slot.subjectName)
            .putString(ScheduleReminderWorker.KEY_START_TIME, slot.startTimeLabel)
            .putString(ScheduleReminderWorker.KEY_LOCATION, slot.location)
            .putString(ScheduleReminderWorker.KEY_SLOT_ID, slot.id)
            .putInt(ScheduleReminderWorker.KEY_DAY_OF_WEEK, slot.dayOfWeek)
            .putInt(ScheduleReminderWorker.KEY_START_MINUTES, slot.startTimeMinutes)
            .build()

        val request = OneTimeWorkRequestBuilder<ScheduleReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("slot_${slot.id}")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "slot_${slot.id}",
                ExistingWorkPolicy.REPLACE,
                request
            )

        android.util.Log.d("Scheduler",
            "Slot reminder scheduled: ${slot.subjectName} " +
                    "in ${delay / 60000} minutes")
    }

    fun cancelSlotReminder(context: Context, slotId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("slot_$slotId")
    }

    // ══════════════════════════════════════════════════════════════
    //  END OF DAY REMINDER
    // ══════════════════════════════════════════════════════════════

    /**
     * Schedules a daily 9 PM reminder for incomplete tasks.
     * Uses PeriodicWorkRequest so it repeats every 24 hours.
     */
    fun scheduleEndOfDayReminder(context: Context) {
        val now  = Calendar.getInstance()
        val nine = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If 9 PM already passed today — schedule for tomorrow
        if (nine.timeInMillis <= now.timeInMillis) {
            nine.add(Calendar.DAY_OF_MONTH, 1)
        }

        val initialDelay = nine.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<EndOfDayReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag("end_of_day")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "end_of_day_reminder",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )

        android.util.Log.d("Scheduler",
            "End-of-day reminder scheduled in ${initialDelay / 3600000}h")
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Returns the timestamp of the next occurrence of
     * the given day + time combination.
     * dayOfWeek: 0=Monday … 6=Sunday
     * startMinutes: minutes from midnight
     */
    private fun getNextOccurrence(dayOfWeek: Int, startMinutes: Int): Long {
        val cal = Calendar.getInstance()

        // Convert our 0=Mon index to Calendar constant
        // Calendar: Sunday=1, Monday=2 … Saturday=7
        val calDay = when (dayOfWeek) {
            0 -> Calendar.MONDAY
            1 -> Calendar.TUESDAY
            2 -> Calendar.WEDNESDAY
            3 -> Calendar.THURSDAY
            4 -> Calendar.FRIDAY
            5 -> Calendar.SATURDAY
            6 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }

        // Set to next occurrence of that day
        val today = cal.get(Calendar.DAY_OF_WEEK)

        var daysUntil = calDay - today
        if (daysUntil < 0) daysUntil += 7
        if (daysUntil == 0) {
            // Same day — check if time already passed
            val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 +
                    cal.get(Calendar.MINUTE)
            if (currentMinutes >= startMinutes) {
                daysUntil = 7 // Already passed today — schedule next week
            }
        }

        cal.add(Calendar.DAY_OF_MONTH, daysUntil)
        cal.set(Calendar.HOUR_OF_DAY, startMinutes / 60)
        cal.set(Calendar.MINUTE, startMinutes % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return cal.timeInMillis
    }

    /**
     * Returns 8 AM timestamp on the day of the exam.
     */
    private fun getEightAmOnDay(examDateMs: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = examDateMs
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}