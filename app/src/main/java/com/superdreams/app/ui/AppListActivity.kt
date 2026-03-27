package com.superdreams.app.ui

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.superdreams.app.R
import com.superdreams.app.data.ThemePreference

class AppListActivity : AppCompatActivity() {

    private val apps = mutableListOf<LaunchableApp>()
    private lateinit var adapter: AppListAdapter
    private lateinit var countView: TextView
    private lateinit var inputView: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        countView = findViewById(R.id.app_list_count)
        inputView = findViewById(R.id.app_list_search_input)
        val recyclerView = findViewById<RecyclerView>(R.id.app_list_recycler)
        recyclerView.layoutManager = GridLayoutManager(this, 5)
        adapter = AppListAdapter(mutableListOf()) { openApp(it) }
        recyclerView.adapter = adapter

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        inputView.doAfterTextChanged { editable ->
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

        apps.clear()
        apps.addAll(loadLaunchableApps())
        adapter.updateItems(apps)
        updateCount(apps.size)
        applyTheme()
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
    }

    private fun updateCount(size: Int) {
        countView.text = "共 $size 个应用"
    }

    private fun loadLaunchableApps(): List<LaunchableApp> {
        val pm = packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launcherInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                launchIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launchIntent, PackageManager.MATCH_ALL)
        }

        val fromLauncher = launcherInfos.map { info ->
            LaunchableApp(
                label = info.loadLabel(pm).toString(),
                packageName = info.activityInfo.packageName,
                activityName = info.activityInfo.name,
                icon = info.loadIcon(pm)
            )
        }

        val fromInstalled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(PackageManager.MATCH_ALL)
        }.mapNotNull { appInfo ->
            val packageName = appInfo.packageName
            val launch = pm.getLaunchIntentForPackage(packageName) ?: return@mapNotNull null
            LaunchableApp(
                label = appInfo.loadLabel(pm).toString(),
                packageName = packageName,
                activityName = launch.component?.className,
                icon = appInfo.loadIcon(pm)
            )
        }

        return (fromLauncher + fromInstalled)
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun openApp(app: LaunchableApp) {
        if (!app.activityName.isNullOrEmpty()) {
            val explicitIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            runCatching {
                startActivity(explicitIntent)
                return
            }
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "无法打开该应用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyTheme() {
        val palette = ThemePreference.getPalette(this)
        findViewById<View>(R.id.app_list_root).setBackgroundColor(palette.rootColor)
        findViewById<View>(R.id.app_list_header).background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(palette.headerStartColor, palette.headerEndColor)
            cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 24f, 24f, 24f, 24f)
        }
        findViewById<TextView>(R.id.app_list_title).setTextColor(palette.titleTextColor)
        findViewById<TextView>(R.id.app_list_subtitle).setTextColor(palette.subtitleTextColor)
        countView.setTextColor(palette.hintTextColor)
        inputView.background = createRoundedBackground(
            palette.searchFillColor,
            palette.searchStrokeColor,
            14f
        )
        inputView.setTextColor(palette.titleTextColor)
        inputView.setHintTextColor(palette.subtitleTextColor)
    }

    private fun createRoundedBackground(fillColor: Int, strokeColor: Int, radiusDp: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp * resources.displayMetrics.density
            setColor(fillColor)
            setStroke((1f * resources.displayMetrics.density).toInt().coerceAtLeast(1), strokeColor)
        }
    }
}
