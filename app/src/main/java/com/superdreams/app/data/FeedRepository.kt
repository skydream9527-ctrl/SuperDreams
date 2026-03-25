package com.superdreams.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class FeedRepository(context: Context) {

    private val prefs = context.getSharedPreferences("feed_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_ITEMS = "feed_items"
        private const val MIN_ITEMS = 8

        private val NEWS_TITLES = listOf(
            "科技巨头发布全新AI助手，引领智能生活新潮流",
            "全球气候峰会达成新协议，各国承诺减排目标",
            "新能源汽车销量创历史新高，市场格局加速变革",
            "航天领域重大突破！深空探测器成功着陆",
            "数字货币监管新政出台，行业迎来规范发展",
            "5G网络覆盖率突破90%，万物互联时代到来",
            "生物医药领域重大发现，新型疗法开启临床试验",
            "智慧城市建设加速推进，城市治理迈入新阶段",
            "元宇宙概念持续升温，多家企业布局虚拟现实",
            "量子计算取得关键突破，计算能力实现飞跃",
            "太空旅游商业化加速，首批民间航天员出发",
            "可持续发展报告发布，绿色经济增长强劲",
            "人工智能教育革命，个性化学习成为趋势",
            "海洋探索新发现，深海生态系统研究进展",
            "全球芯片产能扩张，半导体行业迎来新周期"
        )

        private val TODO_TITLES = listOf(
            "完成本周项目进度报告",
            "预约下周三的团队会议室",
            "回复客户的产品需求邮件",
            "更新个人简历和作品集",
            "整理办公桌和文件资料",
            "准备明天的产品演示 PPT",
            "检查并更新系统安全补丁",
            "提交月度费用报销申请",
            "安排下周的客户拜访行程",
            "复习下周技术面试的知识点",
            "完成在线培训课程第三章",
            "备份重要项目数据到云端",
            "制定下个季度的OKR目标",
            "预订下月出差的机票酒店",
            "整理读书笔记并写总结"
        )

        private val NEWS_SUBTITLES = listOf(
            "业内人士预计将改变消费者使用习惯",
            "这将影响未来十年的全球经济格局",
            "专家分析市场前景持续看好",
            "标志着人类太空探索迈入新纪元",
            "行业从业者关注政策细节落地"
        )

        private val TODO_SUBTITLES = listOf(
            "截止日期：本周五",
            "优先级：高",
            "需要30分钟完成",
            "已延期，请尽快处理",
            "与团队协作完成"
        )

        @Volatile
        private var instance: FeedRepository? = null

        fun getInstance(context: Context): FeedRepository {
            return instance ?: synchronized(this) {
                instance ?: FeedRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getItems(): List<FeedItem> {
        val json = prefs.getString(KEY_ITEMS, null)
        val items = if (json != null) {
            val type = object : TypeToken<List<FeedItem>>() {}.type
            gson.fromJson<List<FeedItem>>(json, type)
        } else {
            emptyList()
        }
        return if (items.size < MIN_ITEMS) {
            val newItems = items.toMutableList()
            while (newItems.size < MIN_ITEMS) {
                newItems.add(generateRandomItem())
            }
            saveItems(newItems)
            newItems
        } else {
            items
        }
    }

    /**
     * Get feed items with user todos and captured notifications interleaved.
     * Notifications are the most time-sensitive so they appear at top, sorted by timestamp.
     */
    fun getItemsWithTodos(context: android.content.Context): List<FeedItem> {
        val contentItems = getItems().filter { it.type != FeedType.TODO }
        val todoRepo = TodoRepository.getInstance(context)
        val withTodos = todoRepo.interleaveIntoFeed(contentItems)
        // Prepend newest notifications
        val notifRepo = com.superdreams.app.data.NotificationRepository.getInstance(context)
        val notifications = notifRepo.getNotifications()
        return notifications + withTodos
    }

    fun removeItem(itemId: String) {
        // Read raw stored list (without auto-padding) so we remove exactly the target item
        val json = prefs.getString(KEY_ITEMS, null)
        val items: MutableList<FeedItem> = if (json != null) {
            val type = object : TypeToken<List<FeedItem>>() {}.type
            gson.fromJson<List<FeedItem>>(json, type).toMutableList()
        } else {
            mutableListOf()
        }
        items.removeAll { it.id == itemId }
        while (items.size < MIN_ITEMS) {
            items.add(generateRandomItem())
        }
        saveItems(items)
    }

    fun addItem(item: FeedItem) {
        val items = getItems().toMutableList()
        items.add(0, item)
        saveItems(items)
    }

    /**
     * Replace feed with crawled items. Keeps existing TODO items,
     * removes old crawled/news items, and adds new crawled content.
     */
    fun replaceCrawledItems(crawledItems: List<FeedItem>) {
        // TODOs are stored separately in TodoRepository; nothing to preserve here.
        // Just replace all content items with newly crawled ones.
        saveItems(crawledItems)
    }

    private fun saveItems(items: List<FeedItem>) {
        prefs.edit().putString(KEY_ITEMS, gson.toJson(items)).apply()
    }

    private fun generateRandomItem(): FeedItem {
        return FeedItem(
            id = UUID.randomUUID().toString(),
            title = NEWS_TITLES.random(),
            subtitle = NEWS_SUBTITLES.random(),
            type = FeedType.NEWS
        )
    }
}
