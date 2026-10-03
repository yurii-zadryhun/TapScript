package dev.tapscript.engine.core.recognition

import dev.tapscript.engine.api.model.MatchMode
import dev.tapscript.engine.api.model.RegexExtractorSpec
import dev.tapscript.engine.api.model.RegexFieldSpec
import dev.tapscript.engine.api.model.ValueType
import java.util.regex.Pattern

class RegexValueExtractor {
    fun extract(text: String, spec: RegexExtractorSpec): Map<String, Any?> {
        val flags = if (spec.ignoreCase) Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE else 0
        val matcher = Pattern.compile(spec.pattern, flags).matcher(text)

        val values = buildList {
            while (matcher.find()) {
                add(extractMatch(matcher, spec.fields))
                if (spec.matchMode == MatchMode.FIRST) break
            }
        }

        if (values.isEmpty()) return emptyMap()
        return mapOf(
            spec.variable to when (spec.matchMode) {
                MatchMode.FIRST -> values.first()
                MatchMode.ALL -> values
            },
        )
    }

    private fun extractMatch(
        matcher: java.util.regex.Matcher,
        fields: List<RegexFieldSpec>,
    ): Any? {
        if (fields.size == 1 && fields.first().name == "value") {
            return parseValue(readGroup(matcher, fields.first()), fields.first().type)
        }

        return fields.associate { field ->
            field.name to parseValue(readGroup(matcher, field), field.type)
        }
    }

    private fun readGroup(matcher: java.util.regex.Matcher, field: RegexFieldSpec): String? =
        runCatching { matcher.group(field.group) }.getOrNull()

    private fun parseValue(raw: String?, type: ValueType): Any? {
        if (raw == null) return null
        return when (type) {
            ValueType.TEXT -> raw.trim()
            ValueType.NUMBER -> raw.trim().replace(',', '.').toDoubleOrNull()
            ValueType.BOOLEAN -> when (raw.trim().lowercase()) {
                "true", "1", "yes", "on" -> true
                "false", "0", "no", "off" -> false
                else -> null
            }
        }
    }
}
