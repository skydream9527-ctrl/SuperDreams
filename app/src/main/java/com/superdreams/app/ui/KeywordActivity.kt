package com.superdreams.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.superdreams.app.R
import com.superdreams.app.SuperDreamsApp
import com.superdreams.app.data.KeywordRepository

class KeywordActivity : AppCompatActivity() {

    private lateinit var keywordRepo: KeywordRepository
    private lateinit var adapter: KeywordListAdapter
    private lateinit var countText: TextView
    private lateinit var inputField: EditText
    private lateinit var btnCrawlNow: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyword)

        keywordRepo = KeywordRepository.getInstance(this)

        countText = findViewById(R.id.keyword_count)
        inputField = findViewById(R.id.keyword_input)
        btnCrawlNow = findViewById(R.id.btn_crawl_now)

        val recyclerView = findViewById<RecyclerView>(R.id.keyword_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = KeywordListAdapter(
            keywordRepo.getKeywords().toMutableList()
        ) { keyword ->
            keywordRepo.removeKeyword(keyword)
            refreshList()
        }
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btn_add_keyword).setOnClickListener {
            val keyword = inputField.text.toString().trim()
            if (keyword.isEmpty()) {
                Toast.makeText(this, "请输入关键词", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (keywordRepo.addKeyword(keyword)) {
                inputField.text.clear()
                refreshList()
            } else {
                val count = keywordRepo.getKeywordCount()
                if (count >= KeywordRepository.MAX_KEYWORDS) {
                    Toast.makeText(this, "最多只能添加 ${KeywordRepository.MAX_KEYWORDS} 个关键词", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "该关键词已存在", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnCrawlNow.setOnClickListener {
            if (!keywordRepo.hasEnoughKeywords()) {
                Toast.makeText(this, "请至少添加 ${KeywordRepository.MIN_KEYWORDS} 个关键词", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            (application as SuperDreamsApp).triggerImmediateCrawl()
            Toast.makeText(this, "正在后台抓取内容，请稍后查看小部件", Toast.LENGTH_LONG).show()
        }

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        refreshList()
    }

    private fun refreshList() {
        val keywords = keywordRepo.getKeywords()
        adapter.updateItems(keywords.toMutableList())
        countText.text = "${keywords.size}/${KeywordRepository.MAX_KEYWORDS} 个关键词"
        btnCrawlNow.isEnabled = keywordRepo.hasEnoughKeywords()
        btnCrawlNow.alpha = if (keywordRepo.hasEnoughKeywords()) 1.0f else 0.4f
    }

    // Simple inline adapter for keyword list
    class KeywordListAdapter(
        private var items: MutableList<String>,
        private val onRemove: (String) -> Unit
    ) : RecyclerView.Adapter<KeywordListAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text: TextView = view.findViewById(R.id.keyword_text)
            val removeBtn: ImageView = view.findViewById(R.id.keyword_remove)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.keyword_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val keyword = items[position]
            holder.text.text = keyword
            holder.removeBtn.setOnClickListener { onRemove(keyword) }
        }

        override fun getItemCount() = items.size

        fun updateItems(newItems: MutableList<String>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}
