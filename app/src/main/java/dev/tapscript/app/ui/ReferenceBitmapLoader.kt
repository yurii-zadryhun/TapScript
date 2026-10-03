package dev.tapscript.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

internal object ReferenceBitmapLoader {
    fun load(context: Context, uri: Uri): Bitmap? = runCatching {
        context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
    }.getOrNull()
}
