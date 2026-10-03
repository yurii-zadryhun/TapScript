package dev.tapscript.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import dev.tapscript.engine.api.model.NormalizedPoint
import kotlin.math.min

internal data class ImageFitTransform(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    fun toNormalized(offset: Offset): NormalizedPoint? {
        if (width <= 0f || height <= 0f) return null
        if (offset.x !in left..(left + width) || offset.y !in top..(top + height)) return null
        return NormalizedPoint(
            x = ((offset.x - left) / width).coerceIn(0f, 1f),
            y = ((offset.y - top) / height).coerceIn(0f, 1f),
        )
    }

    fun toCanvas(point: NormalizedPoint): Offset = Offset(
        x = left + point.x * width,
        y = top + point.y * height,
    )

    companion object {
        fun create(imageWidth: Int, imageHeight: Int, canvasSize: IntSize): ImageFitTransform {
            if (imageWidth <= 0 || imageHeight <= 0 || canvasSize.width <= 0 || canvasSize.height <= 0) {
                return ImageFitTransform(0f, 0f, 0f, 0f)
            }
            val scale = min(
                canvasSize.width.toFloat() / imageWidth,
                canvasSize.height.toFloat() / imageHeight,
            )
            val width = imageWidth * scale
            val height = imageHeight * scale
            return ImageFitTransform(
                left = (canvasSize.width - width) / 2f,
                top = (canvasSize.height - height) / 2f,
                width = width,
                height = height,
            )
        }
    }
}
