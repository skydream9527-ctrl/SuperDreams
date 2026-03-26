package com.superdreams.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.superdreams.app.data.FeedRepository
import com.superdreams.app.ui.FeedAdapter
import com.superdreams.app.ui.KeywordActivity
import com.superdreams.app.ui.HistoryActivity
import com.superdreams.app.ui.TodoActivity
import com.superdreams.app.widget.SuperDreamsWidget

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FeedAdapter
    private lateinit var repository: FeedRepository

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
            refreshList()
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
                    refreshList()
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
    }

    override fun onResume() {
        super.onResume()
        refreshList()
        checkNotificationAccess()
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
}
