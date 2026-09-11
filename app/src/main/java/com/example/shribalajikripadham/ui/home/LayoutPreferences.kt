package com.example.shribalajikripadham.ui.home

import android.content.Context
import android.content.SharedPreferences

object LayoutPreferences {
    private const val PREFS_NAME = "sbkd_layout_preferences"
    private const val KEY_ACTIVE_LAYOUT = "active_ui_layout_id"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSavedLayout(context: Context, defaultLayout: AppUiLayout = AppUiLayout.CLASSIC_DARBAR): AppUiLayout {
        val savedId = getPrefs(context).getString(KEY_ACTIVE_LAYOUT, defaultLayout.id) ?: defaultLayout.id
        return AppUiLayout.fromId(savedId)
    }

    fun saveLayout(context: Context, layout: AppUiLayout) {
        getPrefs(context).edit().putString(KEY_ACTIVE_LAYOUT, layout.id).apply()
    }
}
