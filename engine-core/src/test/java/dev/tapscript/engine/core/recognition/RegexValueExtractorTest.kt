package dev.tapscript.engine.core.recognition

import dev.tapscript.engine.api.model.MatchMode
import dev.tapscript.engine.api.model.RegexExtractorSpec
import dev.tapscript.engine.api.model.RegexFieldSpec
import dev.tapscript.engine.api.model.ValueType
import org.junit.Assert.assertEquals
import org.junit.Test

class RegexValueExtractorTest {
    private val extractor = RegexValueExtractor()

    @Test
    fun extractsNamedNumericGroup() {
        val result = extractor.extract(
            text = "+18.32% Attack Speed",
            spec = RegexExtractorSpec(
                variable = "candidate.attackSpeed",
                pattern = "\\+(?<value>\\d+(?:\\.\\d+)?)% Attack Speed",
                fields = listOf(RegexFieldSpec("value", "value", ValueType.NUMBER)),
            ),
        )

        assertEquals(18.32, result["candidate.attackSpeed"])
    }

    @Test
    fun extractsAllRowsAsMaps() {
        val result = extractor.extract(
            text = "+18.32% Attack Speed\n+31.68% Double Hit Chance",
            spec = RegexExtractorSpec(
                variable = "candidate.stats",
                pattern = "\\+(?<amount>\\d+(?:\\.\\d+)?)% (?<name>[^\\n]+)",
                matchMode = MatchMode.ALL,
                fields = listOf(
                    RegexFieldSpec("value", "amount", ValueType.NUMBER),
                    RegexFieldSpec("name", "name", ValueType.TEXT),
                ),
            ),
        )

        val rows = result["candidate.stats"] as List<*>
        assertEquals(2, rows.size)
        assertEquals("Attack Speed", (rows[0] as Map<*, *>)["name"])
    }
}
