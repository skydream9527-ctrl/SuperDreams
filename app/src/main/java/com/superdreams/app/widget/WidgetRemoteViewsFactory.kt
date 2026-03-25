package com.superdreams.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.superdreams.app.R
import com.superdreams.app.data.FeedItem
import com.superdreams.app.data.FeedRepository
import com.superdreams.app.data.FeedType

class WidgetRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var items = listOf<FeedItem>()
    private val repository = FeedRepository.getInstance(context)

    companion object {
        val PASTEL_COLORS = intArrayOf(
            Color.parseColor("#FFEDE9"),  // pastel red
            Color.parseColor("#E8F0FE"),  // pastel blue
            Color.parseColor("#FFF8E1"),  // pastel yellow
            Color.parseColor("#F3E8FD"),  // pastel purple
            Color.parseColor("#E6F9ED"),  // pastel green
            Color.parseColor("#FDE8F0"),  // pastel pink
            Color.parseColor("#E0F7FA"),  // pastel cyan
            Color.parseColor("#FFF3E0")   // pastel orange
        )
    }

    override fun onCreate() {
        items = repository.getItemsWithTodos(context)
    }

    override fun onDataSetChanged() {
        items = repository.getItemsWithTodos(context)
    }

    override fun onDestroy() {}

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= items.size) {
            return RemoteViews(context.packageName, R.layout.widget_item)
        }
        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_item)

        views.setTextViewText(R.id.item_title, item.title)
        views.setTextViewText(R.id.item_subtitle, item.subtitle)

        // Apply deterministic pastel background based on item ID
        val colorIndex = Math.abs(item.id.hashCode()) % PASTEL_COLORS.size
        views.setInt(R.id.item_card_root, "setBackgroundColor", PASTEL_COLORS[colorIndex])

        when (item.type) {
            FeedType.NEWS -> {
                views.setImageViewResource(R.id.item_type_icon, R.drawable.ic_news)
                views.setInt(R.id.item_type_indicator, "setBackgroundResource", R.drawable.type_indicator_news)
            }
            FeedType.TODO -> {
                views.setImageViewResource(R.id.item_type_icon, R.drawable.ic_todo)
                views.setInt(R.id.item_type_indicator, "setBackgroundResource", R.drawable.type_indicator_todo)
            }
            FeedType.CRAWLED -> {
                views.setImageViewResource(R.id.item_type_icon, R.drawable.ic_news)
                views.setInt(R.id.item_type_indicator, "setBackgroundResource", R.drawable.type_indicator_news)
                if (item.source.isNotEmpty()) {
                    views.setTextViewText(R.id.item_subtitle, "来源: ${item.source} · ${item.subtitle}")
                }
            }
        }

        val fillInIntent = Intent().apply {
            putExtra(DismissActionReceiver.EXTRA_ITEM_ID, item.id)
        }
        views.setOnClickFillInIntent(R.id.item_dismiss_btn, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
}
