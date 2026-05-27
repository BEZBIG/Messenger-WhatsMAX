/** Реестр активных WS-сессий с адресной и broadcast-доставкой. */
package com.whatsmax.websocket

import com.whatsmax.domain.models.WsEvent
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class WebSocketManager(private val broker: RedisBroker? = null) {

    private val logger = LoggerFactory.getLogger(WebSocketManager::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sessions = ConcurrentHashMap<String, MutableList<DefaultWebSocketServerSession>>()
    private val mutex = Mutex()
    private val redisUnsubs = ConcurrentHashMap<String, () -> Unit>()

    suspend fun addSession(uid: String, session: DefaultWebSocketServerSession) {
        val firstSession = mutex.withLock {
            val list = sessions.getOrPut(uid) { mutableListOf() }
            list.add(session)
            list.size == 1
        }
        if (firstSession && broker != null) {
            redisUnsubs[uid] = broker.subscribeForUser(uid) { json ->
                scope.launch { deliverLocally(uid, json) }
            }
        }
        logger.info("User $uid connected. Local sessions: ${sessions.size}")
    }

    suspend fun removeSession(uid: String, session: DefaultWebSocketServerSession) {
        val noMoreSessions = mutex.withLock {
            sessions[uid]?.remove(session)
            val empty = sessions[uid]?.isEmpty() == true
            if (empty) sessions.remove(uid)
            empty
        }
        if (noMoreSessions) {
            redisUnsubs.remove(uid)?.invoke()
        }
        logger.info("User $uid disconnected. Local sessions: ${sessions.size}")
    }

    suspend fun sendToUser(uid: String, event: WsEvent) {
        val json = Json.encodeToString(event)
        if (broker != null) {
            broker.publishToUser(uid, json)
        } else {
            deliverLocally(uid, json)
        }
    }

    private suspend fun deliverLocally(uid: String, json: String) {
        val userSessions = mutex.withLock { sessions[uid]?.toList() } ?: return
        userSessions.forEach { session ->
            try { session.send(Frame.Text(json)) }
            catch (e: Exception) { logger.warn("Failed to send to user $uid: ${e.message}") }
        }
    }

    suspend fun sendToUsers(uids: List<String>, event: WsEvent) = coroutineScope {
        uids.map { uid -> async { sendToUser(uid, event) } }.awaitAll()
        Unit
    }

    fun isOnline(uid: String): Boolean = sessions.containsKey(uid)

    fun getOnlineUsers(uids: List<String>): List<String> = uids.filter { isOnline(it) }

    fun onlineCount(): Int = sessions.size
}
