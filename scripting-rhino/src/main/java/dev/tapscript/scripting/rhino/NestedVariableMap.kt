package dev.tapscript.scripting.rhino

internal object NestedVariableMap {
    fun fromFlat(values: Map<String, Any?>): Map<String, Any?> {
        val root = linkedMapOf<String, Any?>()
        values.forEach { (path, value) -> insert(root, path.split('.'), value) }
        return root
    }

    @Suppress("UNCHECKED_CAST")
    private fun insert(root: MutableMap<String, Any?>, segments: List<String>, value: Any?) {
        if (segments.isEmpty()) return
        var cursor = root
        segments.dropLast(1).forEach { segment ->
            val next = cursor[segment]
            cursor = if (next is MutableMap<*, *>) {
                next as MutableMap<String, Any?>
            } else {
                linkedMapOf<String, Any?>().also { cursor[segment] = it }
            }
        }
        cursor[segments.last()] = value
    }
}
