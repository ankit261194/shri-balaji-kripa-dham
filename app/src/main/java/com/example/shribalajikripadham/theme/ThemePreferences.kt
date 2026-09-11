package com.example.shribalajikripadham.theme

import android.content.Context
import android.content.SharedPreferences

object ThemePreferences {
    private const val PREFS_NAME = "shri_balaji_theme_prefs"
    private const val KEY_THEME_ID = "selected_theme_id"

    fun getSelectedTheme(context: Context): SacredTheme {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeId = prefs.getString(KEY_THEME_ID, SacredTheme.ROYAL_MAROON.id) ?: SacredTheme.ROYAL_MAROON.id
        return SacredTheme.fromId(themeId)
    }

    fun setSelectedTheme(context: Context, theme: SacredTheme) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_ID, theme.id).apply()
    }
}
