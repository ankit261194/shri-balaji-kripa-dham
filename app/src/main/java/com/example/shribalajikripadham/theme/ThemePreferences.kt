package com.example.shribalajikripadham.theme

import android.content.Context
import android.content.SharedPreferences

object ThemePreferences {
    private const val PREFS_NAME = "shri_balaji_theme_prefs"
    private const val KEY_THEME_ID = "selected_theme_id"

    fun getSelectedTheme(context: Context): SacredTheme {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeId = prefs.getString(KEY_THEME_ID, SacredTheme.WHATSAPP_EMERALD.id) ?: SacredTheme.WHATSAPP_EMERALD.id
        return SacredTheme.fromId(themeId)
    }

    fun setSelectedTheme(context: Context, theme: SacredTheme) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_ID, theme.id).apply()
    }

    fun getDivineThemes(): List<SacredTheme> = listOf(
        SacredTheme.TIRANGA,
        SacredTheme.DIVYA_DEEPAWALI,
        SacredTheme.SHERAWALI_MAIYA,
        SacredTheme.VEER_HANUMAN
    )

    fun getPeacefulThemes(): List<SacredTheme> = listOf(
        SacredTheme.WHATSAPP_EMERALD,
        SacredTheme.TELEGRAM_BLUE,
        SacredTheme.CALM_SAGE_MINT,
        SacredTheme.OCEAN_INDIGO,
        SacredTheme.PEACEFUL_LAVENDER,
        SacredTheme.MINIMAL_SLATE,
        SacredTheme.SOOTHING_AMBER,
        SacredTheme.WHATSAPP_DARK
    )

    fun getAllThemes(): List<SacredTheme> = getDivineThemes() + getPeacefulThemes()
}
