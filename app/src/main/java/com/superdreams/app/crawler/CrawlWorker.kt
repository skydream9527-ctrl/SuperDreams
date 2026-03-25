package com.superdreams.app.crawler

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.superdreams.app.data.FeedRepository
import com.superdreams.app.data.KeywordRepository
import com.superdreams.app.widget.SuperDreamsWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CrawlWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "CrawlWorker"
        const val WORK_NAME = "daily_content_crawl"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting daily content crawl...")

            val keywordRepo = KeywordRepository.getInstance(applicationContext)
            val feedRepo = FeedRepository.getInstance(applicationContext)
            val keywords = keywordRepo.getKeywords()

            if (keywords.isEmpty()) {
                Log.w(TAG, "No keywords configured, skipping crawl")
                return@withContext Result.success()
            }

            val crawler = ContentCrawler()
            val crawledItems = crawler.crawlAll(keywords)

            Log.i(TAG, "Crawled ${crawledItems.size} items for ${keywords.size} keywords")

            if (crawledItems.isNotEmpty()) {
                feedRepo.replaceCrawledItems(crawledItems)
                SuperDreamsWidget.refreshWidget(applicationContext)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Crawl failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
