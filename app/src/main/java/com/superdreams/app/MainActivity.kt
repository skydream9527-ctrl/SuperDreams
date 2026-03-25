package com.superdreams.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.superdreams.app.data.FeedRepository
import com.superdreams.app.ui.FeedAdapter
import com.superdreams.app.ui.KeywordActivity
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

        adapter = FeedAdapter(repository.getItems().toMutableList()) { item ->
            repository.removeItem(item.id)
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
                    repository.removeItem(item.id)
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
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        adapter.updateItems(repository.getItems().toMutableList())
        SuperDreamsWidget.refreshWidget(this)
    }

    private fun requestWidgetPin() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val widgetProvider = ComponentName(this, SuperDreamsWidget::class.java)
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(widgetProvider, null, null)
        } else {
            Toast.makeText(this, "请长按桌面手动添加小部件", Toast.LENGTH_LONG).show()
        }
    }
}
