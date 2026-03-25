package com.superdreams.app.crawler

import com.superdreams.app.data.FeedItem
import com.superdreams.app.data.FeedType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

class ContentCrawler {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private const val MAX_RESULTS_PER_SOURCE = 5
    }

    /**
     * Crawl all sources for the given keywords and return aggregated feed items.
     */
    fun crawlAll(keywords: List<String>): List<FeedItem> {
        val allItems = mutableListOf<FeedItem>()
        for (keyword in keywords) {
            try {
                allItems.addAll(crawlBaiduNews(keyword))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                allItems.addAll(crawlBingNews(keyword))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                allItems.addAll(crawlSogouNews(keyword))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Deduplicate by title similarity and shuffle for variety
        return allItems
            .distinctBy { it.title.take(20) }
            .shuffled()
    }

    /**
     * Crawl Baidu News search results.
     */
    private fun crawlBaiduNews(keyword: String): List<FeedItem> {
        val encoded = URLEncoder.encode(keyword, "UTF-8")
        val url = "https://news.baidu.com/ns?word=$encoded&tn=news&from=news&cl=2&rn=10"
        val html = fetchPage(url) ?: return emptyList()

        val doc = Jsoup.parse(html)
        val items = mutableListOf<FeedItem>()

        // Baidu News result items
        val results = doc.select("div.result, div[class*=result]")
        for (result in results.take(MAX_RESULTS_PER_SOURCE)) {
            val titleEl = result.selectFirst("h3 a, a[class*=title]") ?: continue
            val title = titleEl.text().trim()
            if (title.isEmpty()) continue

            val snippetEl = result.selectFirst("div.c-summary, div.c-abstract, span.c-font-normal, div[class*=summary]")
            val snippet = snippetEl?.text()?.trim() ?: ""

            val sourceEl = result.selectFirst("p.c-author span, span.c-color-gray, div[class*=source]")
            val sourceName = sourceEl?.text()?.trim() ?: "百度新闻"

            val linkUrl = titleEl.attr("href")

            items.add(FeedItem(
                id = UUID.randomUUID().toString(),
                title = title,
                subtitle = if (snippet.length > 80) snippet.take(80) + "..." else snippet,
                type = FeedType.CRAWLED,
                source = sourceName,
                url = linkUrl,
                keyword = keyword
            ))
        }
        return items
    }

    /**
     * Crawl Bing News (Chinese) search results.
     */
    private fun crawlBingNews(keyword: String): List<FeedItem> {
        val encoded = URLEncoder.encode(keyword, "UTF-8")
        val url = "https://cn.bing.com/news/search?q=$encoded&FORM=HDRSC6"
        val html = fetchPage(url) ?: return emptyList()

        val doc = Jsoup.parse(html)
        val items = mutableListOf<FeedItem>()

        val cards = doc.select("div.news-card, div.newsitem, a.news-card")
        for (card in cards.take(MAX_RESULTS_PER_SOURCE)) {
            val titleEl = card.selectFirst("a.title, div.title a, span.title") ?: continue
            val title = titleEl.text().trim()
            if (title.isEmpty()) continue

            val snippetEl = card.selectFirst("div.snippet, div.descripion, p")
            val snippet = snippetEl?.text()?.trim() ?: ""

            val sourceEl = card.selectFirst("div.source span, span.source")
            val sourceName = sourceEl?.text()?.trim() ?: "必应新闻"

            val linkUrl = titleEl.attr("href").let {
                if (it.startsWith("http")) it else "https://cn.bing.com$it"
            }

            items.add(FeedItem(
                id = UUID.randomUUID().toString(),
                title = title,
                subtitle = if (snippet.length > 80) snippet.take(80) + "..." else snippet,
                type = FeedType.CRAWLED,
                source = sourceName,
                url = linkUrl,
                keyword = keyword
            ))
        }
        return items
    }

    /**
     * Crawl Sogou News search results.
     */
    private fun crawlSogouNews(keyword: String): List<FeedItem> {
        val encoded = URLEncoder.encode(keyword, "UTF-8")
        val url = "https://news.sogou.com/news?query=$encoded&mode=1"
        val html = fetchPage(url) ?: return emptyList()

        val doc = Jsoup.parse(html)
        val items = mutableListOf<FeedItem>()

        val results = doc.select("div.news-list li, div.results div.vrwrap, div.rb")
        for (result in results.take(MAX_RESULTS_PER_SOURCE)) {
            val titleEl = result.selectFirst("h3 a, a.title, p.news-title a") ?: continue
            val title = titleEl.text().trim()
            if (title.isEmpty()) continue

            val snippetEl = result.selectFirst("p.news-txt, div.news-detail, p.txt-info")
            val snippet = snippetEl?.text()?.trim() ?: ""

            val sourceEl = result.selectFirst("p.news-from span, span.news-from")
            val sourceName = sourceEl?.text()?.trim() ?: "搜狗新闻"

            val linkUrl = titleEl.attr("href").let {
                if (it.startsWith("http")) it else "https://news.sogou.com$it"
            }

            items.add(FeedItem(
                id = UUID.randomUUID().toString(),
                title = title,
                subtitle = if (snippet.length > 80) snippet.take(80) + "..." else snippet,
                type = FeedType.CRAWLED,
                source = sourceName,
                url = linkUrl,
                keyword = keyword
            ))
        }
        return items
    }

    /**
     * Fetch a web page and return HTML content.
     */
    private fun fetchPage(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
