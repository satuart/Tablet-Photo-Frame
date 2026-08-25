package com.satuart.tabletphotoframe.repository

import android.content.Context
import com.satuart.tabletphotoframe.data.LoadPhotosUseCases
import com.satuart.tabletphotoframe.data.PhotoRef
import com.satuart.tabletphotoframe.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PhotoRepository {
    suspend fun loadPhotos(): List<PhotoRef>
}

class SdCardPhotoRepository(
    context: Context,
    private val settingsRepository: SettingsRepository,
    private val loader: LoadPhotosUseCases = LoadPhotosUseCases(),
) : PhotoRepository {

    private val appContext = context.applicationContext

    override suspend fun loadPhotos(): List<PhotoRef> = withContext(Dispatchers.IO) {
        loader.getPhotos(appContext, settingsRepository.photoFolderUri)
    }
}
