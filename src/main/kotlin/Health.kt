package ktor_health_check

import org.jetbrains.ktor.application.ApplicationCallPipeline
import org.jetbrains.ktor.application.ApplicationFeature
import org.jetbrains.ktor.http.ContentType
import org.jetbrains.ktor.request.path
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.util.AttributeKey

// Health and readiness are established by looking
// at a number of checks. A check can be something like
// "Are we connected to the database?" or
// "is this component in a live state?".

// A check is a nullary function returning
// a boolean indicating the success of the check.
typealias Check = suspend () -> Boolean

// We use Kubernetes' recomendations of healthz and readyz
private const val HEALTH_CHECK_URL = "healthz"
private const val READY_CHECK_URL = "readyz"

private fun normalizeURL(url: String) = url.trim('/').also {
    require(url.trim('/').isNotBlank()) {
        "The passed in URL must be more than one" +
            " character not counting a leading slash"
    }
}

class Health private constructor(val cfg: Configuration) {
    fun addInterceptor(pipeline: ApplicationCallPipeline) {
        val checks = cfg.getChecksWithFunctions()
        if (checks.isEmpty()) return
        val lengths = checks.keys.map { it.length }
        val maxL = lengths.max()!!
        val minL = lengths.min()!!
        pipeline.intercept(ApplicationCallPipeline.Call) {
            val path = call.request.path().trim('/')
            if (path.length > maxL || path.length < minL) {
                return@intercept
            }
            val check = checks[path] ?: return@intercept
            val (status, json) = healthCheck(check)
            call.respondText(json, ContentType.Application.Json, status)
            finish()
        }
    }
    class Configuration internal constructor() {
        private var checks: Map<String, CheckMapBuilder> = emptyMap()
        private var noHealth = false
        private var noReady = false

        internal fun getChecksWithFunctions() =
            checks.mapValues { (_, v) -> v.toFunction() }

        private fun ensureDisableUnambiguous(url: String) {
            checks[url]?.let {
                if (it.notEmpty()) {
                    throw AssertionError(
                        "Cannot disable a check which " +
                            "has been assigned functions"
                    )
                }
            }
        }

        /**
         * Calling this disables the default health check on /healthz
         */
        fun disableHealthCheck() {
            noHealth = true
            ensureDisableUnambiguous("healthz")
        }

        /**
         * Calling this disabled the default ready check on /readyz
         */
        fun disableReadyCheck() {
            noReady = true
            ensureDisableUnambiguous("readyz")
        }

        private fun getCheck(url: String) = checks.getOrElse(url) {
            CheckMapBuilder().also {
                checks += url to it
            }
        }

        /**
         * Adds a check function to a custom check living at the specified URL
         */
        fun customCheck(url: String, name: String, check: Check) {
            getCheck(normalizeURL(url)).add(name, check)
        }

        /**
         * Add a health check giving it a name
         */
        fun healthCheck(name: String, check: Check) {
            customCheck(HEALTH_CHECK_URL, name, check)
        }

        /**
         * Add a ready check giving it a name
         */
        fun readyCheck(name: String, check: Check) {
            customCheck(READY_CHECK_URL, name, check)
        }

        internal fun ensureWellKnown() {
            if (!noHealth) {
                getCheck(READY_CHECK_URL)
            }
            if (!noReady) {
                getCheck(HEALTH_CHECK_URL)
            }
        }
    }

    companion object Feature : ApplicationFeature<
        ApplicationCallPipeline, Configuration, Health
        > {
        override val key = AttributeKey<Health>("Health")
        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Configuration.() -> Unit
        ) = Health(
            Configuration()
                .apply(configure)
                .apply { ensureWellKnown() }
        ).apply { addInterceptor(pipeline) }
    }
}