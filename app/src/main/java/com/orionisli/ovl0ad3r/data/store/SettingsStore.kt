package com.orionisli.ovl0ad3r.data.store

import android.content.Context
import android.content.SharedPreferences
import com.orionisli.ovl0ad3r.HapticUtil
import com.orionisli.ovl0ad3r.ui.theme.OvlThemeType
import com.orionisli.ovl0ad3r.ui.theme.TerminalThemeType

class SettingsStore private constructor(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("ovl0ad3r_settings", Context.MODE_PRIVATE)

    var uiThemeName: String
        get() = sp.getString(KEY_UI_THEME, "sakura") ?: "sakura"
        set(value) = sp.edit().putString(KEY_UI_THEME, value).apply()

    var uiTheme: OvlThemeType
        get() = when (uiThemeName.lowercase()) {
            "beige" -> OvlThemeType.Beige
            "slate" -> OvlThemeType.Slate
            "cyber" -> OvlThemeType.Cyber
            "matcha" -> OvlThemeType.Matcha
            "nord", "aurora" -> OvlThemeType.Nord
            else -> OvlThemeType.Sakura
        }
        set(value) {
            uiThemeName = when (value) {
                OvlThemeType.Beige -> "beige"
                OvlThemeType.Slate -> "slate"
                OvlThemeType.Cyber -> "cyber"
                OvlThemeType.Matcha -> "matcha"
                OvlThemeType.Nord -> "nord"
                OvlThemeType.Sakura -> "sakura"
            }
        }

    var terminalThemeName: String
        get() = sp.getString(KEY_TERMINAL_THEME, "parchment") ?: "parchment"
        set(value) = sp.edit().putString(KEY_TERMINAL_THEME, value).apply()

    var terminalTheme: TerminalThemeType
        get() = when (terminalThemeName.lowercase()) {
            "matrix" -> TerminalThemeType.Matrix
            "solarized" -> TerminalThemeType.Solarized
            "monokai" -> TerminalThemeType.Monokai
            "custom" -> TerminalThemeType.Custom
            else -> TerminalThemeType.Parchment
        }
        set(value) {
            terminalThemeName = when (value) {
                TerminalThemeType.Matrix -> "matrix"
                TerminalThemeType.Solarized -> "solarized"
                TerminalThemeType.Monokai -> "monokai"
                TerminalThemeType.Custom -> "custom"
                TerminalThemeType.Parchment -> "parchment"
            }
        }

    var hapticStrength: Int
        get() = sp.getInt(KEY_HAPTIC_STRENGTH, 2)
        set(value) {
            val clamped = value.coerceIn(0, 3)
            sp.edit().putInt(KEY_HAPTIC_STRENGTH, clamped).apply()
            HapticUtil.strength = clamped
        }

    var terminalBgCustom: String
        get() = sp.getString(KEY_TERMINAL_BG_CUSTOM, "#1E1E2E") ?: "#1E1E2E"
        set(value) = sp.edit().putString(KEY_TERMINAL_BG_CUSTOM, value).apply()

    var terminalFgCustom: String
        get() = sp.getString(KEY_TERMINAL_FG_CUSTOM, "#CDD6F4") ?: "#CDD6F4"
        set(value) = sp.edit().putString(KEY_TERMINAL_FG_CUSTOM, value).apply()

    init {
        HapticUtil.strength = hapticStrength
    }

    companion object {
        private const val KEY_UI_THEME = "ui_theme"
        private const val KEY_TERMINAL_THEME = "terminal_theme"
        private const val KEY_HAPTIC_STRENGTH = "haptic_strength"
        private const val KEY_TERMINAL_BG_CUSTOM = "terminal_bg_custom"
        private const val KEY_TERMINAL_FG_CUSTOM = "terminal_fg_custom"

        @Volatile
        private var instance: SettingsStore? = null

        fun getInstance(context: Context): SettingsStore {
            return instance ?: synchronized(this) {
                instance ?: SettingsStore(context).also { instance = it }
            }
        }
    }
}
