package com.satuart.tabletphotoframe.view

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.satuart.tabletphotoframe.R
import com.satuart.tabletphotoframe.data.SettingsRepository
import com.satuart.tabletphotoframe.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val settingsRepository by lazy { SettingsRepository(this) }

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            settingsRepository.photoFolderUri = uri
            renderFolderPath(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.settingsRoot.setOnClickListener { finish() }
        binding.buttonCloseSettings.setOnClickListener { finish() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.buttonChangeFolder.setOnClickListener { folderPicker.launch(null) }
        } else {
            // ACTION_OPEN_DOCUMENT_TREE (SAF folder picker) requires API 21; below that,
            // the app always scans the legacy default folder — see LoadPhotosUseCases.getPhotos.
            binding.buttonChangeFolder.visibility = View.GONE
        }
        binding.buttonIntervalMinus.setOnClickListener { adjustInterval(-SettingsRepository.INTERVAL_STEP_SECONDS) }
        binding.buttonIntervalPlus.setOnClickListener { adjustInterval(SettingsRepository.INTERVAL_STEP_SECONDS) }
        binding.switchNightMode.setOnCheckedChangeListener { _, isChecked ->
            settingsRepository.nightModeEnabled = isChecked
            renderNightModeVisibility(isChecked)
        }
        binding.textNightStart.setOnClickListener { pickTime(isStart = true) }
        binding.textNightEnd.setOnClickListener { pickTime(isStart = false) }

        renderFolderPath(settingsRepository.photoFolderUri)
        renderInterval(settingsRepository.intervalSeconds)
        binding.switchNightMode.isChecked = settingsRepository.nightModeEnabled
        renderNightModeVisibility(settingsRepository.nightModeEnabled)
        renderNightTimes()
    }

    private fun adjustInterval(deltaSeconds: Int) {
        val next = (settingsRepository.intervalSeconds + deltaSeconds)
            .coerceIn(SettingsRepository.MIN_INTERVAL_SECONDS, SettingsRepository.MAX_INTERVAL_SECONDS)
        settingsRepository.intervalSeconds = next
        renderInterval(next)
    }

    private fun renderInterval(seconds: Int) {
        binding.textIntervalValue.text = getString(R.string.settings_interval_value_format, seconds)
    }

    private fun renderFolderPath(uri: Uri?) {
        binding.textPhotoFolderPath.text = uri
            ?.let { runCatching { DocumentFile.fromTreeUri(this, it)?.name }.getOrNull() ?: it.toString() }
            ?: getString(R.string.settings_no_folder_selected)
    }

    private fun renderNightModeVisibility(enabled: Boolean) {
        binding.groupNightTimes.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun renderNightTimes() {
        binding.textNightStart.text = formatMinuteOfDay(settingsRepository.nightStartMinuteOfDay)
        binding.textNightEnd.text = formatMinuteOfDay(settingsRepository.nightEndMinuteOfDay)
    }

    private fun pickTime(isStart: Boolean) {
        val current = if (isStart) settingsRepository.nightStartMinuteOfDay else settingsRepository.nightEndMinuteOfDay
        TimePickerDialog(
            this,
            { _, hour, minute ->
                val minuteOfDay = hour * 60 + minute
                if (isStart) settingsRepository.nightStartMinuteOfDay = minuteOfDay else settingsRepository.nightEndMinuteOfDay = minuteOfDay
                renderNightTimes()
            },
            current / 60,
            current % 60,
            false,
        ).show()
    }

    private fun formatMinuteOfDay(minuteOfDay: Int): String {
        val hour = minuteOfDay / 60
        val minute = minuteOfDay % 60
        val amPm = if (hour < 12) "AM" else "PM"
        val hour12 = (hour % 12).let { if (it == 0) 12 else it }
        return String.format("%d:%02d %s", hour12, minute, amPm)
    }
}
