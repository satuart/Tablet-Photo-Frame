package com.satuart.tabletphotoframe.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

class LoadPhotosUseCases {

    fun getPhotos(context: Context, folderUri: Uri?): List<PhotoRef> {
        if (folderUri != null) {
            val fromTree = loadFromTree(context, folderUri)
            if (fromTree.isNotEmpty()) return fromTree
        }
        return getSdCardImages().map { PhotoRef.LocalFile(it) }
    }

    fun getSdCardImages(): List<File> {
        val possiblePaths = listOf(
            "/sdcard/Pictures",
        )

        val result = mutableListOf<File>()

        for (path in possiblePaths) {
            runCatching {
                val dir = File(path)
                if (dir.exists() && dir.isDirectory) {
                    dir.walk().forEach { file ->
                        if (file.isFile && file.extension.lowercase() in IMAGE_EXTENSIONS) {
                            result.add(file)
                        }
                    }
                }
            }
        }

        return result
    }

    private fun loadFromTree(context: Context, folderUri: Uri): List<PhotoRef> {
        val root = runCatching { DocumentFile.fromTreeUri(context, folderUri) }.getOrNull() ?: return emptyList()
        val result = mutableListOf<PhotoRef>()
        collectImages(root, result)
        return result
    }

    private fun collectImages(dir: DocumentFile, into: MutableList<PhotoRef>) {
        for (child in dir.listFiles()) {
            when {
                child.isDirectory -> collectImages(child, into)
                child.isFile && isImage(child) -> into.add(PhotoRef.DocumentUri(child.uri))
            }
        }
    }

    private fun isImage(doc: DocumentFile): Boolean {
        val mime = doc.type
        if (mime != null && mime.startsWith("image/")) return true
        val ext = doc.name?.substringAfterLast('.', "")?.lowercase()
        return ext in IMAGE_EXTENSIONS
    }

    companion object {
        private val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png")
    }
}
