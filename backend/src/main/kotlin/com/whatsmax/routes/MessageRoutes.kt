/** Маршруты сообщений чата: история, отправка, правка, удаление, read-receipts. */
package com.whatsmax.routes

import com.whatsmax.domain.models.*
import com.whatsmax.domain.repositories.ChatRepository
import com.whatsmax.domain.repositories.FileRepository
import com.whatsmax.domain.repositories.MessageRepository
import com.whatsmax.plugins.FirebasePrincipal
import com.whatsmax.utils.StorageService
import com.whatsmax.websocket.WebSocketManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

fun Route.messageRoutes(
    messageRepository: MessageRepository,
    chatRepository: ChatRepository,
    fileRepository: FileRepository,
    storageService: StorageService,
    wsManager: WebSocketManager,
    @Suppress("UNUSED_PARAMETER") uploadPath: String
) {
    authenticate("firebase") {
        route("/chats/{chatId}/messages") {

            get {
                val principal = call.principal<FirebasePrincipal>()!!
                val chatId = UUID.fromString(call.parameters["chatId"])
                if (!chatRepository.isUserInChat(chatId, principal.uid))
                    throw SecurityException("Access denied")

                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val before = call.request.queryParameters["before"]?.let { UUID.fromString(it) }
                val messages = messageRepository.getChatMessages(chatId, limit, before)
                call.respond(messages)
            }

            rateLimit(RateLimitName("messages")) {
                post {
                    val principal = call.principal<FirebasePrincipal>()!!
                    val chatId = UUID.fromString(call.parameters["chatId"])
                    if (!chatRepository.isUserInChat(chatId, principal.uid))
                        throw SecurityException("Access denied")

                    val request = call.receive<SendMessageRequest>()
                    val message = messageRepository.sendMessage(chatId, principal.uid, request)

                    val members = chatRepository.getChatMembers(chatId).map { it.userId }
                    val event = WsEvent(
                        type    = WsEventType.NEW_MESSAGE,
                        payload = Json.encodeToString(message)
                    )
                    wsManager.sendToUsers(members, event)

                    call.respond(HttpStatusCode.Created, message)
                }
            }

            put("/{msgId}") {
                val principal = call.principal<FirebasePrincipal>()!!
                val chatId = UUID.fromString(call.parameters["chatId"])
                val msgId  = UUID.fromString(call.parameters["msgId"])
                val request = call.receive<EditMessageRequest>()

                val existing = messageRepository.getMessageById(msgId)
                    ?: throw NoSuchElementException("Message not found")
                if (existing.senderId != principal.uid) throw SecurityException("Cannot edit others' messages")

                val updated = messageRepository.editMessage(msgId, request.content)
                val members = chatRepository.getChatMembers(chatId).map { it.userId }
                wsManager.sendToUsers(members, WsEvent(WsEventType.MESSAGE_EDITED, Json.encodeToString(updated)))
                call.respond(updated)
            }

            delete("/{msgId}") {
                val principal = call.principal<FirebasePrincipal>()!!
                val chatId = UUID.fromString(call.parameters["chatId"])
                val msgId  = UUID.fromString(call.parameters["msgId"])

                val existing = messageRepository.getMessageById(msgId)
                    ?: throw NoSuchElementException("Message not found")
                if (existing.senderId != principal.uid) throw SecurityException("Cannot delete others' messages")

                messageRepository.deleteMessage(msgId)

                existing.fileId?.let { fileIdStr ->
                    val fileId = UUID.fromString(fileIdStr)
                    if (fileRepository.isOrphan(fileId)) {
                        fileRepository.getMeta(fileId)?.let { meta ->
                            meta.objectKey?.let      { storageService.delete(it) }
                            meta.thumbObjectKey?.let { storageService.delete(it) }
                        }
                        fileRepository.deleteFile(fileId)
                    }
                }

                val members = chatRepository.getChatMembers(chatId).map { it.userId }
                wsManager.sendToUsers(members, WsEvent(WsEventType.MESSAGE_DELETED,
                    Json.encodeToString(mapOf("messageId" to msgId.toString(), "chatId" to chatId.toString()))))
                call.respond(HttpStatusCode.NoContent)
            }

            post("/{msgId}/read") {
                val principal = call.principal<FirebasePrincipal>()!!
                val chatId = UUID.fromString(call.parameters["chatId"])
                val msgId  = UUID.fromString(call.parameters["msgId"])

                messageRepository.markAsRead(msgId, principal.uid)
                val members = chatRepository.getChatMembers(chatId).map { it.userId }
                val readEvent = ReadEvent(chatId.toString(), msgId.toString(), principal.uid)
                wsManager.sendToUsers(members, WsEvent(WsEventType.MESSAGE_READ, Json.encodeToString(readEvent)))
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
