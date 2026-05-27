/** Rate limiting per-user для защиты от спама и DoS. */
package com.whatsmax.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun Application.configureRateLimit() {
    install(RateLimit) {
        val identityFn: (io.ktor.server.application.ApplicationCall) -> String = { call ->
            call.principal<FirebasePrincipal>()?.uid
                ?: call.request.origin.remoteHost
        }

        register(RateLimitName("messages")) {
            rateLimiter(limit = 30, refillPeriod = 10.seconds)
            requestKey { identityFn(it) }
        }
        register(RateLimitName("reactions")) {
            rateLimiter(limit = 60, refillPeriod = 10.seconds)
            requestKey { identityFn(it) }
        }
        register(RateLimitName("uploads")) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
            requestKey { identityFn(it) }
        }
        register(RateLimitName("auth")) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
            requestKey { identityFn(it) }
        }
    }
}
