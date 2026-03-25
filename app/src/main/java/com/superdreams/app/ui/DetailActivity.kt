package com.superdreams.app.ui

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.superdreams.app.R

class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CONTENT = "extra_content"
        const val EXTRA_SOURCE = "extra_source"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_KEYWORD = "extra_keyword"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_COLOR = "extra_color"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: ""
        val source = intent.getStringExtra(EXTRA_SOURCE) ?: ""
        val url = intent.getStringExtra(EXTRA_URL) ?: ""
        val keyword = intent.getStringExtra(EXTRA_KEYWORD) ?: ""
        val type = intent.getStringExtra(EXTRA_TYPE) ?: ""
        val color = intent.getIntExtra(EXTRA_COLOR, 0xFFF5F5F5.toInt())

        // Apply pastel background to card area
        val cardArea = findViewById<View>(R.id.detail_card_area)
        val bg = GradientDrawable().apply {
            setColor(color)
            cornerRadius = 16f * resources.displayMetrics.density
        }
        cardArea.background = bg

        // Set type icon
        val typeIcon = findViewById<ImageView>(R.id.detail_type_icon)
        val typeLabel = findViewById<TextView>(R.id.detail_type_label)
        if (type == "TODO") {
            typeIcon.setImageResource(R.drawable.ic_todo)
            typeLabel.text = "待办事项"
        } else {
            typeIcon.setImageResource(R.drawable.ic_news)
            typeLabel.text = if (type == "CRAWLED") "抓取资讯" else "新闻资讯"
        }

        // Set content
        findViewById<TextView>(R.id.detail_title).text = title
        findViewById<TextView>(R.id.detail_content).text = content

        // Source & keyword info
        val infoText = buildString {
            if (source.isNotEmpty()) append("来源: $source")
            if (keyword.isNotEmpty()) {
                if (isNotEmpty()) append("\n")
                append("关键词: $keyword")
            }
        }
        val infoView = findViewById<TextView>(R.id.detail_info)
        if (infoText.isNotEmpty()) {
            infoView.text = infoText
            infoView.visibility = View.VISIBLE
        } else {
            infoView.visibility = View.GONE
        }

        // Open in browser button — show for any non-empty URL
        val btnOpen = findViewById<Button>(R.id.btn_open_browser)
        if (url.isNotEmpty()) {
            btnOpen.visibility = View.VISIBLE
            btnOpen.setOnClickListener {
                val finalUrl = if (url.startsWith("http")) url else "https://news.baidu.com$url"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)))
            }
        } else {
            btnOpen.visibility = View.GONE
        }

        // Back button
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
    }
}
