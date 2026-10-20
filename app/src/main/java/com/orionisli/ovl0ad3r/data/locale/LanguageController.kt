package com.orionisli.ovl0ad3r.data.locale

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LanguageController {

    const val ZH_CN = "zh-CN"
    const val ZH_TW = "zh-TW"
    const val EN = "en"

    val supported: List<String> = listOf(ZH_CN, ZH_TW, EN)

    private const val PREFS_NAME = "ovl0ad3r_prefs"
    private const val KEY_UI_LANGUAGE = "ui_language"

    val autoRestartsOnApply: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun resolve(systemLocale: Locale, override: String?): String {
        if (override != null && override in supported) return override
        return when (systemLocale.language) {
            Locale.CHINESE.language ->
                if (isTraditional(systemLocale)) ZH_TW else ZH_CN
            else -> EN
        }
    }

    private fun isTraditional(locale: Locale): Boolean =
        locale.script == "Hant" ||
            locale.country.uppercase() in setOf("TW", "HK", "MO")

    fun stored(context: Context): String? =
        prefs(context).getString(KEY_UI_LANGUAGE, null)?.takeIf { it in supported }

    fun systemLocale(context: Context): Locale =
        context.resources.configuration.locales
            .takeIf { !it.isEmpty }
            ?.get(0)
            ?: Locale.getDefault()

    fun current(context: Context): String {
        if (autoRestartsOnApply) {
            val appLocales = context.getSystemService(LocaleManager::class.java).applicationLocales
            val applied = appLocales.takeIf { !it.isEmpty }?.get(0)
            if (applied != null) {
                val resolved = resolve(applied, null)
                if (applied.toLanguageTag() == tagOf(resolved)) return resolved
            }
        }
        return resolve(systemLocale(context), stored(context))
    }

    fun apply(context: Context, code: String) {
        if (code !in supported) return
        if (autoRestartsOnApply) {
            context.getSystemService(LocaleManager::class.java)
                .setApplicationLocales(LocaleList.forLanguageTags(code))
        } else {
            prefs(context).edit().putString(KEY_UI_LANGUAGE, code).apply()
        }
    }

    fun wrap(base: Context): Context {
        val override = stored(base) ?: return base
        val locale = Locale.forLanguageTag(override)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }

    fun applyDefault(context: Context) {
        Locale.setDefault(Locale.forLanguageTag(current(context)))
    }

    private fun tagOf(code: String): String =
        if (code == EN) "en" else code

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
