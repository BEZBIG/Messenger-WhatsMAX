/** WS-маршрут: typing, WebRTC signaling, read-receipts. */
package com.whatsmax.routes

import com.google.firebase.auth.FirebaseAuth
import com.whatsmax.domain.models.*
import com.whatsmax.domain.repositories.ChatRepository
import com.whatsmax.domain.repositories.MessageRepository
import com.whatsmax.domain.repositories.UserRepository
import com.whatsmax.websocket.WebSocketManager
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

private val logger = LoggerFactory.getLogger("WebSocketRoutes")

@Serializable
private data class AuthMessage(val type: String, val payload: String)

fun Route.webSocketRoutes(
    wsManager: WebSocketManager,
    userRepository: UserRepository,
    messageRepository: MessageRepository,
    chatRepository: ChatRepository
) {
    webSocket("/ws") {
        val authFrame = withTimeoutOrNull(5_000L) { incoming.receive() }
        if (authFrame !is Frame.Text) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Auth frame required"))
            return@webSocket
        }
        val token = try {
            val msg = Json.decodeFromString<AuthMessage>(authFrame.readText())
            if (msg.type != "auth") throw IllegalArgumentException("Expected type=auth")
            msg.payload
        } catch (e: Exception) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Malformed auth message"))
            return@webSocket
        }

        val uid = try {
            withContext(Dispatchers.IO) {
                FirebaseAuth.getInstance().verifyIdToken(token, true).uid
            }
        } catch (e: Exception) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
            return@webSocket
        }

        wsManager.addSession(uid, this)
        userRepository.updateOnlineStatus(uid, true)
        logger.info("User $uid connected via WebSocket")

        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val text = frame.readText()
                try {
                    val event = Json.decodeFromString<WsEvent>(text)
                    handleWsEvent(uid, event, wsManager, messageRepository, chatRepository)
                } catch (e: Exception) {
                    logger.warn("Failed to parse WS event from $uid: $e")
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            logger.info("User $uid WebSocket closed normally")
        } catch (e: Exception) {
            logger.error("WebSocket error for user $uid", e)
        } finally {
            wsManager.removeSession(uid, this)
            userRepository.updateOnlineStatus(uid, false)
            logger.info("User $uid disconnected")
        }
    }
}

private suspend fun handleWsEvent(
    senderUid: String,
    event: WsEvent,
    wsManager: WebSocketManager,
    messageRepository: MessageRepository,
    chatRepository: ChatRepository
) {
    when (event.type) {

        WsEventType.USER_TYPING -> {
            val typing = Json.decodeFromString<TypingEvent>(event.payload)
            if (typing.userId != senderUid) return
            val chatId = UUID.fromString(typing.chatId)
            if (!chatRepository.isUserInChat(chatId, senderUid)) return
            val members = chatRepository.getChatMembers(chatId).map { it.userId }
                .filter { it != senderUid }
            wsManager.sendToUsers(members, event)
        }

        WsEventType.CALL_OFFER, WsEventType.CALL_ANSWER, WsEventType.CALL_ICE, WsEventType.CALL_END -> {
            val signal = Json.decodeFromString<CallSignal>(event.payload)
            if (signal.fromUserId == senderUid) {
                wsManager.sendToUser(signal.toUserId, event)
            }
        }

        WsEventType.MESSAGE_READ -> {
            val readEvent = Json.decodeFromString<ReadEvent>(event.payload)
            messageRepository.markAsRead(UUID.fromString(readEvent.messageId), senderUid)
        }

        else -> logger.debug("Unknown WS event type: ${event.type}")
    }
}
