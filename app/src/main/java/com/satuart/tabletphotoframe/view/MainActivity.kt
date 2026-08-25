package com.satuart.tabletphotoframe.view

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.satuart.tabletphotoframe.R
import com.satuart.tabletphotoframe.data.PhotoRef
import com.satuart.tabletphotoframe.data.SettingsRepository
import com.satuart.tabletphotoframe.databinding.ActivityMainBinding
import com.satuart.tabletphotoframe.repository.SdCardPhotoRepository
import com.satuart.tabletphotoframe.util.NightModeMath
import com.satuart.tabletphotoframe.viewmodel.SlideshowViewModel
import com.satuart.tabletphotoframe.viewmodel.SlideshowViewModelFactory
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var menuTriggerController: MenuTriggerController
    private val settingsRepository by lazy { SettingsRepository(this) }
    private var toastJob: Job? = null

    private val viewModel: SlideshowViewModel by viewModels {
        SlideshowViewModelFactory(
            repository = SdCardPhotoRepository(this, settingsRepository),
            intervalSecondsProvider = { settingsRepository.intervalSeconds },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyImmersiveMode()

        menuTriggerController = MenuTriggerController(
            activity = this,
            rootView = binding.main,
            imageView = binding.photoFrameField,
            holdRingView = binding.holdRingView,
            menuOverlayBinding = binding.menuOverlay,
            isPausedProvider = { viewModel.uiState.value.isPaused },
            onOpenSettings = ::openSettings,
            onTogglePause = viewModel::togglePause,
            onRefresh = viewModel::refresh,
        )

        observeViewModel()
        observeNightMode()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { state -> renderPhoto(state.currentPhoto) } }
                launch { viewModel.refreshed.collect { showToast(getString(R.string.toast_refreshed)) } }
            }
        }
    }

    private fun observeNightMode() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    menuTriggerController.setBaseDimFactor(
                        NightModeMath.currentDimFactor(
                            enabled = settingsRepository.nightModeEnabled,
                            startMinute = settingsRepository.nightStartMinuteOfDay,
                            endMinute = settingsRepository.nightEndMinuteOfDay,
                            nowMinute = currentMinuteOfDay(),
                        )
                    )
                    delay(NIGHT_CHECK_INTERVAL_MS)
                }
            }
        }
    }

    private fun currentMinuteOfDay(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    private suspend fun renderPhoto(photo: PhotoRef?) {
        if (photo == null) return
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                when (photo) {
                    is PhotoRef.LocalFile -> BitmapFactory.decodeFile(photo.file.absolutePath)
                    is PhotoRef.DocumentUri -> contentResolver.openInputStream(photo.uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
            }.getOrNull()
        }
        bitmap?.let { binding.photoFrameField.setImageBitmap(it) }
    }

    private fun showToast(message: String) {
        toastJob?.cancel()
        val pill = binding.toastPill
        pill.text = message
        pill.animate().cancel()
        pill.alpha = 0f
        pill.visibility = View.VISIBLE
        pill.animate().alpha(1f).setDuration(TOAST_FADE_MS).start()
        toastJob = lifecycleScope.launch {
            delay(TOAST_VISIBLE_MS)
            pill.animate().alpha(0f).setDuration(TOAST_FADE_MS)
                .withEndAction { pill.visibility = View.GONE }
                .start()
        }
    }

    private fun applyImmersiveMode() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
        viewModel.startSlideshow()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopSlideshow()
    }

    override fun onDestroy() {
        super.onDestroy()
        menuTriggerController.destroy()
    }

    private fun openSettings() {
        showToast(getString(R.string.toast_opening_settings))
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    companion object {
        private const val TOAST_FADE_MS = 200L
        private const val TOAST_VISIBLE_MS = 1600L
        private const val NIGHT_CHECK_INTERVAL_MS = 60_000L
    }
}
