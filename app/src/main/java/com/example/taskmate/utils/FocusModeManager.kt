package com.example.taskmate.utils

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * FocusModeManager.kt
 * Location: utils/FocusModeManager.kt
 *
 * Manages Do Not Disturb mode during focus sessions.
 * When focus starts — enables DND (all notifications suppressed).
 * When focus pauses/ends — restores normal notification mode.
 */
object FocusModeManager {

    /**
     * Returns true if the app has DND permission.
     */
    fun hasDndPermission(context: Context): Boolean {
        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    /**
     * Opens the system settings screen where user can grant DND permission.
     */
    fun requestDndPermission(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Enables Do Not Disturb — suppresses all notifications from other apps.
     * Only affects OTHER apps — TaskMate focus completion notifications still work.
     */
    fun enableDnd(context: Context) {
        if (!hasDndPermission(context)) return

        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        notificationManager.setInterruptionFilter(
            // PRIORITY_ONLY — only priority notifications get through
            // Change to INTERRUPTION_FILTER_NONE to block everything
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        )
    }

    /**
     * Disables Do Not Disturb — restores all notifications.
     * Called when focus session is paused, reset, or completed.
     */
    fun disableDnd(context: Context) {
        if (!hasDndPermission(context)) return

        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        notificationManager.setInterruptionFilter(
            NotificationManager.INTERRUPTION_FILTER_ALL
        )
    }

    /**
     * Returns true if DND is currently active.
     */
    fun isDndActive(context: Context): Boolean {
        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        return notificationManager.currentInterruptionFilter !=
                NotificationManager.INTERRUPTION_FILTER_ALL
    }
}