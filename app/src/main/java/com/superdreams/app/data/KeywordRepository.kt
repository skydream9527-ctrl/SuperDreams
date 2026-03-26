package com.superdreams.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class KeywordRepository(context: Context) {

    private val prefs = context.getSharedPreferences("keyword_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_KEYWORDS = "keywords"
        const val MIN_KEYWORDS = 5
        const val MAX_KEYWORDS = 60

        @Volatile
        private var instance: KeywordRepository? = null

        fun getInstance(context: Context): KeywordRepository {
            return instance ?: synchronized(this) {
                instance ?: KeywordRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getKeywords(): List<String> {
        val json = prefs.getString(KEY_KEYWORDS, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addKeyword(keyword: String): Boolean {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return false
        val keywords = getKeywords().toMutableList()
        if (keywords.size >= MAX_KEYWORDS) return false
        if (keywords.any { it.equals(trimmed, ignoreCase = true) }) return false
        keywords.add(trimmed)
        saveKeywords(keywords)
        return true
    }

    fun removeKeyword(keyword: String) {
        val keywords = getKeywords().toMutableList()
        keywords.removeAll { it.equals(keyword, ignoreCase = true) }
        saveKeywords(keywords)
    }

    fun getKeywordCount(): Int = getKeywords().size

    fun hasEnoughKeywords(): Boolean = getKeywords().size >= MIN_KEYWORDS

    private fun saveKeywords(keywords: List<String>) {
        prefs.edit().putString(KEY_KEYWORDS, gson.toJson(keywords)).apply()
    }
}
