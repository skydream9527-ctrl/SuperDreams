package com.superdreams.app.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardRoot: View = view.findViewById(R.id.feed_card_root)
        val typeIndicator: View = view.findViewById(R.id.feed_type_indicator)
        val typeIcon: ImageView = view.findViewById(R.id.feed_type_icon)
        val title: TextView = view.findViewById(R.id.feed_title)
        val subtitle: TextView = view.findViewById(R.id.feed_subtitle)
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

        // Apply deterministic pastel background with rounded corners
        val colorIndex = Math.abs(item.id.hashCode()) % PASTEL_COLORS.size
        val bg = GradientDrawable().apply {
            setColor(PASTEL_COLORS[colorIndex])
            cornerRadius = 12f * holder.cardRoot.context.resources.displayMetrics.density
        }
        holder.cardRoot.background = bg

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
                    holder.subtitle.text = "来源: ${item.source} · ${item.subtitle}"
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun getItemAt(position: Int): FeedItem = items[position]

    fun updateItems(newItems: MutableList<FeedItem>) {
        val diffCallback = FeedDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items = newItems
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
