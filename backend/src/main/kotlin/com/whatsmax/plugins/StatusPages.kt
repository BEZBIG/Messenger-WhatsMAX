/** Глобальный обработчик ошибок Ktor. */
package com.whatsmax.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val error: String, val code: Int)

fun Application.configureStatusPages() {
    val devMode = developmentMode

    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Bad request", 400))
        }
        exception<SecurityException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ErrorResponse(cause.message ?: "Forbidden", 403))
        }
        exception<NoSuchElementException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message ?: "Not found", 404))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            val msg = if (devMode) {
                buildString {
                    append(cause::class.simpleName ?: "Error")
                    cause.message?.let { append(": $it") }
                    cause.cause?.message?.let { append(" | caused by: $it") }
                }
            } else {
                "Internal server error"
            }
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(msg, 500))
        }
        status(HttpStatusCode.Unauthorized) { call, _ ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized. Provide valid Firebase token.", 401))
        }
    }
}
