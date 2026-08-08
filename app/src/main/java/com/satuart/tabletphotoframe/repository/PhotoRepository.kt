package com.satuart.tabletphotoframe.repository

import com.satuart.tabletphotoframe.data.LoadPhotosUseCases
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PhotoRepository {
    suspend fun loadPhotos(): List<File>
}

class SdCardPhotoRepository(
    private val loader: LoadPhotosUseCases = LoadPhotosUseCases(),
) : PhotoRepository {

    override suspend fun loadPhotos(): List<File> = withContext(Dispatchers.IO) {
        loader.getSdCardImages()
    }
}
