package ktor_health_check
import org.jetbrains.ktor.http.HttpStatusCode

// bool value = ~5. name = 10, four quotes and a comma
const private val EST_JSON_PER_KEY = 5 + 10 + 4

private fun checksResultsToJSON(res: Map<String, Boolean>) =
    StringBuffer(res.size * EST_JSON_PER_KEY)
        .apply {
            append('{')
            res.forEach { (k , v) ->
                append('"')
                append(k)
                append("\":")
                append(v)
                append(',')
            }
            append('}')
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