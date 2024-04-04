package com.example.qr_gren

import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.qr_gren.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    lateinit var b: ActivityMainBinding
var workbitmap:Bitmap?=null
    var org:Bitmap?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.edit.setOnClickListener {
            uiedit()
        }
        b.back.setOnClickListener {
            uiback()
        }
        grid()
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                88
            )
        }

        b.qrtext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                var input = s.toString()
                if (input.contains(" ", true))
                    input = input.replace(" ", "%20")
                var api = "https://api.qrserver.com/v1/create-qr-code/?data=${input}&size=500x500"
                GlobalScope.launch {
                    workbitmap = loadimagebitmapfromurl(api)
                    org=loadimagebitmapfromurl(api)
                    runOnUiThread {

                        b.qr.setImageBitmap(workbitmap)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })


        b.download.setOnClickListener {
            Toast.makeText(this, "click", Toast.LENGTH_SHORT).show()
            GlobalScope.launch {
//                savebitmap(workbitmap!!)
                saveImageToGallery(workbitmap!!)
            }
        }
    }

    fun uiedit() {
        b.edit.visibility = View.GONE
        b.back.visibility = View.VISIBLE
        b.qrtextlay.visibility = View.GONE
        b.colorchose.visibility = View.VISIBLE
    }

    fun uiback() {
        b.edit.visibility = View.VISIBLE
        b.back.visibility = View.GONE
        b.qrtextlay.visibility = View.VISIBLE
        b.colorchose.visibility = View.GONE
    }

    private suspend fun loadimagebitmapfromurl(imageurl: String): Bitmap? {
        return withContext(Dispatchers.IO)
        {
            var bitmap: Bitmap? = null
            var inputStream: InputStream? = null
            try {
                val url = URL(imageurl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                inputStream = connection.inputStream
                bitmap = BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                Log.e("qr gen fail ", " reason $e")
            } finally {
                inputStream?.close()
            }
            bitmap
        }
    }
    private fun savebitmap(bitmap: Bitmap?) {


        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "QRImagese"
        )

        try {
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val fileName = "qrimagee.png"
            val file = File(directory, fileName)
            val outputStream = FileOutputStream(file)
            bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // Trigger media scan
            MediaScannerConnection.scanFile(
                applicationContext,
                arrayOf(file.absolutePath),
                null
            ) { path, uri ->
                Log.i("MediaScanner", "Scanned $path")
            }

            Toast.makeText(this, "QR image saved", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("SaveBitmap", "Error saving QR image: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 88) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {

            }
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "qrimage.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let { imageUri ->
            resolver.openOutputStream(imageUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }


            MediaScannerConnection.scanFile(
                applicationContext,
                arrayOf(imageUri.path),
                arrayOf("image/png"),
                null
            )
        }
    }

    fun grid() {
        val gridLayout = b.colorgrid

        for (i in 0 until gridLayout.childCount) {
            val childView = gridLayout.getChildAt(i)
            if (childView is CardView) {
                childView.setOnClickListener {

                    val backgroundDrawable = childView.cardBackgroundColor
                    val clickedCardColor = backgroundDrawable.defaultColor
                    workbitmap=changeBlackColor(org!!,clickedCardColor)

                    b.qr.setImageBitmap(workbitmap)


                }
            }
        }
    }
    fun changeBlackColor(bitmap: Bitmap, newColor: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val modifiedBitmap = bitmap.copy(bitmap.config, true)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                if (pixel == android.graphics.Color.BLACK) {
                    modifiedBitmap.setPixel(x, y, newColor)
                }
            }
        }

        return modifiedBitmap
    }

}