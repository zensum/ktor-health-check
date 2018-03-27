package ktor_health_check

import io.ktor.http.HttpStatusCode

// bool value = ~5. name = 10, four quotes and a comma
private const val EST_JSON_PER_KEY = 5 + 10 + 4

private fun checksResultsToJSON(res: Map<String, Boolean>) =
    StringBuilder(res.size * EST_JSON_PER_KEY)
        .apply {
            append('{')
            // We use a prefix to add a comma before all but the first element
            var prefix = "\""
            res.forEach { (k, v) ->
                append(prefix)
                prefix = ",\""
                append(k)
                append("\":")
                append(v)
            }
            append("}")
        }.toString()

internal suspend fun healthCheck(fn: suspend () -> Map<String, Boolean>) = fn().let {
    val success = it.values.all { it }
    val json = checksResultsToJSON(it)
    val status = if (success) {
        HttpStatusCode.OK
    } else {
        HttpStatusCode.InternalServerError
    }
    status to json
}