package dev.tapscript.engine.core.action

import dev.tapscript.engine.api.model.PixelPoint
import dev.tapscript.engine.api.model.PixelSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class CommandRandomizer(
    private val random: Random = Random.Default,
) {
    fun jitterPoint(point: PixelPoint, radiusPx: Int, screenSize: PixelSize): PixelPoint {
        val radius = radiusPx.coerceIn(0, MAX_POSITION_JITTER_PX)
        if (radius == 0) return point

        // sqrt() makes the samples uniform over the circle area rather than
        // concentrating them around its centre.
        val distance = sqrt(random.nextDouble()) * radius
        val angle = random.nextDouble() * 2.0 * PI
        val x = point.x + (cos(angle) * distance).roundToInt()
        val y = point.y + (sin(angle) * distance).roundToInt()
        return PixelPoint(
            x = x.coerceIn(0, screenSize.width - 1),
            y = y.coerceIn(0, screenSize.height - 1),
        )
    }

    fun durationBetween(minDurationMs: Long, maxDurationMs: Long): Long {
        val min = minDurationMs.coerceIn(0, MAX_WAIT_MS)
        val max = maxDurationMs.coerceIn(0, MAX_WAIT_MS)
        require(min <= max) { "Minimum duration must not exceed maximum duration" }
        return if (min == max) min else random.nextLong(min, max + 1)
    }

    fun durationAround(baseDurationMs: Long, jitterMs: Long): Long {
        val base = baseDurationMs.coerceIn(1, MAX_WAIT_MS)
        val jitter = jitterMs.coerceIn(0, MAX_WAIT_MS)
        val min = (base - jitter).coerceAtLeast(1)
        val max = (base + jitter).coerceAtMost(MAX_WAIT_MS)
        return durationBetween(min, max)
    }

    companion object {
        const val MAX_POSITION_JITTER_PX = 1_000
        const val MAX_WAIT_MS = 60_000L
    }
}
