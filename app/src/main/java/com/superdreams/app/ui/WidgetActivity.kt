package com.superdreams.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.superdreams.app.R
import com.superdreams.app.data.FeedRepository
import com.superdreams.app.widget.SuperDreamsWidget

class WidgetActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FeedAdapter
    private lateinit var repository: FeedRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget)

        repository = FeedRepository.getInstance(this)

        recyclerView = findViewById(R.id.feed_recycler_view)
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
                        com.superdreams.app.data.NotificationRepository.getInstance(this@WidgetActivity).removeNotification(item.id)
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

        findViewById<android.view.View>(R.id.widget_activity_root).setOnClickListener {
            finish()
        }

        recyclerView.setOnClickListener(null)
    }

    private fun refreshList() {
        adapter.updateItems(repository.getItemsWithTodos(this).toMutableList())
        SuperDreamsWidget.refreshWidget(this)
    }

    override fun onPause() {
        super.onPause()
        SuperDreamsWidget.refreshWidget(this)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, android.R.anim.fade_out)
    }
}
