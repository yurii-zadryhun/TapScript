package dev.tapscript.engine.core.action

import dev.tapscript.engine.api.model.PixelPoint
import dev.tapscript.engine.api.model.PixelSize
import kotlin.math.hypot
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandRandomizerTest {
    @Test
    fun jitteredPointsStayOnScreenAndInsideRadius() {
        val randomizer = CommandRandomizer(Random(42))
        val origin = PixelPoint(2, 2)
        val screen = PixelSize(100, 100)

        repeat(500) {
            val point = randomizer.jitterPoint(origin, 20, screen)
            assertTrue(point.x in 0 until screen.width)
            assertTrue(point.y in 0 until screen.height)
            assertTrue(hypot((point.x - origin.x).toDouble(), (point.y - origin.y).toDouble()) <= 21.0)
        }
    }

    @Test
    fun randomDurationUsesInclusiveBounds() {
        val randomizer = CommandRandomizer(Random(7))
        repeat(200) {
            assertTrue(randomizer.durationBetween(120, 180) in 120L..180L)
        }
        assertEquals(150L, randomizer.durationBetween(150, 150))
    }

    @Test(expected = IllegalArgumentException::class)
    fun randomDurationRejectsReversedRange() {
        CommandRandomizer(Random(1)).durationBetween(200, 100)
    }
}
