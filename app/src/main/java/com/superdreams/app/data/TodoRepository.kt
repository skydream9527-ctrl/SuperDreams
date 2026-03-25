package com.superdreams.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * Manages user-created todo items with ordering support.
 * Todos can be interleaved into the feed in user-specified or random order.
 */
class TodoRepository(context: Context) {

    private val prefs = context.getSharedPreferences("todo_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    enum class InterleaveMode {
        SEQUENTIAL,  // Todos appear at fixed intervals in user-defined order
        RANDOM       // Todos are randomly shuffled into the feed
    }

    companion object {
        private const val KEY_TODOS = "todos"
        private const val KEY_MODE = "interleave_mode"

        @Volatile
        private var instance: TodoRepository? = null

        fun getInstance(context: Context): TodoRepository {
            return instance ?: synchronized(this) {
                instance ?: TodoRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getTodos(): List<FeedItem> {
        val json = prefs.getString(KEY_TODOS, null) ?: return emptyList()
        val type = object : TypeToken<List<FeedItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addTodo(title: String, subtitle: String = ""): FeedItem {
        val todo = FeedItem(
            id = UUID.randomUUID().toString(),
            title = title,
            subtitle = subtitle.ifEmpty { "待办事项" },
            type = FeedType.TODO,
            source = "用户待办"
        )
        val todos = getTodos().toMutableList()
        todos.add(todo)
        saveTodos(todos)
        return todo
    }

    fun removeTodo(todoId: String) {
        val todos = getTodos().toMutableList()
        todos.removeAll { it.id == todoId }
        saveTodos(todos)
    }

    fun moveTodo(fromIndex: Int, toIndex: Int) {
        val todos = getTodos().toMutableList()
        if (fromIndex in todos.indices && toIndex in todos.indices) {
            val item = todos.removeAt(fromIndex)
            todos.add(toIndex, item)
            saveTodos(todos)
        }
    }

    fun getInterleaveMode(): InterleaveMode {
        val mode = prefs.getString(KEY_MODE, InterleaveMode.SEQUENTIAL.name)
        return try {
            InterleaveMode.valueOf(mode ?: InterleaveMode.SEQUENTIAL.name)
        } catch (e: Exception) {
            InterleaveMode.SEQUENTIAL
        }
    }

    fun setInterleaveMode(mode: InterleaveMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    /**
     * Merge todos into a content feed based on interleave mode.
     */
    fun interleaveIntoFeed(contentItems: List<FeedItem>): List<FeedItem> {
        val todos = getTodos()
        if (todos.isEmpty()) return contentItems
        if (contentItems.isEmpty()) return todos

        return when (getInterleaveMode()) {
            InterleaveMode.SEQUENTIAL -> interleaveSequential(contentItems, todos)
            InterleaveMode.RANDOM -> interleaveRandom(contentItems, todos)
        }
    }

    /**
     * Insert todos at regular intervals in user-defined order.
     * E.g. with 3 content and 2 todos: C, T, C, T, C
     */
    private fun interleaveSequential(content: List<FeedItem>, todos: List<FeedItem>): List<FeedItem> {
        val result = mutableListOf<FeedItem>()
        val interval = if (todos.size > 0) {
            maxOf(1, content.size / (todos.size + 1))
        } else 0

        var todoIndex = 0
        for (i in content.indices) {
            result.add(content[i])
            if (interval > 0 && (i + 1) % interval == 0 && todoIndex < todos.size) {
                result.add(todos[todoIndex])
                todoIndex++
            }
        }
        // Append any remaining todos
        while (todoIndex < todos.size) {
            result.add(todos[todoIndex])
            todoIndex++
        }
        return result
    }

    /**
     * Randomly interleave todos into the content feed.
     */
    private fun interleaveRandom(content: List<FeedItem>, todos: List<FeedItem>): List<FeedItem> {
        val result = mutableListOf<FeedItem>()
        result.addAll(content)
        val shuffledTodos = todos.shuffled()
        for (todo in shuffledTodos) {
            val pos = (0..result.size).random()
            result.add(pos, todo)
        }
        return result
    }

    private fun saveTodos(todos: List<FeedItem>) {
        prefs.edit().putString(KEY_TODOS, gson.toJson(todos)).apply()
    }
}
