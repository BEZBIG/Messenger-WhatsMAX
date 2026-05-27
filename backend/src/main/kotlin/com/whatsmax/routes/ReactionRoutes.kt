/** GET/PUT/DELETE реакций с проверкой доступа к сущности. */
package com.whatsmax.routes

import com.whatsmax.domain.models.ReactionRequest
import com.whatsmax.domain.repositories.ChannelRepository
import com.whatsmax.domain.repositories.ChatRepository
import com.whatsmax.domain.repositories.MessageRepository
import com.whatsmax.domain.repositories.ReactionRepository
import com.whatsmax.plugins.FirebasePrincipal
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.reactionRoutes(
    reactionRepository: ReactionRepository,
    messageRepository: MessageRepository,
    chatRepository: ChatRepository,
    channelRepository: ChannelRepository
) {
    suspend fun assertAccess(type: String, entityId: UUID, userId: String) {
        val allowed = when (type) {
            "message" -> {
                val chatId = messageRepository.getChatIdOf(entityId)
                    ?: throw NoSuchElementException("Message not found")
                chatRepository.isUserInChat(chatId, userId)
            }
            "channel_post" -> {
                val channelId = channelRepository.getChannelIdOfPost(entityId)
                    ?: throw NoSuchElementException("Post not found")
                channelRepository.canViewChannel(channelId, userId)
            }
            "comment" -> {
                val channelId = channelRepository.getChannelIdOfComment(entityId)
                    ?: throw NoSuchElementException("Comment not found")
                channelRepository.canViewChannel(channelId, userId)
            }
            else -> throw IllegalArgumentException("Unknown entity type: $type")
        }
        if (!allowed) throw SecurityException("Access denied to this entity")
    }

    authenticate("firebase") {
        rateLimit(RateLimitName("reactions")) {
        route("/reactions/{type}/{entityId}") {

            get {
                val principal = call.principal<FirebasePrincipal>()!!
                val type      = call.parameters["type"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val entityId  = call.parameters["entityId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

                assertAccess(type, entityId, principal.uid)
                call.respond(reactionRepository.getReactions(type, entityId, principal.uid))
            }

            put {
                val principal = call.principal<FirebasePrincipal>()!!
                val type      = call.parameters["type"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
                val entityId  = call.parameters["entityId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request   = call.receive<ReactionRequest>()

                if (request.emoji.isBlank()) return@put call.respond(HttpStatusCode.BadRequest, "emoji required")
                assertAccess(type, entityId, principal.uid)
                call.respond(reactionRepository.setReaction(type, entityId, principal.uid, request.emoji))
            }

            delete {
                val principal = call.principal<FirebasePrincipal>()!!
                val type      = call.parameters["type"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val entityId  = call.parameters["entityId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)

                assertAccess(type, entityId, principal.uid)
                call.respond(reactionRepository.removeReaction(type, entityId, principal.uid))
            }
        }
        }
    }
}
