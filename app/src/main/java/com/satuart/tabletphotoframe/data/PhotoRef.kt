package com.satuart.tabletphotoframe.data

import android.net.Uri
import java.io.File

sealed class PhotoRef {
    data class LocalFile(val file: File) : PhotoRef()
    data class DocumentUri(val uri: Uri) : PhotoRef()
}
