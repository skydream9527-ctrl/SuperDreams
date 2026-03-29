package com.superdreams.app.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.superdreams.app.R
import com.superdreams.app.data.FeedItem
import com.superdreams.app.data.FeedType

class FeedAdapter(
    private var items: MutableList<FeedItem>,
    private val onDismiss: (FeedItem) -> Unit
) : RecyclerView.Adapter<FeedAdapter.ViewHolder>() {

    // Track which items are expanded by ID
    private val expandedIds = mutableSetOf<String>()

    companion object {
        val PASTEL_COLORS = intArrayOf(
            Color.parseColor("#FFEDE9"),
            Color.parseColor("#E8F0FE"),
            Color.parseColor("#FFF8E1"),
            Color.parseColor("#F3E8FD"),
            Color.parseColor("#E6F9ED"),
            Color.parseColor("#FDE8F0"),
            Color.parseColor("#E0F7FA"),
            Color.parseColor("#FFF3E0")
        )
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardRoot: View = view.findViewById(R.id.feed_card_root)
        val typeIndicator: View = view.findViewById(R.id.feed_type_indicator)
        val typeIcon: ImageView = view.findViewById(R.id.feed_type_icon)
        val title: TextView = view.findViewById(R.id.feed_title)
        val subtitle: TextView = view.findViewById(R.id.feed_subtitle)
        // Expanded section
        val expandedSection: LinearLayout = view.findViewById(R.id.feed_expanded_section)
        val expandedTitle: TextView = view.findViewById(R.id.feed_expanded_title)
        val expandedContent: TextView = view.findViewById(R.id.feed_expanded_content)
        val expandedSource: TextView = view.findViewById(R.id.feed_expanded_source)
        val expandedHint: TextView = view.findViewById(R.id.feed_expanded_hint)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.feed_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val supportsExpand = item.type == FeedType.NEWS || item.type == FeedType.CRAWLED
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle

        // Pastel background
        val colorIndex = Math.abs(item.id.hashCode()) % PASTEL_COLORS.size
        val bg = GradientDrawable().apply {
            setColor(PASTEL_COLORS[colorIndex])
            cornerRadius = 12f * holder.cardRoot.context.resources.displayMetrics.density
        }
        holder.cardRoot.background = bg

        // Type indicator & icon
        when (item.type) {
            FeedType.NEWS -> {
                holder.typeIcon.setImageResource(R.drawable.ic_news)
                holder.typeIndicator.setBackgroundResource(R.drawable.type_indicator_news)
            }
            FeedType.TODO -> {
                holder.typeIcon.setImageResource(R.drawable.ic_todo)
                holder.typeIndicator.setBackgroundResource(R.drawable.type_indicator_todo)
            }
            FeedType.CRAWLED -> {
                holder.typeIcon.setImageResource(R.drawable.ic_news)
                holder.typeIndicator.setBackgroundResource(R.drawable.type_indicator_news)
                if (item.source.isNotEmpty()) {
                    holder.subtitle.text = "来源: ${item.source}"
                }
            }
            FeedType.NOTIFICATION -> {
                holder.typeIcon.setImageResource(R.drawable.ic_notification)
                holder.typeIndicator.setBackgroundResource(R.drawable.type_indicator_notification)
                if (item.source.isNotEmpty()) {
                    holder.subtitle.text = "📱 ${item.source}"
                }
            }
        }

        // Expanded state
        val isExpanded = supportsExpand && expandedIds.contains(item.id)
        holder.expandedSection.visibility = if (isExpanded) View.VISIBLE else View.GONE

        if (isExpanded) {
            holder.expandedTitle.text = item.title
            holder.expandedContent.text = item.subtitle
            holder.title.maxLines = 2

            val sourceInfo = buildString {
                if (item.source.isNotEmpty()) append(item.source)
                if (item.keyword.isNotEmpty()) {
                    if (isNotEmpty()) append(" · ")
                    append("关键词: ${item.keyword}")
                }
                if (item.type == FeedType.TODO) {
                    if (isNotEmpty()) append(" · ")
                    append("待办事项")
                }
            }
            holder.expandedSource.text = sourceInfo
            holder.expandedHint.text = when {
                item.type == FeedType.NOTIFICATION -> "打开来源应用 →"
                item.url.isNotEmpty() -> "点击查看详情 →"
                else -> "点击查看 →"
            }
        } else {
            holder.title.maxLines = 1
        }

        // Click handler: toggle expand → open detail
        holder.cardRoot.setOnClickListener {
            val position = holder.adapterPosition
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener
            val currentItem = items[position]
            val canExpand = currentItem.type == FeedType.NEWS || currentItem.type == FeedType.CRAWLED
            if (!canExpand) {
                openDetail(holder, currentItem)
                return@setOnClickListener
            }
            if (expandedIds.contains(currentItem.id)) {
                openDetail(holder, currentItem)
            } else {
                // Collapse any other expanded item
                val previouslyExpanded = expandedIds.toSet()
                expandedIds.clear()
                expandedIds.add(currentItem.id)
                // Notify changes
                previouslyExpanded.forEach { id ->
                    val idx = items.indexOfFirst { it.id == id }
                    if (idx >= 0) notifyItemChanged(idx)
                }
                notifyItemChanged(position)
            }
        }
    }

    private fun openDetail(holder: ViewHolder, item: FeedItem) {
        val context = holder.itemView.context

        // For NEWS/CRAWLED items with a URL, open directly in app's built-in browser
        if ((item.type == FeedType.NEWS || item.type == FeedType.CRAWLED) && item.url.isNotEmpty()) {
            if (context is com.superdreams.app.MainActivity) {
                context.openInAppBrowser(item.url)
                return
            }
        }

        val detailContent = when {
            item.content.isNotBlank() -> item.content
            item.subtitle.isNotBlank() -> item.subtitle
            else -> item.title
        }
        val intent = Intent(context, DetailActivity::class.java).apply {
            putExtra(DetailActivity.EXTRA_TITLE, item.title)
            putExtra(DetailActivity.EXTRA_CONTENT, detailContent)
            putExtra(DetailActivity.EXTRA_SOURCE, item.source)
            putExtra(DetailActivity.EXTRA_URL, item.url)
            putExtra(DetailActivity.EXTRA_KEYWORD, item.keyword)
            putExtra(DetailActivity.EXTRA_TYPE, item.type.name)
            putExtra(DetailActivity.EXTRA_COLOR, PASTEL_COLORS[
                Math.abs(item.id.hashCode()) % PASTEL_COLORS.size
            ])
        }
        context.startActivity(intent)
    }

    override fun getItemCount(): Int = items.size

    fun getItemAt(position: Int): FeedItem = items[position]

    fun removeItemById(itemId: String): FeedItem? {
        val index = items.indexOfFirst { it.id == itemId }
        if (index < 0) return null
        val removed = items.removeAt(index)
        expandedIds.remove(itemId)
        notifyItemRemoved(index)
        return removed
    }

    fun updateItems(newItems: MutableList<FeedItem>) {
        val diffCallback = FeedDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items = newItems
        expandedIds.clear()
        diffResult.dispatchUpdatesTo(this)
    }

    private class FeedDiffCallback(
        private val oldList: List<FeedItem>,
        private val newList: List<FeedItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int) =
            oldList[oldPos].id == newList[newPos].id
        override fun areContentsTheSame(oldPos: Int, newPos: Int) =
            oldList[oldPos] == newList[newPos]
    }
}
