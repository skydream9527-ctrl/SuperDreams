package com.superdreams.app.data

data class FeedItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: FeedType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class FeedType {
    NEWS, TODO
}
