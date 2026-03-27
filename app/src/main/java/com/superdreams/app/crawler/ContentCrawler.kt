package com.superdreams.app.crawler

import com.superdreams.app.data.FeedItem
import com.superdreams.app.data.FeedType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
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
    fun crawlAll(keywords: List<String>, maxItems: Int = Int.MAX_VALUE): List<FeedItem> {
        if (keywords.isEmpty()) return emptyList()
        val allItems = mutableListOf<FeedItem>()
        for (keyword in keywords) {
            try {
                val newsApiItems = fetchNewsFromNewsApi(keyword)
                if (newsApiItems.isNotEmpty()) {
                    allItems.addAll(newsApiItems)
                } else {
                    allItems.addAll(fetchNewsFromRss(keyword))
                }
            } catch (e: Exception) {
                try {
                    allItems.addAll(fetchNewsFromRss(keyword))
                } catch (_: Exception) {
                }
            }
        }
        // Deduplicate by title prefix and shuffle for variety
        val limit = if (maxItems <= 0) 1 else maxItems
        return allItems
            .distinctBy { it.title.take(30) }
            .shuffled()
            .take(limit)
    }

    /**
     * Query NewsAPI /v2/everything for a single keyword.
     */
    private fun fetchNewsFromNewsApi(keyword: String): List<FeedItem> {
        val encoded = URLEncoder.encode(keyword, "UTF-8")
        val url = "$BASE_URL?q=$encoded&pageSize=$PAGE_SIZE&sortBy=publishedAt&apiKey=$API_KEY"

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "SuperDreams/1.0")
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
                val mergedContent = buildString {
                    if (description.isNotEmpty()) {
                        append(description)
                    }
                    if (content.isNotEmpty() && !content.equals(description, ignoreCase = true)) {
                        if (isNotEmpty()) append("\n\n")
                        append(content)
                    }
                }.trim()
                val subtitle = when {
                    description.isNotEmpty() -> description
                    content.isNotEmpty() -> content
                    else -> ""
                }

                val sourceName = article.optJSONObject("source")?.optString("name", "") ?: ""
                val articleUrl = article.optString("url", "")
                val publishedAt = article.optString("publishedAt", "")

                val timestamp = parseTimestamp(
                    value = publishedAt,
                    patterns = listOf("yyyy-MM-dd'T'HH:mm:ss'Z'")
                )

                items.add(FeedItem(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    subtitle = if (subtitle.length > 300) subtitle.take(300) + "..." else subtitle,
                    content = mergedContent,
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

    private fun fetchNewsFromRss(keyword: String): List<FeedItem> {
        val encoded = URLEncoder.encode(keyword, "UTF-8")
        val rssUrl = "https://news.google.com/rss/search?q=$encoded&hl=zh-CN&gl=CN&ceid=CN:zh-Hans"
        val request = Request.Builder()
            .url(rssUrl)
            .header("Accept", "application/rss+xml, application/xml;q=0.9, */*;q=0.8")
            .header("User-Agent", "SuperDreams/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val xml = response.body?.string() ?: return emptyList()
            val document = Jsoup.parse(xml, "", Parser.xmlParser())
            return document.select("item").mapNotNull { item ->
                val title = item.selectFirst("title")?.text()?.trim().orEmpty()
                if (title.isEmpty()) return@mapNotNull null
                val descriptionHtml = item.selectFirst("description")?.text().orEmpty()
                val description = Jsoup.parse(descriptionHtml).text().trim()
                val url = item.selectFirst("link")?.text()?.trim().orEmpty()
                val pubDate = item.selectFirst("pubDate")?.text()?.trim().orEmpty()
                val sourceName = item.selectFirst("source")?.text()?.trim().orEmpty()

                val subtitle = if (description.isNotEmpty()) description else title
                FeedItem(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    subtitle = if (subtitle.length > 300) subtitle.take(300) + "..." else subtitle,
                    content = description,
                    type = FeedType.CRAWLED,
                    timestamp = parseTimestamp(
                        value = pubDate,
                        patterns = listOf(
                            "EEE, dd MMM yyyy HH:mm:ss Z",
                            "EEE, dd MMM yyyy HH:mm:ss zzz"
                        )
                    ),
                    source = sourceName,
                    url = url,
                    keyword = keyword
                )
            }
        }
    }

    private fun parseTimestamp(value: String, patterns: List<String>): Long {
        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = format.parse(value)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return System.currentTimeMillis()
    }
}
