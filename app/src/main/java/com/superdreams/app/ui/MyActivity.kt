package com.superdreams.app.ui

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.superdreams.app.R
import com.superdreams.app.data.AppTheme
import com.superdreams.app.data.FeedItem
import com.superdreams.app.data.FeedType
import com.superdreams.app.data.HistoryRepository
import com.superdreams.app.data.ThemePreference
import java.util.concurrent.TimeUnit

class MyActivity : AppCompatActivity() {

    private lateinit var historyRepo: HistoryRepository
    private lateinit var adapter: FeedAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var countView: TextView
    private lateinit var btnSettings: Button
    private lateinit var tabNotification: Button
    private lateinit var tabNews: Button
    private lateinit var tabPush: Button
    private lateinit var tabTodo: Button
    private var allItems: List<FeedItem> = emptyList()
    private var currentTab: StorageTab = StorageTab.NOTIFICATION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my)

        historyRepo = HistoryRepository.getInstance(this)
        recyclerView = findViewById(R.id.my_list)
        emptyView = findViewById(R.id.my_empty)
        countView = findViewById(R.id.my_count)
        btnSettings = findViewById(R.id.btn_settings)
        tabNotification = findViewById(R.id.tab_notification)
        tabNews = findViewById(R.id.tab_news)
        tabPush = findViewById(R.id.tab_push)
        tabTodo = findViewById(R.id.tab_todo)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FeedAdapter(mutableListOf()) { }
        recyclerView.adapter = adapter

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        btnSettings.setOnClickListener { showThemeDialog() }

        tabNotification.setOnClickListener { setTab(StorageTab.NOTIFICATION) }
        tabNews.setOnClickListener { setTab(StorageTab.NEWS) }
        tabPush.setOnClickListener { setTab(StorageTab.PUSH) }
        tabTodo.setOnClickListener { setTab(StorageTab.TODO) }

        applyTheme()
        reloadItems()
        setTab(StorageTab.NOTIFICATION)
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
        reloadItems()
        refreshList()
    }

    private fun setTab(tab: StorageTab) {
        currentTab = tab
        updateTabStyles()
        refreshList()
    }

    private fun reloadItems() {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        allItems = historyRepo.getHistory().filter { it.timestamp >= cutoff }
    }

    private fun refreshList() {
        val filtered = when (currentTab) {
            StorageTab.NOTIFICATION -> allItems.filter { it.type == FeedType.NOTIFICATION }
            StorageTab.NEWS -> allItems.filter { it.type == FeedType.NEWS }
            StorageTab.PUSH -> allItems.filter { it.type == FeedType.CRAWLED }
            StorageTab.TODO -> allItems.filter { it.type == FeedType.TODO }
        }

        adapter.updateItems(filtered.toMutableList())
        countView.text = "近30天共 ${filtered.size} 条"
        if (filtered.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }

    private fun showThemeDialog() {
        val options = arrayOf("薰衣草", "薄荷绿", "蜜桃粉")
        val current = ThemePreference.getTheme(this)
        val selectedIndex = when (current) {
            AppTheme.LAVENDER -> 0
            AppTheme.MINT -> 1
            AppTheme.PEACH -> 2
        }
        var pendingIndex = selectedIndex
        AlertDialog.Builder(this)
            .setTitle("主题颜色")
            .setSingleChoiceItems(options, selectedIndex) { _, which ->
                pendingIndex = which
            }
            .setPositiveButton("应用") { _, _ ->
                val theme = when (pendingIndex) {
                    1 -> AppTheme.MINT
                    2 -> AppTheme.PEACH
                    else -> AppTheme.LAVENDER
                }
                ThemePreference.setTheme(this, theme)
                applyTheme()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun applyTheme() {
        val palette = ThemePreference.getPalette(this)
        findViewById<View>(R.id.my_root).setBackgroundColor(palette.rootColor)
        findViewById<View>(R.id.my_header).background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(palette.headerStartColor, palette.headerEndColor)
            cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 24f, 24f, 24f, 24f)
        }
        findViewById<TextView>(R.id.my_title).setTextColor(palette.titleTextColor)
        findViewById<TextView>(R.id.my_subtitle).setTextColor(palette.subtitleTextColor)
        countView.setTextColor(palette.hintTextColor)
        emptyView.setTextColor(palette.hintTextColor)
        btnSettings.background = createRoundedBackground(
            palette.secondaryFillColor,
            palette.secondaryStrokeColor,
            14f
        )
        btnSettings.setTextColor(palette.titleTextColor)
        updateTabStyles()
    }

    private fun updateTabStyles() {
        val palette = ThemePreference.getPalette(this)
        val selectedBackground = createRoundedBackground(
            palette.primaryFillColor,
            palette.primaryStrokeColor,
            14f
        )
        val normalBackground = createRoundedBackground(
            palette.secondaryFillColor,
            palette.secondaryStrokeColor,
            14f
        )
        val tabs = listOf(
            tabNotification to StorageTab.NOTIFICATION,
            tabNews to StorageTab.NEWS,
            tabPush to StorageTab.PUSH,
            tabTodo to StorageTab.TODO
        )
        tabs.forEach { (button, tab) ->
            button.background = if (tab == currentTab) selectedBackground.constantState?.newDrawable()?.mutate()
            else normalBackground.constantState?.newDrawable()?.mutate()
            button.setTextColor(palette.titleTextColor)
        }
    }

    private fun createRoundedBackground(fillColor: Int, strokeColor: Int, radiusDp: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp * resources.displayMetrics.density
            setColor(fillColor)
            setStroke((1f * resources.displayMetrics.density).toInt().coerceAtLeast(1), strokeColor)
        }
    }
}

enum class StorageTab {
    NOTIFICATION, NEWS, PUSH, TODO
}
