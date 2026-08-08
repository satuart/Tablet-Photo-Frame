package com.satuart.tabletphotoframe.data

import android.os.Environment
import java.io.File

class LoadPhotosUseCases {

    fun getSdCardImages(): List<File> {
        val possiblePaths = listOf(
            "/sdcard/Pictures",
        )

        val imageExtensions = listOf("jpg", "jpeg", "png")

        val result = mutableListOf<File>()

        for (path in possiblePaths) {
            runCatching {
                val dir = File(path)
                if (dir.exists() && dir.isDirectory) {
                    val content = dir.listFiles()
                    dir.walk().forEach { file ->
                        if (file.isFile) {
                            val ext = file.extension.lowercase()
                            if (ext in imageExtensions) {
                                result.add(file)
                            }
                        }
                    }
                }
            }
        }

        return result
    }

}