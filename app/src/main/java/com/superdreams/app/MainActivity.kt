package com.superdreams.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.superdreams.app.data.FeedRepository
import com.superdreams.app.data.ThemePreference
import com.superdreams.app.ui.FeedAdapter
import com.superdreams.app.ui.AppListActivity
import com.superdreams.app.ui.KeywordActivity
import com.superdreams.app.ui.HistoryActivity
import com.superdreams.app.ui.MyActivity
import com.superdreams.app.ui.TodoActivity
import com.superdreams.app.widget.SuperDreamsWidget
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FeedAdapter
    private lateinit var repository: FeedRepository
    private lateinit var pageList: View
    private lateinit var pageBrowser: View
    private lateinit var pageMy: View
    private lateinit var tabList: View
    private lateinit var tabBrowser: View
    private lateinit var tabApps: View
    private lateinit var tabMy: View
    private lateinit var browserSpinner: Spinner
    private lateinit var browserInput: EditText
    private lateinit var browserGoButton: Button
    private lateinit var browserProgress: ProgressBar
    private lateinit var browserWebView: WebView
    private var browserWebViewInitialized = false
    private var browserLoaded = false
    private var currentPage = HomePage.LIST
    private val searchEngines = linkedMapOf(
        "百度" to "https://www.baidu.com/s?wd=",
        "搜狗" to "https://www.sogou.com/web?query=",
        "必应" to "https://www.bing.com/search?q=",
        "抖音" to "https://www.douyin.com/search/"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = FeedRepository.getInstance(this)

        recyclerView = findViewById(R.id.main_feed_list)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FeedAdapter(repository.getItemsWithTodos(this).toMutableList()) { item ->
            if (item.type == com.superdreams.app.data.FeedType.NOTIFICATION) {
                com.superdreams.app.data.NotificationRepository.getInstance(this).removeNotification(item.id)
            } else {
                repository.removeItem(item.id)
            }
            adapter.removeItemById(item.id)
            SuperDreamsWidget.refreshWidget(this)
        }
        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = adapter.getItemAt(position)
                    if (item.type == com.superdreams.app.data.FeedType.NOTIFICATION) {
                        com.superdreams.app.data.NotificationRepository.getInstance(this@MainActivity).removeNotification(item.id)
                    } else {
                        repository.removeItem(item.id)
                    }
                    adapter.removeItemById(item.id)
                    SuperDreamsWidget.refreshWidget(this@MainActivity)
                }
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val alpha = 1.0f - Math.abs(dX) / itemView.width.toFloat()
                    itemView.alpha = alpha
                    itemView.translationX = dX
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        pageList = findViewById(R.id.page_list)
        pageBrowser = findViewById(R.id.page_browser)
        pageMy = findViewById(R.id.page_my)
        tabList = findViewById(R.id.tab_list)
        tabBrowser = findViewById(R.id.tab_browser)
        tabApps = findViewById(R.id.tab_apps)
        tabMy = findViewById(R.id.tab_my)
        browserSpinner = findViewById(R.id.browser_engine_spinner)
        browserInput = findViewById(R.id.browser_query_input)
        browserGoButton = findViewById(R.id.browser_go_btn)
        browserProgress = findViewById(R.id.browser_progress)
        browserWebView = findViewById(R.id.browser_webview)
        browserWebViewInitialized = true

        setupBrowser()
        setupBrowserHomepage()
        setupBottomTabs()
        applyHomeTheme()

        findViewById<Button>(R.id.btn_add_widget).setOnClickListener {
            requestWidgetPin()
        }

        findViewById<Button>(R.id.btn_manage_keywords).setOnClickListener {
            startActivity(Intent(this, KeywordActivity::class.java))
        }

        findViewById<Button>(R.id.btn_manage_todos).setOnClickListener {
            startActivity(Intent(this, TodoActivity::class.java))
        }

        findViewById<Button>(R.id.btn_history).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        findViewById<View>(R.id.btn_my).setOnClickListener {
            startActivity(Intent(this, MyActivity::class.java))
        }

        // Settings button inside the embedded My page
        findViewById<View>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, MyActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        applyHomeTheme()
        refreshList()
        checkNotificationAccess()
    }

    override fun onBackPressed() {
        if (currentPage == HomePage.BROWSER && browserWebViewInitialized && browserWebView.canGoBack()) {
            browserWebView.goBack()
            return
        }
        if (currentPage != HomePage.LIST) {
            switchPage(HomePage.LIST)
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        if (browserWebViewInitialized) {
            browserWebView.destroy()
        }
        super.onDestroy()
    }

    private fun checkNotificationAccess() {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this)
        if (!enabledPackages.contains(packageName)) {
            AlertDialog.Builder(this)
                .setTitle("需要通知访问权限")
                .setMessage("SuperDreams 需要「通知使用权」才能将手机推送消息展示在 Feed 中。\n\n请在接下来的设置页面中开启 SuperDreams 的通知访问权限。")
                .setPositiveButton("去授权") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setNegativeButton("暂不授权", null)
                .show()
        }
    }

    private fun refreshList() {
        adapter.updateItems(repository.getItemsWithTodos(this).toMutableList())
        SuperDreamsWidget.refreshWidget(this)
    }

    private fun requestWidgetPin() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val widgetProvider = ComponentName(this, SuperDreamsWidget::class.java)

        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(widgetProvider, null, null)
        } else {
            showWidgetGuideDialog()
        }
    }

    private fun showWidgetGuideDialog() {
        val msg = StringBuilder()
        msg.append("请按以下步骤手动添加 SuperDreams 小部件：\n\n")
        msg.append("1. 返回桌面，在空白处长按（约 1-2 秒）\n\n")
        msg.append("2. 点击弹出菜单中的「小部件」或「Widget」\n\n")
        msg.append("3. 在小部件列表中找到 SuperDreams\n\n")
        msg.append("4. 长按 SuperDreams 小部件，拖动到桌面上即可\n\n")
        msg.append("小米/华为设备：长按桌面 -> 小部件 -> 搜索 SuperDreams\n")
        msg.append("三星设备：长按桌面空白 -> Widget -> 找到本应用")

        AlertDialog.Builder(this)
            .setTitle("如何添加桌面小部件")
            .setMessage(msg.toString())
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun applyHomeTheme() {
        val palette = ThemePreference.getPalette(this)
        findViewById<View>(R.id.main_root).setBackgroundColor(palette.rootColor)
        findViewById<View>(R.id.home_header_container).background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(palette.headerStartColor, palette.headerEndColor)
            cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 24f, 24f, 24f, 24f)
        }
        findViewById<TextView>(R.id.home_title).setTextColor(palette.titleTextColor)
        findViewById<TextView>(R.id.home_subtitle).setTextColor(palette.subtitleTextColor)
        findViewById<TextView>(R.id.home_swipe_hint).setTextColor(palette.hintTextColor)

        val primaryBackground = createRoundedBackground(
            palette.primaryFillColor,
            palette.primaryStrokeColor,
            16f
        )
        val secondaryBackground = createRoundedBackground(
            palette.secondaryFillColor,
            palette.secondaryStrokeColor,
            16f
        )
        // btn_add_widget is primary, keyword/todo/history are secondary buttons in header
        // btn_my is now a LinearLayout avatar button — skip Button casting for it
        findViewById<Button>(R.id.btn_add_widget).background =
            primaryBackground.constantState?.newDrawable()?.mutate()
        listOf(
            R.id.btn_manage_keywords,
            R.id.btn_manage_todos,
            R.id.btn_history
        ).forEach { id ->
            findViewById<Button>(id).background = secondaryBackground.constantState?.newDrawable()?.mutate()
            findViewById<Button>(id).setTextColor(palette.titleTextColor)
        }
        browserInput.background = createRoundedBackground(
            palette.searchFillColor,
            palette.searchStrokeColor,
            14f
        )
        browserInput.setTextColor(palette.titleTextColor)
        browserInput.setHintTextColor(palette.subtitleTextColor)
        browserSpinner.background = createRoundedBackground(
            palette.secondaryFillColor,
            palette.secondaryStrokeColor,
            14f
        )
        browserGoButton.background = createRoundedBackground(
            palette.primaryFillColor,
            palette.primaryStrokeColor,
            14f
        )
        browserGoButton.setTextColor(palette.titleTextColor)
        updateBottomTabStyles()
    }

    private fun createRoundedBackground(fillColor: Int, strokeColor: Int, radiusDp: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp * resources.displayMetrics.density
            setColor(fillColor)
            setStroke((1f * resources.displayMetrics.density).toInt().coerceAtLeast(1), strokeColor)
        }
    }

    private fun setupBottomTabs() {
        tabList.setOnClickListener {
            switchPage(HomePage.LIST)
        }
        tabBrowser.setOnClickListener {
            switchPage(HomePage.BROWSER)
        }
        tabApps.setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }
        tabMy.setOnClickListener {
            switchPage(HomePage.MY)
        }
        switchPage(HomePage.LIST)
    }

    private fun switchPage(page: HomePage) {
        currentPage = page
        pageList.visibility = if (page == HomePage.LIST) View.VISIBLE else View.GONE
        pageBrowser.visibility = if (page == HomePage.BROWSER) View.VISIBLE else View.GONE
        pageMy.visibility = if (page == HomePage.MY) View.VISIBLE else View.GONE
        if (page == HomePage.BROWSER && !browserLoaded) {
            browserWebView.loadUrl("https://www.bing.com")
            browserLoaded = true
        }
        updateBottomTabStyles()
    }

    private fun updateBottomTabStyles() {
        val palette = ThemePreference.getPalette(this)
        val selectedColor = palette.primaryFillColor
        val normalColor = palette.secondaryFillColor
        val mapping = listOf(
            tabList to HomePage.LIST,
            tabBrowser to HomePage.BROWSER,
            tabApps to null,
            tabMy to HomePage.MY
        )
        mapping.forEach { (tab, page) ->
            val isSelected = page != null && page == currentPage
            tab.setBackgroundColor(if (isSelected) selectedColor else normalColor)
            // Update label text colors inside the LinearLayout tabs
            val labelId = when (tab.id) {
                R.id.tab_list -> R.id.tab_list_label
                R.id.tab_browser -> R.id.tab_browser_label
                R.id.tab_apps -> R.id.tab_apps_label
                R.id.tab_my -> R.id.tab_my_label
                else -> null
            }
            if (labelId != null) {
                findViewById<TextView>(labelId).setTextColor(
                    if (isSelected) palette.primaryStrokeColor else palette.hintTextColor
                )
            }
        }
    }

    private fun setupBrowserHomepage() {
        val engineMap = mapOf(
            R.id.engine_baidu to "https://www.baidu.com",
            R.id.engine_sogou to "https://www.sogou.com",
            R.id.engine_bing to "https://www.bing.com",
            R.id.engine_douyin to "https://www.douyin.com",
            R.id.engine_bilibili to "https://www.bilibili.com",
            R.id.engine_qianwen to "https://tongyi.aliyun.com",
            R.id.engine_doubao to "https://www.doubao.com",
            R.id.engine_zhihu to "https://www.zhihu.com"
        )
        engineMap.forEach { (viewId, url) ->
            findViewById<View>(viewId).setOnClickListener {
                switchPage(HomePage.BROWSER)
                browserWebView.loadUrl(url)
                browserLoaded = true
            }
        }
    }

    private fun setupBrowser() {
        val engineNames = searchEngines.keys.toList()
        browserSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            engineNames
        )

        browserWebView.settings.javaScriptEnabled = true
        browserWebView.settings.domStorageEnabled = true
        browserWebView.settings.loadsImagesAutomatically = true
        browserWebView.settings.useWideViewPort = true
        browserWebView.settings.loadWithOverviewMode = true
        browserWebView.webViewClient = WebViewClient()
        browserWebView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                browserProgress.progress = newProgress
                browserProgress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }
        }

        browserGoButton.setOnClickListener {
            openBrowserInput()
        }
        browserInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                openBrowserInput()
                true
            } else {
                false
            }
        }
    }

    private fun openBrowserInput() {
        val input = browserInput.text.toString().trim()
        if (input.isEmpty()) return
        val isUrl = input.startsWith("http://") ||
            input.startsWith("https://") ||
            (Patterns.WEB_URL.matcher(input).matches() && !input.contains(" "))
        val targetUrl = if (isUrl) {
            if (input.startsWith("http://") || input.startsWith("https://")) input else "https://$input"
        } else {
            val engine = browserSpinner.selectedItem?.toString().orEmpty()
            val prefix = searchEngines[engine] ?: searchEngines.values.first()
            prefix + URLEncoder.encode(input, "UTF-8")
        }
        browserWebView.loadUrl(targetUrl)
        browserLoaded = true
    }
}

enum class HomePage {
    LIST, BROWSER, MY
}
