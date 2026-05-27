/** Маршруты поиска, получения и обновления профилей. */
package com.whatsmax.routes

import com.whatsmax.domain.models.UpdateUserRequest
import com.whatsmax.domain.repositories.UserRepository
import com.whatsmax.plugins.FirebasePrincipal
import com.whatsmax.utils.Validation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(userRepository: UserRepository) {

    authenticate("firebase") {
        route("/users") {

            get("/search") {
                val query = call.request.queryParameters["q"]
                    ?: throw IllegalArgumentException("Query parameter 'q' is required")
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val users = userRepository.searchUsers(query, limit)
                call.respond(users)
            }

            get("/{uid}") {
                val uid = call.parameters["uid"]!!
                val user = userRepository.getUserByUid(uid)
                    ?: throw NoSuchElementException("User with uid=$uid not found")
                call.respond(user)
            }

            put("/me") {
                val principal = call.principal<FirebasePrincipal>()!!
                val raw = call.receive<UpdateUserRequest>()
                val cleaned = raw.copy(
                    username    = raw.username?.also { Validation.validateUsername(it) },
                    displayName = raw.displayName?.let { Validation.sanitizeDisplayName(it) },
                    bio         = raw.bio?.let { Validation.sanitizeBio(it) }
                )
                val updated = userRepository.updateUser(principal.uid, cleaned)
                call.respond(updated)
            }

            post("/me/fcm-token") {
                val principal = call.principal<FirebasePrincipal>()!!
                val body = call.receive<Map<String, String>>()
                val token = body["token"] ?: throw IllegalArgumentException("Token required")
                userRepository.updateFcmToken(principal.uid, token)
                call.respond(HttpStatusCode.OK, mapOf("status" to "updated"))
            }
        }
    }
}
