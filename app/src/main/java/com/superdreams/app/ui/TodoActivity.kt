package com.superdreams.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.superdreams.app.R
import com.superdreams.app.data.FeedItem
import com.superdreams.app.data.TodoRepository

class TodoActivity : AppCompatActivity() {

    private lateinit var todoRepo: TodoRepository
    private lateinit var adapter: TodoListAdapter
    private lateinit var countText: TextView
    private lateinit var inputTitle: EditText
    private lateinit var inputSubtitle: EditText
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo)

        todoRepo = TodoRepository.getInstance(this)

        countText = findViewById(R.id.todo_count)
        inputTitle = findViewById(R.id.todo_input_title)
        inputSubtitle = findViewById(R.id.todo_input_subtitle)

        // Recycler view with drag-to-reorder
        val recyclerView = findViewById<RecyclerView>(R.id.todo_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TodoListAdapter(
            todoRepo.getTodos().toMutableList(),
            onRemove = { todo ->
                todoRepo.removeTodo(todo.id)
                refreshList()
            },
            onStartDrag = { viewHolder ->
                itemTouchHelper?.startDrag(viewHolder)
            }
        )
        recyclerView.adapter = adapter

        // Drag & drop support
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                adapter.moveItem(from, to)
                todoRepo.moveTodo(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled() = false
        })
        touchHelper.attachToRecyclerView(recyclerView)
        itemTouchHelper = touchHelper

        // Add button
        findViewById<Button>(R.id.btn_add_todo).setOnClickListener {
            val title = inputTitle.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(this, "请输入待办内容", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val subtitle = inputSubtitle.text.toString().trim()
            todoRepo.addTodo(title, subtitle)
            inputTitle.text.clear()
            inputSubtitle.text.clear()
            refreshList()
        }

        // Interleave mode toggle
        val modeGroup = findViewById<RadioGroup>(R.id.mode_radio_group)
        val currentMode = todoRepo.getInterleaveMode()
        modeGroup.check(
            if (currentMode == TodoRepository.InterleaveMode.SEQUENTIAL)
                R.id.radio_sequential else R.id.radio_random
        )
        modeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = if (checkedId == R.id.radio_sequential)
                TodoRepository.InterleaveMode.SEQUENTIAL
            else TodoRepository.InterleaveMode.RANDOM
            todoRepo.setInterleaveMode(mode)
        }

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        refreshList()
    }

    private fun refreshList() {
        val todos = todoRepo.getTodos()
        adapter.updateItems(todos.toMutableList())
        countText.text = "${todos.size} 条待办"
    }

    // Adapter with drag handle
    class TodoListAdapter(
        private var items: MutableList<FeedItem>,
        private val onRemove: (FeedItem) -> Unit,
        private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
    ) : RecyclerView.Adapter<TodoListAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.todo_item_title)
            val subtitle: TextView = view.findViewById(R.id.todo_item_subtitle)
            val removeBtn: ImageView = view.findViewById(R.id.todo_item_remove)
            val dragHandle: ImageView = view.findViewById(R.id.todo_item_drag)
            val indexText: TextView = view.findViewById(R.id.todo_item_index)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.todo_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = item.title
            holder.subtitle.text = item.subtitle
            holder.indexText.text = "${position + 1}"
            holder.removeBtn.setOnClickListener { onRemove(item) }
            holder.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag(holder)
                }
                false
            }
        }

        override fun getItemCount() = items.size

        fun moveItem(from: Int, to: Int) {
            val item = items.removeAt(from)
            items.add(to, item)
            notifyItemMoved(from, to)
            // Update index numbers
            notifyItemRangeChanged(minOf(from, to), Math.abs(from - to) + 1)
        }

        fun updateItems(newItems: MutableList<FeedItem>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}
