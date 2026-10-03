package dev.tapscript.engine.api.model

import android.graphics.Rect
import kotlin.math.roundToInt

data class PixelSize(
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0) { "width must be positive" }
        require(height > 0) { "height must be positive" }
    }
}

data class NormalizedPoint(
    val x: Float,
    val y: Float,
) {
    init {
        require(x in 0f..1f) { "x must be in 0..1" }
        require(y in 0f..1f) { "y must be in 0..1" }
    }

    fun toPixel(size: PixelSize): PixelPoint = PixelPoint(
        x = (x * (size.width - 1)).roundToInt().coerceIn(0, size.width - 1),
        y = (y * (size.height - 1)).roundToInt().coerceIn(0, size.height - 1),
    )
}

data class PixelPoint(
    val x: Int,
    val y: Int,
)

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f) {
            "rectangle coordinates must be in 0..1"
        }
        require(right > left) { "right must be greater than left" }
        require(bottom > top) { "bottom must be greater than top" }
    }

    fun toPixelRect(size: PixelSize): Rect {
        val leftPx = (left * size.width).roundToInt().coerceIn(0, size.width - 1)
        val topPx = (top * size.height).roundToInt().coerceIn(0, size.height - 1)
        val rightPx = (right * size.width).roundToInt().coerceIn(leftPx + 1, size.width)
        val bottomPx = (bottom * size.height).roundToInt().coerceIn(topPx + 1, size.height)
        return Rect(leftPx, topPx, rightPx, bottomPx)
    }
}
