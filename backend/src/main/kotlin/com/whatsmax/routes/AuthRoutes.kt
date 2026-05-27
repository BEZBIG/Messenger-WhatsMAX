/** Маршруты регистрации, профиля и sign-out. */
package com.whatsmax.routes

import com.google.firebase.auth.FirebaseAuth
import com.whatsmax.domain.models.CreateUserRequest
import com.whatsmax.domain.repositories.UserRepository
import com.whatsmax.plugins.FirebasePrincipal
import com.whatsmax.plugins.invalidateAuthCache
import com.whatsmax.utils.Validation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Route.authRoutes(userRepository: UserRepository) {

    route("/auth") {

        authenticate("firebase") {
          rateLimit(RateLimitName("auth")) {
            post("/register") {
                val principal = call.principal<FirebasePrincipal>()!!
                val raw = call.receive<CreateUserRequest>()

                Validation.validateUsername(raw.username)
                val request = raw.copy(
                    username    = raw.username,
                    displayName = Validation.sanitizeDisplayName(raw.displayName)
                )

                val existing = userRepository.getUserByUid(principal.uid)
                if (existing != null) {
                    call.respond(HttpStatusCode.OK, existing)
                    return@post
                }

                val user = userRepository.createUser(principal.uid, request)
                call.respond(HttpStatusCode.Created, user)
            }

            get("/me") {
                val principal = call.principal<FirebasePrincipal>()!!
                val user = userRepository.getUserByUid(principal.uid)
                    ?: throw NoSuchElementException("User not found. Please register first.")
                call.respond(user)
            }

            delete("/me") {
                val principal = call.principal<FirebasePrincipal>()!!
                userRepository.deleteUser(principal.uid)
                call.respond(HttpStatusCode.NoContent)
            }

            post("/sign-out") {
                val principal = call.principal<FirebasePrincipal>()!!
                withContext(Dispatchers.IO) {
                    FirebaseAuth.getInstance().revokeRefreshTokens(principal.uid)
                }
                call.request.headers["Authorization"]
                    ?.removePrefix("Bearer ")?.removePrefix("bearer ")
                    ?.let { invalidateAuthCache(it) }
                call.respond(HttpStatusCode.OK, mapOf("status" to "signed_out"))
            }
          }
        }
    }
}
