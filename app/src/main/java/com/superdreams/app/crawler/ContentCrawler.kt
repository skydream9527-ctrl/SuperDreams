package com.superdreams.app.crawler

import com.superdreams.app.data.FeedItem
import com.superdreams.app.data.FeedType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Fetches news from NewsAPI.org using the /v2/everything endpoint.
 * Each keyword is queried separately, results are deduplicated and shuffled.
 */
class ContentCrawler {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val API_KEY = "a490849e920447779010b24e3da4ab60"
        private const val BASE_URL = "https://newsapi.org/v2/everything"
        private const val PAGE_SIZE = 10
    }

    /**
     * Fetch news for all keywords and return aggregated feed items.
     */
    fun crawlAll(keywords: List<String>): List<FeedItem> {
        val allItems = mutableListOf<FeedItem>()
        for (keyword in keywords) {
            try {
                allItems.addAll(fetchNews(keyword))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Deduplicate by title prefix and shuffle for variety
        return allItems
            .distinctBy { it.title.take(30) }
            .shuffled()
    }

    /**
     * Query NewsAPI /v2/everything for a single keyword.
     */
    private fun fetchNews(keyword: String): List<FeedItem> {
        val encoded = URLEncoder.encode(keyword, "UTF-8")
        val url = "$BASE_URL?q=$encoded&pageSize=$PAGE_SIZE&sortBy=publishedAt&language=zh&apiKey=$API_KEY"

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        val items = mutableListOf<FeedItem>()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string() ?: return emptyList()
            val json = JSONObject(body)

            if (json.optString("status") != "ok") return emptyList()

            val articles = json.optJSONArray("articles") ?: return emptyList()

            for (i in 0 until articles.length()) {
                val article = articles.getJSONObject(i)

                val title = article.optString("title", "").trim()
                if (title.isEmpty() || title == "[Removed]") continue

                val description = article.optString("description", "").trim()
                val content = article.optString("content", "").trim()
                // Use description first, fall back to content snippet
                val subtitle = when {
                    description.isNotEmpty() -> description
                    content.isNotEmpty() -> content.take(300)
                    else -> ""
                }

                val sourceName = article.optJSONObject("source")?.optString("name", "") ?: ""
                val articleUrl = article.optString("url", "")
                val publishedAt = article.optString("publishedAt", "")

                // Parse ISO 8601 timestamp to millis
                val timestamp = try {
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).let {
                        it.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        it.parse(publishedAt)?.time ?: System.currentTimeMillis()
                    }
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                items.add(FeedItem(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    subtitle = if (subtitle.length > 300) subtitle.take(300) + "..." else subtitle,
                    type = FeedType.CRAWLED,
                    timestamp = timestamp,
                    source = sourceName,
                    url = articleUrl,
                    keyword = keyword
                ))
            }
        }
        return items
    }
}
