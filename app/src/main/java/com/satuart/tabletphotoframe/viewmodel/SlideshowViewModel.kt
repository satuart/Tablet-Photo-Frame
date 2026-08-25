package com.satuart.tabletphotoframe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.satuart.tabletphotoframe.repository.PhotoRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SlideshowUiState(
    val currentPhoto: File? = null,
    val isPaused: Boolean = false,
)

class SlideshowViewModel(private val repository: PhotoRepository) : ViewModel() {

    private val photos: MutableList<File> = mutableListOf()

    private val _uiState = MutableStateFlow(SlideshowUiState())
    val uiState: StateFlow<SlideshowUiState> = _uiState.asStateFlow()

    private val _refreshed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshed: SharedFlow<Unit> = _refreshed.asSharedFlow()

    private var slideshowJob: Job? = null

    fun startSlideshow() {
        viewModelScope.launch {
            reloadPhotos()
            restartLoop()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            reloadPhotos()
            restartLoop()
            _refreshed.emit(Unit)
        }
    }

    fun stopSlideshow() {
        slideshowJob?.cancel()
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    private suspend fun reloadPhotos() {
        val files = repository.loadPhotos()
        photos.clear()
        photos.addAll(files)
    }

    private fun restartLoop() {
        slideshowJob?.cancel()
        slideshowJob = viewModelScope.launch { runLoop() }
    }

    private suspend fun CoroutineScope.runLoop() {
        while (isActive) {
            if (photos.isEmpty()) return
            photos.shuffle()
            for (file in photos) {
                if (!isActive) break
                _uiState.update { it.copy(currentPhoto = file) }
                awaitUnpausedDelay(PHOTO_INTERVAL_MS)
            }
        }
    }

    private suspend fun awaitUnpausedDelay(totalMs: Long) {
        var remaining = totalMs
        while (remaining > 0) {
            delay(DELAY_STEP_MS)
            if (!_uiState.value.isPaused) remaining -= DELAY_STEP_MS
        }
    }

    companion object {
        private const val PHOTO_INTERVAL_MS = 60_000L
        private const val DELAY_STEP_MS = 250L
    }
}

class SlideshowViewModelFactory(
    private val repository: PhotoRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SlideshowViewModel(repository) as T
    }
}
