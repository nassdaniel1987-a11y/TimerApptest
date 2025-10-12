package com.example.timerapp.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareHelper {

    fun shareQRCodeImage(context: Context, qrCodeBitmap: Bitmap, text: String) {
        try {
            // 1. Bild im Cache-Verzeichnis der App speichern
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs() // Stellt sicher, dass der Ordner existiert
            val file = File(cachePath, "qr_code_to_share.png")
            val fileOutputStream = FileOutputStream(file)
            qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.close()

            // 2. Eine teilbare URI für die Datei erstellen
            val uri: Uri = FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".fileprovider",
                file
            )

            // 3. Den Android "Teilen"-Dialog aufrufen
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
}