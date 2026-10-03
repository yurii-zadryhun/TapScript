package dev.tapscript.platform.android.capture

import android.graphics.Bitmap
import android.media.Image

class ScreenImageConverter {
    fun toBitmap(image: Image): Bitmap {
        val plane = image.planes.firstOrNull() ?: error("Captured image has no planes")
        val buffer = plane.buffer
        buffer.rewind()

        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val paddedWidth = image.width + rowPadding / pixelStride

        val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(buffer)
        if (paddedWidth == image.width) return padded

        return Bitmap.createBitmap(padded, 0, 0, image.width, image.height).also {
            padded.recycle()
        }
    }
}
