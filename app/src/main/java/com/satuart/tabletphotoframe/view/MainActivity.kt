package com.satuart.tabletphotoframe.view

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.satuart.tabletphotoframe.R
import com.satuart.tabletphotoframe.databinding.ActivityMainBinding
import com.satuart.tabletphotoframe.repository.SdCardPhotoRepository
import com.satuart.tabletphotoframe.viewmodel.SlideshowViewModel
import com.satuart.tabletphotoframe.viewmodel.SlideshowViewModelFactory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var menuTriggerController: MenuTriggerController

    private val viewModel: SlideshowViewModel by viewModels {
        SlideshowViewModelFactory(SdCardPhotoRepository())
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
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { state -> renderPhoto(state.currentPhoto) } }
                launch { viewModel.refreshed.collect { showRefreshedToast() } }
            }
        }
    }

    private suspend fun renderPhoto(file: File?) {
        if (file == null) return
        val bitmap = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
        }
        bitmap?.let { binding.photoFrameField.setImageBitmap(it) }
    }

    private fun showRefreshedToast() {
        Toast.makeText(this, getString(R.string.toast_refreshed), Toast.LENGTH_SHORT).show()
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
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}
