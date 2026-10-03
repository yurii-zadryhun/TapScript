package dev.tapscript.engine.core.frame

import android.graphics.Bitmap
import dev.tapscript.engine.api.model.NormalizedRect
import dev.tapscript.engine.api.model.PixelSize
import dev.tapscript.engine.api.model.ScreenFrame
import kotlin.math.abs

class SampledFrameChangeDetector(
    private val columnsPerRegion: Int = 8,
    private val rowsPerRegion: Int = 6,
) {
    init {
        require(columnsPerRegion > 0) { "columnsPerRegion must be positive" }
        require(rowsPerRegion > 0) { "rowsPerRegion must be positive" }
    }

    private var previous: IntArray? = null

    fun hasMeaningfulChange(
        frame: ScreenFrame,
        regions: List<NormalizedRect>,
        threshold: Double,
    ): Boolean {
        if (threshold <= 0.0) return true

        val effectiveRegions = regions.ifEmpty { listOf(FULL_SCREEN) }
        val current = signature(frame.bitmap, effectiveRegions)
        val old = previous
        previous = current
        if (old == null || old.size != current.size) return true

        var totalDifference = 0L
        for (index in current.indices) {
            totalDifference += abs(current[index] - old[index])
        }

        val normalized = totalDifference.toDouble() / (current.size * 255.0)
        return normalized >= threshold
    }

    fun reset() {
        previous = null
    }

    private fun signature(bitmap: Bitmap, regions: List<NormalizedRect>): IntArray {
        val samplesPerRegion = columnsPerRegion * rowsPerRegion
        val result = IntArray(samplesPerRegion * regions.size)
        val size = PixelSize(bitmap.width, bitmap.height)
        var output = 0

        regions.forEach { normalized ->
            val rect = normalized.toPixelRect(size)
            for (row in 0 until rowsPerRegion) {
                val y = sampleCoordinate(rect.top, rect.bottom, row, rowsPerRegion)
                for (column in 0 until columnsPerRegion) {
                    val x = sampleCoordinate(rect.left, rect.right, column, columnsPerRegion)
                    result[output++] = luminance(bitmap.getPixel(x, y))
                }
            }
        }
        return result
    }

    private fun sampleCoordinate(start: Int, endExclusive: Int, index: Int, count: Int): Int {
        val span = (endExclusive - start).coerceAtLeast(1)
        return (start + ((index + 0.5f) * span / count).toInt())
            .coerceIn(start, endExclusive - 1)
    }

    private fun luminance(pixel: Int): Int {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        return (red * 30 + green * 59 + blue * 11) / 100
    }

    private companion object {
        val FULL_SCREEN = NormalizedRect(0f, 0f, 1f, 1f)
    }
}
