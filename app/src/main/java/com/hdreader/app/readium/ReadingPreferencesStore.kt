package com.hdreader.app.readium

import android.content.Context
import android.content.SharedPreferences
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Global reading prefs for stage A. Dual column default ON in landscape is applied at runtime.
 */
@OptIn(ExperimentalReadiumApi::class)
class ReadingPreferencesStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hdreader_reading", Context.MODE_PRIVATE)

    var dualColumnEnabled: Boolean
        get() = prefs.getBoolean(KEY_DUAL, true)
        set(value) = prefs.edit().putBoolean(KEY_DUAL, value).apply()

    var volumeKeysEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOLUME, true)
        set(value) = prefs.edit().putBoolean(KEY_VOLUME, value).apply()

    var fontSize: Double
        get() = prefs.getFloat(KEY_FONT, 1.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_FONT, value.toFloat()).apply()

    var night: Boolean
        get() = prefs.getBoolean(KEY_NIGHT, false)
        set(value) = prefs.edit().putBoolean(KEY_NIGHT, value).apply()

    fun buildEpubPreferences(isLandscape: Boolean): EpubPreferences {
        val columns = when {
            !dualColumnEnabled -> ColumnCount.ONE
            isLandscape -> ColumnCount.TWO
            else -> ColumnCount.ONE
        }
        return EpubPreferences(
            scroll = false,
            columnCount = columns,
            fontSize = fontSize,
            theme = if (night) Theme.DARK else Theme.LIGHT,
            publisherStyles = false
        )
    }

    companion object {
        private const val KEY_DUAL = "dual_column"
        private const val KEY_VOLUME = "volume_keys"
        private const val KEY_FONT = "font_size"
        private const val KEY_NIGHT = "night"
    }
}
