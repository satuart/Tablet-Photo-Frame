package com.satuart.tabletphotoframe

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private val loader = LoadPhotosUseCases()
    private val photosFiles: MutableList<File> = ArrayList()
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        imageView = findViewById<ImageView>(R.id.photo_frame_field)
    }

    override fun onResume() {
        super.onResume()
        loader.getSdCardImages().forEach {
            photosFiles.add(it)
        }
        startSlideshow()
    }

    fun startSlideshow() =
        CoroutineScope(Dispatchers.Main).launch {
            while (this.isActive) {
                runCatching {
                    photosFiles.shuffle()

                    photosFiles.forEach {
                        runCatching {
                            val bitmap = BitmapFactory.decodeFile(it.absolutePath)
                            imageView.setImageBitmap(bitmap)
                        }

                        delay(60000)
                    }
                }
            }
        }
}
