package com.superdreams.app.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.superdreams.app.R
import com.superdreams.app.data.FeedType
import com.superdreams.app.data.HistoryRepository

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyRepo: HistoryRepository
    private lateinit var adapter: FeedAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var countText: TextView

    private var currentFilter: FeedType? = null

    // Tab buttons
    private lateinit var tabAll: Button
    private lateinit var tabNews: Button
    private lateinit var tabNotification: Button
    private lateinit var tabTodo: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyRepo = HistoryRepository.getInstance(this)

        recyclerView = findViewById(R.id.history_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        emptyView = findViewById(R.id.history_empty)
        countText = findViewById(R.id.history_count)

        adapter = FeedAdapter(mutableListOf()) { /* no dismiss action in history */ }
        recyclerView.adapter = adapter

        // Filter tabs
        tabAll = findViewById(R.id.tab_all)
        tabNews = findViewById(R.id.tab_news)
        tabNotification = findViewById(R.id.tab_notification)
        tabTodo = findViewById(R.id.tab_todo)

        tabAll.setOnClickListener { setFilter(null) }
        tabNews.setOnClickListener { setFilter(FeedType.NEWS) }
        tabNotification.setOnClickListener { setFilter(FeedType.NOTIFICATION) }
        tabTodo.setOnClickListener { setFilter(FeedType.TODO) }

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun setFilter(type: FeedType?) {
        currentFilter = type
        updateTabStyles()
        refreshList()
    }

    private fun updateTabStyles() {
        // Active tab gets crawl_bg (brighter), inactive gets add_widget_bg (dimmer)
        tabAll.setBackgroundResource(
            if (currentFilter == null) R.drawable.btn_crawl_bg else R.drawable.btn_add_widget_bg
        )
        tabNews.setBackgroundResource(
            if (currentFilter == FeedType.NEWS || currentFilter == FeedType.CRAWLED)
                R.drawable.btn_crawl_bg else R.drawable.btn_add_widget_bg
        )
        tabNotification.setBackgroundResource(
            if (currentFilter == FeedType.NOTIFICATION) R.drawable.btn_crawl_bg else R.drawable.btn_add_widget_bg
        )
        tabTodo.setBackgroundResource(
            if (currentFilter == FeedType.TODO) R.drawable.btn_crawl_bg else R.drawable.btn_add_widget_bg
        )
    }

    private fun refreshList() {
        val items = when (currentFilter) {
            FeedType.NEWS -> {
                // "新闻" tab shows both NEWS and CRAWLED types
                val all = historyRepo.getHistory()
                all.filter { it.type == FeedType.NEWS || it.type == FeedType.CRAWLED }
            }
            else -> historyRepo.getHistoryByType(currentFilter)
        }

        adapter.updateItems(items.toMutableList())
        countText.text = "${items.size} 条记录"

        if (items.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }
}
