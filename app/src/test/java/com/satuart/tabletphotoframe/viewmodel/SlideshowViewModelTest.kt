package com.satuart.tabletphotoframe.viewmodel

import com.satuart.tabletphotoframe.data.PhotoRef
import com.satuart.tabletphotoframe.repository.PhotoRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SlideshowViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startSlideshow_showsFirstLoadedPhoto() = runTest(dispatcher) {
        val photo = PhotoRef.LocalFile(File("photo1.jpg"))
        val viewModel = SlideshowViewModel(FakePhotoRepository(listOf(photo)))

        viewModel.startSlideshow()
        dispatcher.scheduler.runCurrent()

        assertEquals(photo, viewModel.uiState.value.currentPhoto)
    }

    @Test
    fun togglePause_flipsIsPaused() {
        val viewModel = SlideshowViewModel(FakePhotoRepository(emptyList()))

        assertFalse(viewModel.uiState.value.isPaused)
        viewModel.togglePause()
        assertTrue(viewModel.uiState.value.isPaused)
    }

    @Test
    fun refresh_emitsRefreshedEvent() = runTest(dispatcher) {
        val viewModel = SlideshowViewModel(FakePhotoRepository(listOf(PhotoRef.LocalFile(File("photo1.jpg")))))
        var refreshedCount = 0
        val collector = launch { viewModel.refreshed.collect { refreshedCount++ } }
        dispatcher.scheduler.runCurrent()

        viewModel.refresh()
        dispatcher.scheduler.runCurrent()

        assertEquals(1, refreshedCount)
        collector.cancel()
    }

    private class FakePhotoRepository(private val photos: List<PhotoRef>) : PhotoRepository {
        override suspend fun loadPhotos(): List<PhotoRef> = photos
    }
}
