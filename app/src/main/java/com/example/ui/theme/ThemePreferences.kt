package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)

    private val _currentTheme = MutableStateFlow(getSavedTheme())
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private fun getSavedTheme(): AppTheme {
        val themeId = prefs.getString("selected_theme", AppTheme.SOPHISTICATED_DARK.id)
        return AppTheme.entries.find { it.id == themeId } ?: AppTheme.SOPHISTICATED_DARK
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("selected_theme", theme.id).apply()
        _currentTheme.value = theme
    }
}
