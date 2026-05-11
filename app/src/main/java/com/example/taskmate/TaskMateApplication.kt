package com.example.taskmate

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.taskmate.utils.NotificationHelper
import com.example.taskmate.utils.TaskNotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * TaskMateApplication.kt
 * Location: java/com/example/taskmate/TaskMateApplication.kt
 *
 * @HiltAndroidApp triggers Hilt code generation.
 * Implements Configuration.Provider so WorkManager
 * uses Hilt to inject dependencies into Workers.
 */
@HiltAndroidApp
class TaskMateApplication : Application(), Configuration.Provider {

    // Hilt injects this — needed for HiltWorker to work
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Create all notification channels on first launch
        NotificationHelper.createNotificationChannels(this)

        // Schedule daily 9 PM end-of-day reminder
        TaskNotificationScheduler.scheduleEndOfDayReminder(this)
    }

    /**
     * Required for WorkManager + Hilt integration.
     * Tells WorkManager to use HiltWorkerFactory instead
     * of the default factory — this allows @Inject in Workers.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}