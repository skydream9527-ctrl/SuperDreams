package com.superdreams.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.superdreams.app.data.FeedRepository

class DismissActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "com.superdreams.app.ACTION_DISMISS_ITEM"
        const val EXTRA_ITEM_ID = "extra_item_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_DISMISS) {
            val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
            // Check notifications first; fall back to feed repository
            val notifRepo = com.superdreams.app.data.NotificationRepository.getInstance(context)
            val isNotification = notifRepo.getNotifications().any { it.id == itemId }
            if (isNotification) {
                notifRepo.removeNotification(itemId)
            } else {
                FeedRepository.getInstance(context).removeItem(itemId)
            }
            SuperDreamsWidget.refreshWidget(context)
        }
    }
}
