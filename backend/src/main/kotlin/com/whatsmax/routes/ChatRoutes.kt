/** CRUD чатов и управление участниками. */
package com.whatsmax.routes

import com.whatsmax.domain.models.CreateChatRequest
import com.whatsmax.domain.repositories.ChatRepository
import com.whatsmax.domain.repositories.UserRepository
import com.whatsmax.plugins.FirebasePrincipal
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.chatRoutes(chatRepository: ChatRepository, userRepository: UserRepository) {

    authenticate("firebase") {
        route("/chats") {

            post {
                val principal = call.principal<FirebasePrincipal>()!!
                val request = call.receive<CreateChatRequest>()

                if (request.type == "direct" && request.memberUids.size == 1) {
                    val existing = chatRepository.findDirectChat(principal.uid, request.memberUids[0])
                    if (existing != null) {
                        call.respond(HttpStatusCode.OK, existing)
                        return@post
                    }
                }

                val chat = chatRepository.createChat(request.copy(
                    memberUids = (request.memberUids + principal.uid).distinct()
                ), principal.uid)
                call.respond(HttpStatusCode.Created, chat)
            }

            get {
                val principal = call.principal<FirebasePrincipal>()!!
                val chats = chatRepository.getUserChats(principal.uid)
                call.respond(chats)
            }

            get("/{id}") {
                val principal = call.principal<FirebasePrincipal>()!!
                val chatId = UUID.fromString(call.parameters["id"])
                if (!chatRepository.isUserInChat(chatId, principal.uid))
                    throw SecurityException("Access denied")
                val chat = chatRepository.getChatById(chatId)
                    ?: throw NoSuchElementException("Chat not found")
                call.respond(chat)
            }

            put("/{id}") {
                val principal = call.principal<FirebasePrincipal>()!!
                val chatId = UUID.fromString(call.parameters["id"])
                if (chatRepository.getMemberRole(chatId, principal.uid) != "admin")
                    throw SecurityException("Only chat admins can update chat settings")
                val body = call.receive<Map<String, String>>()
                val updated = chatRepository.updateChat(
                    chatId, body["name"], body["description"], body["avatarUrl"]
                )
                call.respond(updated)
            }

            post("/{id}/members") {
                val principal = call.principal<FirebasePrincipal>()!!
                val chatId = UUID.fromString(call.parameters["id"])
                if (chatRepository.getMemberRole(chatId, principal.uid) != "admin")
                    throw SecurityException("Only chat admins can add members")
                val body = call.receive<Map<String, String>>()
                val userId = body["userId"] ?: throw IllegalArgumentException("userId required")
                chatRepository.addMember(chatId, userId)
                call.respond(HttpStatusCode.OK, mapOf("status" to "added"))
            }

            delete("/{id}/members/{uid}") {
                val principal = call.principal<FirebasePrincipal>()!!
                val chatId = UUID.fromString(call.parameters["id"])
                val userId = call.parameters["uid"]!!
                val callerRole = chatRepository.getMemberRole(chatId, principal.uid)
                val isSelf = userId == principal.uid
                if (!isSelf && callerRole != "admin")
                    throw SecurityException("Only chat admins can remove other members")
                if (callerRole == null && !isSelf)
                    throw SecurityException("You are not a member of this chat")
                chatRepository.removeMember(chatId, userId)
                call.respond(HttpStatusCode.OK, mapOf("status" to "removed"))
            }

            get("/{id}/members") {
                val principal = call.principal<FirebasePrincipal>()!!
                val chatId = UUID.fromString(call.parameters["id"])
                if (!chatRepository.isUserInChat(chatId, principal.uid))
                    throw SecurityException("Access denied")
                val members = chatRepository.getChatMembers(chatId)
                call.respond(members)
            }

            delete("/{id}") {
                val principal = call.principal<FirebasePrincipal>()!!
                val chatId = UUID.fromString(call.parameters["id"])
                val chat = chatRepository.getChatById(chatId)
                    ?: throw NoSuchElementException("Chat not found")
                if (chat.createdBy != principal.uid)
                    throw SecurityException("Only the creator can delete this chat")
                chatRepository.deleteChat(chatId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
