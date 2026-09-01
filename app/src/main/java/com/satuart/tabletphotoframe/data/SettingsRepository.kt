package com.satuart.tabletphotoframe.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var photoFolderUri: Uri?
        get() = prefs.getString(KEY_FOLDER_URI, null)?.let(Uri::parse)
        set(value) = prefs.edit { putString(KEY_FOLDER_URI, value?.toString()) }

    var intervalSeconds: Int
        get() = prefs.getInt(KEY_INTERVAL_SECONDS, DEFAULT_INTERVAL_SECONDS)
        set(value) = prefs.edit {
            putInt(KEY_INTERVAL_SECONDS, value.coerceIn(MIN_INTERVAL_SECONDS, MAX_INTERVAL_SECONDS))
        }

    var nightModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_MODE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_NIGHT_MODE_ENABLED, value) }

    var nightStartMinuteOfDay: Int
        get() = prefs.getInt(KEY_NIGHT_START_MINUTE, DEFAULT_NIGHT_START_MINUTE)
        set(value) = prefs.edit { putInt(KEY_NIGHT_START_MINUTE, value) }

    var nightEndMinuteOfDay: Int
        get() = prefs.getInt(KEY_NIGHT_END_MINUTE, DEFAULT_NIGHT_END_MINUTE)
        set(value) = prefs.edit { putInt(KEY_NIGHT_END_MINUTE, value) }

    companion object {
        const val DEFAULT_INTERVAL_SECONDS = 30
        const val MIN_INTERVAL_SECONDS = 5
        const val MAX_INTERVAL_SECONDS = 300
        const val INTERVAL_STEP_SECONDS = 5
        const val DEFAULT_NIGHT_START_MINUTE = 22 * 60
        const val DEFAULT_NIGHT_END_MINUTE = 7 * 60

        private const val PREFS_NAME = "photo_frame_settings"
        private const val KEY_FOLDER_URI = "photo_folder_uri"
        private const val KEY_INTERVAL_SECONDS = "interval_seconds"
        private const val KEY_NIGHT_MODE_ENABLED = "night_mode_enabled"
        private const val KEY_NIGHT_START_MINUTE = "night_start_minute"
        private const val KEY_NIGHT_END_MINUTE = "night_end_minute"
    }
}
