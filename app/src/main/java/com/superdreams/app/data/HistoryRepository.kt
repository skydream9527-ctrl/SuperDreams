package com.superdreams.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Persists all feed items (news, notifications, todos) as historical records.
 * Items are archived when they are first created or when removed from their active repositories.
 * Stores up to MAX_HISTORY items, oldest dropped automatically.
 */
class HistoryRepository(context: Context) {

    private val prefs = context.getSharedPreferences("history_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_HISTORY = "history_items"
        private const val MAX_HISTORY = 500

        @Volatile
        private var instance: HistoryRepository? = null

        fun getInstance(context: Context): HistoryRepository {
            return instance ?: synchronized(this) {
                instance ?: HistoryRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getHistory(): List<FeedItem> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<FeedItem>>() {}.type
        return gson.fromJson(json, type)
    }

    /**
     * Get history items filtered by type. Pass null for all items.
     */
    fun getHistoryByType(feedType: FeedType?): List<FeedItem> {
        val all = getHistory()
        return if (feedType == null) all else all.filter { it.type == feedType }
    }

    /**
     * Archive an item. Skips exact duplicates (same ID).
     */
    fun archiveItem(item: FeedItem) {
        val list = getHistory().toMutableList()
        // Skip if already archived
        if (list.any { it.id == item.id }) return
        list.add(0, item)
        // Trim to max
        val trimmed = if (list.size > MAX_HISTORY) list.take(MAX_HISTORY) else list
        saveHistory(trimmed)
    }

    fun removeItem(id: String) {
        val list = getHistory().toMutableList()
        list.removeAll { it.id == id }
        saveHistory(list)
    }

    fun clearAll() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveHistory(items: List<FeedItem>) {
        prefs.edit().putString(KEY_HISTORY, gson.toJson(items)).apply()
    }
}
