package com.orionisli.ovl0ad3r.data

import com.orionisli.ovl0ad3r.data.locale.LanguageController
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LanguageControllerTest {

    @Test
    fun simplifiedChineseSystemLocaleResolvesToSimplified() {
        assertEquals(
            LanguageController.ZH_CN,
            LanguageController.resolve(Locale.SIMPLIFIED_CHINESE, null)
        )
    }

    @Test
    fun traditionalRegionsResolveToTraditional() {
        for (region in listOf("TW", "HK", "MO")) {
            assertEquals(
                "region $region must map to zh-TW",
                LanguageController.ZH_TW,
                LanguageController.resolve(Locale.forLanguageTag("zh-$region"), null)
            )
        }
    }

    @Test
    fun hantScriptWinsEvenOnAnUnusualRegion() {
        val locale = Locale.forLanguageTag("zh-Hant-CN")
        assertEquals("Hant", locale.script)
        assertEquals("CN", locale.country)
        assertEquals(LanguageController.ZH_TW, LanguageController.resolve(locale, null))
    }

    @Test
    fun nonChineseSystemLocaleResolvesToEnglish() {
        assertEquals(LanguageController.EN, LanguageController.resolve(Locale.US, null))
        assertEquals(LanguageController.EN, LanguageController.resolve(Locale.JAPAN, null))
    }

    @Test
    fun storedOverrideBeatsTheSystemLocale() {
        assertEquals(
            LanguageController.ZH_CN,
            LanguageController.resolve(Locale.US, LanguageController.ZH_CN)
        )
        assertEquals(
            LanguageController.EN,
            LanguageController.resolve(Locale.SIMPLIFIED_CHINESE, LanguageController.EN)
        )
    }

    @Test
    fun unsupportedOrBlankOverrideFallsBackToTheSystemLocale() {
        assertEquals(
            LanguageController.ZH_CN,
            LanguageController.resolve(Locale.SIMPLIFIED_CHINESE, "fr-FR")
        )
        assertEquals(
            LanguageController.ZH_CN,
            LanguageController.resolve(Locale.SIMPLIFIED_CHINESE, "")
        )
        assertEquals(
            LanguageController.ZH_CN,
            LanguageController.resolve(Locale.SIMPLIFIED_CHINESE, "zh")
        )
    }

    @Test
    fun everySupportedCodeRoundTripsThroughTagLookup() {
        for (code in LanguageController.supported) {
            val locale = Locale.forLanguageTag(code)
            assertEquals(code, LanguageController.resolve(locale, null))
        }
    }
}
