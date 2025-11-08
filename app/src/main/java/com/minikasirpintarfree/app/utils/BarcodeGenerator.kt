package com.minikasirpintarfree.app.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object BarcodeGenerator {
    
    /**
     * Generate unique barcode dengan format: MKP-{timestamp}-{random}
     * Example: MKP-20250108-A7F
     */
    fun generateLocalBarcode(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val random = UUID.randomUUID().toString().substring(0, 3).uppercase(Locale.getDefault())
        return "MKP-$timestamp-$random"
    }
    
    /**
     * Generate barcode bitmap dari string content
     * @param content Barcode content (text/number)
     * @param width Bitmap width (default 400px)
     * @param height Bitmap height (default 200px)
     * @return Bitmap of barcode image
     */
    fun generateBarcodeBitmap(
        content: String,
        width: Int = 400,
        height: Int = 200
    ): Bitmap? {
        return try {
            val writer = MultiFormatWriter()
            val bitMatrix: BitMatrix = writer.encode(
                content,
                BarcodeFormat.CODE_128,
                width,
                height
            )
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Save barcode bitmap to gallery menggunakan Scoped Storage (Android 10+)
     * @param context Application context
     * @param bitmap Barcode bitmap to save
     * @param filename Filename without extension
     * @return Uri of saved image, or null if failed
     */
    fun saveBarcodeToGallery(
        context: Context,
        bitmap: Bitmap,
        filename: String
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (Scoped Storage) - NO PERMISSION NEEDED
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "BARCODE_$filename.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MiniKasir")
                }
                
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                }
                uri
            } else {
                // Android 9 and below (Legacy Storage)
                val imagesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                ).toString() + "/MiniKasir"
                
                val dir = File(imagesDir)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                
                val file = File(dir, "BARCODE_$filename.png")
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                
                // Add to MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                }
                context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Create barcode image file untuk sharing
     * @param context Application context
     * @param bitmap Barcode bitmap
     * @param filename Filename
     * @return File object or null
     */
    fun createBarcodeFile(
        context: Context,
        bitmap: Bitmap,
        filename: String
    ): File? {
        return try {
            val cacheDir = File(context.cacheDir, "barcodes")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val file = File(cacheDir, "BARCODE_$filename.png")
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}