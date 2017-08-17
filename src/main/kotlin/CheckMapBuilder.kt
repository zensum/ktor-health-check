package ktor_health_check

// A checkmap is simply a map of names to Check functions.
private typealias CheckMap = Map<String, Check>

// A CheckMap can be converted to a function returning
// results for each of the checks.
private fun CheckMap.toFunction(): suspend () -> Map<String, Boolean> = {
    mapValues { it.value() }
}

internal class CheckMapBuilder {
    private var inner: CheckMap = emptyMap()
    fun add(name: String, fn: Check) {
        inner += name to fn
    }
    fun notEmpty() = inner.isNotEmpty()
    internal inline fun toFunction() =
        inner.toFunction()
}