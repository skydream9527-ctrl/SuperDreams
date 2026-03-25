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
        val isExpanded = expandedIds.contains(item.id)
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
            holder.expandedHint.text = if (item.url.isNotEmpty()) "点击查看详情 →" else "点击查看 →"
        } else {
            holder.title.maxLines = 1
        }

        // Click handler: toggle expand → open detail
        holder.cardRoot.setOnClickListener {
            val position = holder.adapterPosition
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener
            val currentItem = items[position]
            if (expandedIds.contains(currentItem.id)) {
                // Already expanded → open detail
                val context = holder.itemView.context
                val intent = Intent(context, DetailActivity::class.java).apply {
                    putExtra(DetailActivity.EXTRA_TITLE, currentItem.title)
                    // For crawled items, build a richer content string for the detail page
                    val detailContent = if (currentItem.type == com.superdreams.app.data.FeedType.CRAWLED && currentItem.source.isNotEmpty()) {
                        buildString {
                            append(currentItem.subtitle)
                            if (currentItem.url.isNotEmpty()) {
                                append("\n\n📖 此处为摘要内容，点击下方按钮可在浏览器中阅读完整原文。")
                            }
                        }
                    } else {
                        currentItem.subtitle
                    }
                    putExtra(DetailActivity.EXTRA_CONTENT, detailContent)
                    putExtra(DetailActivity.EXTRA_SOURCE, currentItem.source)
                    putExtra(DetailActivity.EXTRA_URL, currentItem.url)
                    putExtra(DetailActivity.EXTRA_KEYWORD, currentItem.keyword)
                    putExtra(DetailActivity.EXTRA_TYPE, currentItem.type.name)
                    putExtra(DetailActivity.EXTRA_COLOR, PASTEL_COLORS[
                        Math.abs(currentItem.id.hashCode()) % PASTEL_COLORS.size
                    ])
                }
                context.startActivity(intent)
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

    override fun getItemCount(): Int = items.size

    fun getItemAt(position: Int): FeedItem = items[position]

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
