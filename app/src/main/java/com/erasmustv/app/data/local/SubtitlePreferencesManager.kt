package com.erasmustv.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class SubtitleFont(val displayName: String) {
    SANS_SERIF("Modern Sans"),
    SERIF("Cinematic Serif"),
    MONOSPACE("Monospace"),
    CASUAL("Casual Rounded"),
    BOSTONE("Erasmus Bostone")
}

enum class SubtitleSize(val displayName: String, val spSize: Float) {
    SMALL("Small", 20f),
    MEDIUM("Medium", 26f),
    LARGE("Large", 32f)
}

private val Context.subtitleDataStore by preferencesDataStore(name = "erasmus_subtitle_prefs")

class SubtitlePreferencesManager(private val context: Context) {

    private object Keys {
        val FONT = stringPreferencesKey("subtitle_font")
        val SIZE = stringPreferencesKey("subtitle_size")
    }

    val fontFlow: Flow<SubtitleFont> = context.subtitleDataStore.data.map { prefs ->
        val raw = prefs[Keys.FONT]
        raw?.let {
            runCatching { SubtitleFont.valueOf(it) }.getOrDefault(SubtitleFont.SANS_SERIF)
        } ?: SubtitleFont.SANS_SERIF
    }

    val sizeFlow: Flow<SubtitleSize> = context.subtitleDataStore.data.map { prefs ->
        val raw = prefs[Keys.SIZE]
        raw?.let {
            runCatching { SubtitleSize.valueOf(it) }.getOrDefault(SubtitleSize.MEDIUM)
        } ?: SubtitleSize.MEDIUM
    }

    suspend fun getFont(): SubtitleFont {
        val raw = context.subtitleDataStore.data.first()[Keys.FONT]
        return raw?.let {
            runCatching { SubtitleFont.valueOf(it) }.getOrDefault(SubtitleFont.SANS_SERIF)
        } ?: SubtitleFont.SANS_SERIF
    }

    suspend fun getSize(): SubtitleSize {
        val raw = context.subtitleDataStore.data.first()[Keys.SIZE]
        return raw?.let {
            runCatching { SubtitleSize.valueOf(it) }.getOrDefault(SubtitleSize.MEDIUM)
        } ?: SubtitleSize.MEDIUM
    }

    suspend fun setFont(font: SubtitleFont) {
        context.subtitleDataStore.edit { prefs ->
            prefs[Keys.FONT] = font.name
        }
    }

    suspend fun setSize(size: SubtitleSize) {
        context.subtitleDataStore.edit { prefs ->
            prefs[Keys.SIZE] = size.name
        }
    }
}
