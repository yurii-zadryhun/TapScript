package dev.tapscript.engine.api.model

data class RecognitionRegion(
    val id: String,
    val name: String,
    val bounds: NormalizedRect,
    val enabled: Boolean = true,
    val recognizer: RecognizerKind = RecognizerKind.TEXT,
    val textConfig: TextRecognitionConfig = TextRecognitionConfig(),
)

enum class RecognizerKind {
    TEXT,
}

data class TextRecognitionConfig(
    val extractors: List<RegexExtractorSpec> = emptyList(),
)

data class RegexExtractorSpec(
    val variable: String,
    val pattern: String,
    val matchMode: MatchMode = MatchMode.FIRST,
    val fields: List<RegexFieldSpec> = listOf(
        RegexFieldSpec(name = "value", group = "value", type = ValueType.TEXT),
    ),
    val ignoreCase: Boolean = false,
)

data class RegexFieldSpec(
    val name: String,
    val group: String,
    val type: ValueType = ValueType.TEXT,
)

enum class MatchMode {
    FIRST,
    ALL,
}

enum class ValueType {
    TEXT,
    NUMBER,
    BOOLEAN,
}
