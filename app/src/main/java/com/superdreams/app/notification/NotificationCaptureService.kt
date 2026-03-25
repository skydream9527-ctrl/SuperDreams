package com.superdreams.app.notification

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.superdreams.app.data.FeedItem
import com.superdreams.app.data.FeedType
import com.superdreams.app.data.NotificationRepository
import com.superdreams.app.widget.SuperDreamsWidget
import java.util.UUID

/**
 * Listens to all status-bar notifications on the device.
 * Each non-trivial notification is stored in NotificationRepository and shown in the main feed.
 *
 * The user must grant Notification Access in system Settings for this service to activate.
 */
class NotificationCaptureService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationCapture"
        // Packages to ignore (our own app and common noisy system services)
        private val IGNORED_PACKAGES = setOf(
            "com.superdreams.app",
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.google.android.gms",
            "com.google.android.gsf"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName ?: return

            // Skip our own app and system noise
            if (IGNORED_PACKAGES.any { pkg.startsWith(it) }) return

            // Skip ongoing (persistent) notifications like music players, downloads
            if (sbn.isOngoing) return

            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
            val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim() ?: ""

            // Skip if both title and text are empty
            if (title.isEmpty() && text.isEmpty()) return

            // Resolve human-readable app name
            val appLabel = try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
                ).toString()
            } catch (e: Exception) {
                pkg
            }

            val feedTitle = title.ifEmpty { appLabel }
            val feedSubtitle = text.ifEmpty { title }

            val item = FeedItem(
                id = UUID.randomUUID().toString(),
                title = feedTitle,
                subtitle = feedSubtitle,
                type = FeedType.NOTIFICATION,
                timestamp = sbn.postTime,
                source = appLabel,
                url = "",
                keyword = pkg
            )

            val repo = NotificationRepository.getInstance(applicationContext)
            repo.addNotification(item)

            // Refresh widget to show new notification
            SuperDreamsWidget.refreshWidget(applicationContext)

            Log.d(TAG, "Captured notification from $appLabel: $feedTitle")
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // We intentionally keep captured notifications in our feed even after dismissal,
        // so the user can review them later. They can swipe to dismiss from our list.
    }
}
