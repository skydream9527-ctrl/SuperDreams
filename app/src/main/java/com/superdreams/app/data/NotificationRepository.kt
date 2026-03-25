package com.superdreams.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Stores captured push notifications as FeedItems.
 * Retains up to MAX_NOTIFICATIONS most-recent items (oldest dropped automatically).
 */
class NotificationRepository(context: Context) {

    private val prefs = context.getSharedPreferences("notification_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val MAX_NOTIFICATIONS = 50

        @Volatile
        private var instance: NotificationRepository? = null

        fun getInstance(context: Context): NotificationRepository {
            return instance ?: synchronized(this) {
                instance ?: NotificationRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getNotifications(): List<FeedItem> {
        val json = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<FeedItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addNotification(item: FeedItem) {
        val list = getNotifications().toMutableList()
        // Avoid exact-duplicate (same title + source within 1 second)
        val alreadyExists = list.any {
            it.title == item.title && it.source == item.source &&
                    Math.abs(it.timestamp - item.timestamp) < 1000L
        }
        if (alreadyExists) return

        list.add(0, item)
        // Keep at most MAX_NOTIFICATIONS
        val trimmed = if (list.size > MAX_NOTIFICATIONS) list.take(MAX_NOTIFICATIONS) else list
        saveNotifications(trimmed)
    }

    fun removeNotification(id: String) {
        val list = getNotifications().toMutableList()
        list.removeAll { it.id == id }
        saveNotifications(list)
    }

    fun clearAll() {
        prefs.edit().remove(KEY_NOTIFICATIONS).apply()
    }

    private fun saveNotifications(list: List<FeedItem>) {
        prefs.edit().putString(KEY_NOTIFICATIONS, gson.toJson(list)).apply()
    }
}
