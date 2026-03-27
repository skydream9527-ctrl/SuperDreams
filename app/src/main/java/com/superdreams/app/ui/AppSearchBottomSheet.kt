package com.superdreams.app.ui

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.superdreams.app.R

class AppSearchBottomSheet : BottomSheetDialogFragment() {

    private val apps = mutableListOf<LaunchableApp>()
    private lateinit var adapter: AppListAdapter
    private lateinit var countView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_app_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val input = view.findViewById<EditText>(R.id.app_search_input)
        val list = view.findViewById<RecyclerView>(R.id.app_search_list)
        countView = view.findViewById(R.id.app_search_count)

        adapter = AppListAdapter(mutableListOf()) { app ->
            openApp(app)
        }

        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        apps.clear()
        apps.addAll(loadLaunchableApps())
        adapter.updateItems(apps)
        updateCount(adapter.itemCount)

        input.doAfterTextChanged { editable ->
            val keyword = editable?.toString()?.trim().orEmpty()
            val filtered = if (keyword.isEmpty()) {
                apps
            } else {
                apps.filter {
                    it.label.contains(keyword, ignoreCase = true) ||
                        it.packageName.contains(keyword, ignoreCase = true)
                }
            }
            adapter.updateItems(filtered)
            updateCount(filtered.size)
        }
    }

    private fun updateCount(size: Int) {
        countView.text = "共 $size 个应用"
    }

    private fun loadLaunchableApps(): List<LaunchableApp> {
        val pm = requireContext().packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launchIntent, android.content.pm.PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launchIntent, 0)
        }
        return infos
            .map { info -> info.toLaunchableApp(pm) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun ResolveInfo.toLaunchableApp(pm: android.content.pm.PackageManager): LaunchableApp {
        val activityInfo = this.activityInfo
        return LaunchableApp(
            label = loadLabel(pm).toString(),
            packageName = activityInfo.packageName,
            activityName = activityInfo.name,
            icon = loadIcon(pm)
        )
    }

    private fun openApp(app: LaunchableApp) {
        val context = requireContext()
        val explicitIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching {
            startActivity(explicitIntent)
            dismiss()
        }.onFailure {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                dismiss()
            } else {
                Toast.makeText(context, "无法打开该应用", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable
)

class AppListAdapter(
    private var items: MutableList<LaunchableApp>,
    private val onClick: (LaunchableApp) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.item_app_icon)
        val name: TextView = view.findViewById(R.id.item_app_name)
        val packageName: TextView = view.findViewById(R.id.item_app_package)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageDrawable(item.icon)
        holder.name.text = item.label
        holder.packageName.text = item.packageName
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<LaunchableApp>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }
}
