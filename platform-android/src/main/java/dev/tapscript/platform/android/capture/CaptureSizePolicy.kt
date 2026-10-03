package dev.tapscript.platform.android.capture

import dev.tapscript.engine.api.model.PixelSize

class CaptureSizePolicy(
    private val maxWidth: Int = DEFAULT_MAX_WIDTH,
) {
    init {
        require(maxWidth > 0) { "maxWidth must be positive" }
    }

    fun resolve(sourceWidth: Int, sourceHeight: Int): PixelSize {
        require(sourceWidth > 0 && sourceHeight > 0) { "Source size must be positive" }
        if (sourceWidth <= maxWidth) return PixelSize(sourceWidth, sourceHeight)

        val scale = maxWidth.toFloat() / sourceWidth
        return PixelSize(
            width = maxWidth,
            height = (sourceHeight * scale).toInt().coerceAtLeast(1),
        )
    }

    companion object {
        const val DEFAULT_MAX_WIDTH = 1080
    }
}
