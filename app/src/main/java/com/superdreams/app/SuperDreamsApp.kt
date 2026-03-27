package com.superdreams.app

import android.app.Application
import androidx.work.*
import com.superdreams.app.crawler.ContentCrawler
import com.superdreams.app.crawler.CrawlWorker
import com.superdreams.app.data.FeedRepository
import com.superdreams.app.widget.SuperDreamsWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SuperDreamsApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scheduleDailyCrawl()
        ensureStartupNews()
    }

    fun scheduleDailyCrawl() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Calculate delay to next 9:00 AM
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val delayMs = target.timeInMillis - now.timeInMillis

        val dailyWork = PeriodicWorkRequestBuilder<CrawlWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CrawlWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWork
        )
    }

    /**
     * Trigger an immediate one-time crawl (e.g. user manually refreshes).
     */
    fun triggerImmediateCrawl() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeWork = OneTimeWorkRequestBuilder<CrawlWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(oneTimeWork)
    }

    private fun ensureStartupNews() {
        appScope.launch {
            val feedRepo = FeedRepository.getInstance(applicationContext)
            if (feedRepo.hasCrawledNews()) return@launch

            val defaultKeywords = listOf("AI", "模型", "openclaw")
            val crawledItems = ContentCrawler().crawlAll(defaultKeywords, maxItems = 20)
            if (crawledItems.isNotEmpty()) {
                feedRepo.replaceCrawledItems(crawledItems)
                SuperDreamsWidget.refreshWidget(applicationContext)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        appScope.cancel()
    }
}
