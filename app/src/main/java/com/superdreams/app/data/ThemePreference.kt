package com.superdreams.app.data

import android.content.Context
import android.graphics.Color

enum class AppTheme {
    LAVENDER, MINT, PEACH
}

data class ThemePalette(
    val rootColor: Int,
    val headerStartColor: Int,
    val headerEndColor: Int,
    val primaryFillColor: Int,
    val primaryStrokeColor: Int,
    val secondaryFillColor: Int,
    val secondaryStrokeColor: Int,
    val searchFillColor: Int,
    val searchStrokeColor: Int,
    val titleTextColor: Int,
    val subtitleTextColor: Int,
    val hintTextColor: Int
)

object ThemePreference {
    private const val PREF_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"

    fun getTheme(context: Context): AppTheme {
        val value = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, AppTheme.LAVENDER.name)
        return try {
            AppTheme.valueOf(value ?: AppTheme.LAVENDER.name)
        } catch (e: Exception) {
            AppTheme.LAVENDER
        }
    }

    fun setTheme(context: Context, theme: AppTheme) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme.name)
            .apply()
    }

    fun getPalette(context: Context): ThemePalette {
        return when (getTheme(context)) {
            AppTheme.MINT -> ThemePalette(
                rootColor = Color.parseColor("#F4FBF8"),
                headerStartColor = Color.parseColor("#F9FFFC"),
                headerEndColor = Color.parseColor("#E9F8F0"),
                primaryFillColor = Color.parseColor("#DCF5E7"),
                primaryStrokeColor = Color.parseColor("#BEEAD2"),
                secondaryFillColor = Color.parseColor("#EAF8F1"),
                secondaryStrokeColor = Color.parseColor("#D4F0E2"),
                searchFillColor = Color.parseColor("#ECF8F3"),
                searchStrokeColor = Color.parseColor("#D5EFDF"),
                titleTextColor = Color.parseColor("#244235"),
                subtitleTextColor = Color.parseColor("#5F8072"),
                hintTextColor = Color.parseColor("#7A978A")
            )
            AppTheme.PEACH -> ThemePalette(
                rootColor = Color.parseColor("#FFF8F4"),
                headerStartColor = Color.parseColor("#FFFCFB"),
                headerEndColor = Color.parseColor("#FFEDE4"),
                primaryFillColor = Color.parseColor("#FFE7DA"),
                primaryStrokeColor = Color.parseColor("#FFD4BF"),
                secondaryFillColor = Color.parseColor("#FFF0E8"),
                secondaryStrokeColor = Color.parseColor("#FFE1D0"),
                searchFillColor = Color.parseColor("#FFF2EA"),
                searchStrokeColor = Color.parseColor("#FFE2D4"),
                titleTextColor = Color.parseColor("#4A3128"),
                subtitleTextColor = Color.parseColor("#866555"),
                hintTextColor = Color.parseColor("#9A7A69")
            )
            AppTheme.LAVENDER -> ThemePalette(
                rootColor = Color.parseColor("#F6F7FB"),
                headerStartColor = Color.parseColor("#FDFDFF"),
                headerEndColor = Color.parseColor("#EEF1FF"),
                primaryFillColor = Color.parseColor("#E8EEFF"),
                primaryStrokeColor = Color.parseColor("#D0DAFF"),
                secondaryFillColor = Color.parseColor("#F2F5FF"),
                secondaryStrokeColor = Color.parseColor("#E1E7FF"),
                searchFillColor = Color.parseColor("#F1F4FF"),
                searchStrokeColor = Color.parseColor("#DEE5FF"),
                titleTextColor = Color.parseColor("#2B2B3A"),
                subtitleTextColor = Color.parseColor("#6F7285"),
                hintTextColor = Color.parseColor("#8B90A7")
            )
        }
    }
}
