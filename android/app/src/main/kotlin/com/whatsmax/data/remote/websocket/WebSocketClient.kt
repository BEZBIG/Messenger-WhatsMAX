/**
 * data/remote/websocket/WebSocketClient.kt
 * OkHttp WebSocket-клиент для real-time связи с Ktor-сервером:
 * авто-реконнект и трансляция входящих событий в SharedFlow.
 */
package com.whatsmax.data.remote.websocket

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.whatsmax.data.remote.dto.WsEventDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val wsBaseUrl: String
) {
    private val TAG = "WebSocketClient"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var webSocket: WebSocket? = null
    private var isConnected = false

    private val _events = MutableSharedFlow<WsEventDto>(extraBufferCapacity = 64)
    val events: SharedFlow<WsEventDto> = _events

    /** Подключиться к WebSocket. Токен отправляется первым кадром после handshake,
     *  а не как query-параметр (защита от логирования токена в прокси/CDN). */
    fun connect() {
        scope.launch {
            try {
                val token = FirebaseAuth.getInstance().currentUser
                    ?.getIdToken(false)?.await()?.token ?: return@launch
                val request = Request.Builder().url(wsBaseUrl).build()
                webSocket = okHttpClient.newWebSocket(request, createListener(token))
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                scheduleReconnect()
            }
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
    }

    fun sendEvent(event: WsEventDto) {
        if (!isConnected) { Log.w(TAG, "WS not connected, cannot send event"); return }
        val json = Json.encodeToString(event)
        webSocket?.send(json)
    }

    fun isConnected() = isConnected

    private fun createListener(authToken: String) = object : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            isConnected = true
            // Отправляем auth-кадр сразу после handshake — серверный webSocketRoutes
            // ждёт его в течение 5 секунд, иначе закрывает соединение.
            val authJson = Json.encodeToString(WsEventDto(type = "auth", payload = authToken))
            ws.send(authJson)
            Log.i(TAG, "WebSocket connected, auth frame sent")
        }

        override fun onMessage(ws: WebSocket, text: String) {
            try {
                val event = Json.decodeFromString<WsEventDto>(text)
                scope.launch { _events.emit(event) }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse WS message: $text")
            }
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            ws.close(1000, null)
            isConnected = false
            Log.i(TAG, "WebSocket closing: $reason")
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            isConnected = false
            Log.i(TAG, "WebSocket closed: $reason")
            if (code != 1000) scheduleReconnect()  // Переподключение при неожиданном закрытии
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            isConnected = false
            Log.e(TAG, "WebSocket failure", t)
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        scope.launch {
            delay(3_000L)  // Ждём 3 секунды перед переподключением
            if (!isConnected) connect()
        }
    }
}
