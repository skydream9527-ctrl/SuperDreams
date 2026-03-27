package com.superdreams.app.data

data class FeedItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val content: String = "",
    val type: FeedType,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "",
    val url: String = "",
    val keyword: String = ""
)

enum class FeedType {
    NEWS, TODO, CRAWLED, NOTIFICATION
}
