package dev.tapscript.engine.core.recognition

import android.graphics.Bitmap
import dev.tapscript.engine.api.model.NormalizedRect
import dev.tapscript.engine.api.model.PixelSize

class BitmapRegionCropper {
    fun crop(source: Bitmap, bounds: NormalizedRect): Bitmap {
        val rect = bounds.toPixelRect(PixelSize(source.width, source.height))
        return Bitmap.createBitmap(source, rect.left, rect.top, rect.width(), rect.height())
    }
}
