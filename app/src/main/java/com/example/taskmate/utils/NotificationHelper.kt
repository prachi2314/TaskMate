package com.example.taskmate.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.taskmate.MainActivity
import com.example.taskmate.R

/**
 * NotificationHelper.kt
 * Location: utils/NotificationHelper.kt
 *
 * Handles all notification creation for TaskMate.
 * Channels: Tasks, Exams, Schedule, Focus
 */
object NotificationHelper {

    const val CHANNEL_TASKS    = "tasks_channel"
    const val CHANNEL_EXAMS    = "exams_channel"
    const val CHANNEL_SCHEDULE = "schedule_channel"
    const val CHANNEL_FOCUS    = "focus_channel"

    // ══════════════════════════════════════════════════════════════
    //  CREATE CHANNELS
    // ══════════════════════════════════════════════════════════════

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_TASKS,
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for upcoming and pending tasks"
                    enableVibration(true)
                }
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_EXAMS,
                    "Exam Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders before upcoming exams"
                    enableVibration(true)
                }
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SCHEDULE,
                    "Schedule Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders when a scheduled class is about to start"
                    enableVibration(true)
                }
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_FOCUS,
                    "Focus Session",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Pomodoro timer notifications"
                }
            )
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PERMISSION CHECK
    // ══════════════════════════════════════════════════════════════

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun safeNotify(
        context: Context,
        notificationId: Int,
        notification: android.app.Notification
    ) {
        if (!hasNotificationPermission(context)) return
        try {
            NotificationManagerCompat.from(context)
                .notify(notificationId, notification)
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "SecurityException: ${e.message}")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  TASK NOTIFICATIONS
    // ══════════════════════════════════════════════════════════════

    /**
     * Fires 1 hour before task due time.
     */
    fun showTaskReminderNotification(
        context: Context,
        taskTitle: String,
        notificationId: Int
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(R.drawable.ic_check_24)
            .setContentTitle("⏰ Task due in 1 hour!")
            .setContentText(taskTitle)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("\"$taskTitle\" is due in 1 hour. Finish it now!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(buildPendingIntent(context))
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        safeNotify(context, notificationId, notification)
    }

    /**
     * Fires at 9 PM for all incomplete tasks of the day.
     */
    fun showEndOfDayReminder(
        context: Context,
        incompleteCount: Int,
        notificationId: Int = 5001
    ) {
        if (incompleteCount <= 0) return

        val message = when (incompleteCount) {
            1    -> "You have 1 task still pending today!"
            else -> "You have $incompleteCount tasks still pending today!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(R.drawable.ic_check_24)
            .setContentTitle("📋 Don't forget your tasks!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$message Open TaskMate to complete them before midnight."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(buildPendingIntent(context))
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        safeNotify(context, notificationId, notification)
    }

    // ══════════════════════════════════════════════════════════════
    //  EXAM NOTIFICATIONS
    // ══════════════════════════════════════════════════════════════

    /**
     * Fires based on days away from exam.
     */
    fun showExamNotification(
        context: Context,
        examTitle: String,
        daysAway: Int,
        notificationId: Int
    ) {
        val title = when (daysAway) {
            0    -> "📝 Exam TODAY — $examTitle"
            1    -> "📝 Exam TOMORROW — $examTitle"
            else -> "📝 Exam in $daysAway days — $examTitle"
        }

        val message = when (daysAway) {
            0    -> "Your exam is today! Review your notes and stay calm."
            1    -> "Last day to revise! Make sure you're prepared."
            2, 3 -> "Only $daysAway days left. Focus on weak areas."
            else -> "Start preparing now to stay ahead."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_EXAMS)
            .setSmallIcon(R.drawable.ic_schedule_24)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(buildPendingIntent(context))
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        safeNotify(context, notificationId, notification)
    }

    // ══════════════════════════════════════════════════════════════
    //  SCHEDULE NOTIFICATIONS
    // ══════════════════════════════════════════════════════════════

    /**
     * Fires 10 minutes before a scheduled class starts.
     */
    fun showScheduleReminder(
        context: Context,
        subjectName: String,
        startTime: String,
        location: String,
        notificationId: Int
    ) {
        val locationText = if (location.isNotBlank()) " • $location" else ""
        val message      = "$subjectName starts at $startTime$locationText"

        val notification = NotificationCompat.Builder(context, CHANNEL_SCHEDULE)
            .setSmallIcon(R.drawable.ic_schedule_24)
            .setContentTitle("📚 Class starting in 10 minutes!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(buildPendingIntent(context))
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        safeNotify(context, notificationId, notification)
    }

    // ══════════════════════════════════════════════════════════════
    //  FOCUS NOTIFICATIONS
    // ══════════════════════════════════════════════════════════════

    fun showFocusCompleteNotification(
        context: Context,
        message: String = "Focus session complete! Take a break.",
        notificationId: Int = 3001
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_timer_24)
            .setContentTitle("TaskMate Focus")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        safeNotify(context, notificationId, notification)
    }

    fun showBreakCompleteNotification(
        context: Context,
        notificationId: Int = 3002
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_timer_24)
            .setContentTitle("Break Complete")
            .setContentText("Break is over! Time to focus again.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(buildPendingIntent(context))
            .setAutoCancel(true)
            .build()

        safeNotify(context, notificationId, notification)
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPER
    // ══════════════════════════════════════════════════════════════

    fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}