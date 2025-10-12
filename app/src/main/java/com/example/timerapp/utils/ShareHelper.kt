package com.example.timerapp.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ShareHelper {

    fun shareQRCodeImage(context: Context, qrCodeBitmap: Bitmap, text: String) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "qr_code_to_share.png")
            val fileOutputStream = FileOutputStream(file)
            qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.close()

            val uri: Uri = FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".fileprovider",
                file
            )

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "QR-Code teilen...")
            context.startActivity(chooser)

        } catch (e: Exception) {
            Log.e("ShareHelper", "Fehler beim Teilen des Bildes", e)
        }
    }

    fun saveImageToGallery(context: Context, bitmap: Bitmap, displayName: String) {
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(imageCollection, contentValues)

        try {
            uri?.let {
                // HIER IST DIE KORREKTUR: Wir verwenden ?.use, um sicher mit dem Stream zu arbeiten
                resolver.openOutputStream(it)?.use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                        throw Exception("Bitmap Komprimierung fehlgeschlagen")
                    }
                } ?: throw Exception("OutputStream konnte nicht geöffnet werden")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                Toast.makeText(context, "QR-Code in Galerie gespeichert!", Toast.LENGTH_SHORT).show()
            } ?: throw Exception("MediaStore URI konnte nicht erstellt werden")
        } catch (e: Exception) {
            Log.e("ShareHelper", "Fehler beim Speichern des Bildes", e)
            Toast.makeText(context, "Speichern fehlgeschlagen.", Toast.LENGTH_SHORT).show()
        }
    }
}