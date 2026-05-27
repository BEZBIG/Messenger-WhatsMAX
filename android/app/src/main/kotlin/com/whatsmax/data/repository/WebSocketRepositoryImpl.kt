/**
 * data/repository/WebSocketRepositoryImpl.kt
 * Реализация WebSocketRepository — обёртка над WebSocketClient для domain-слоя.
 */
package com.whatsmax.data.repository

import com.whatsmax.data.remote.dto.WsEventDto
import com.whatsmax.data.remote.websocket.WebSocketClient
import com.whatsmax.domain.model.WsEvent
import com.whatsmax.domain.repository.WebSocketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketRepositoryImpl @Inject constructor(
    private val wsClient: WebSocketClient
) : WebSocketRepository {

    override fun connect(token: String) = wsClient.connect()
    override fun disconnect() = wsClient.disconnect()

    override fun sendEvent(event: WsEvent) {
        wsClient.sendEvent(WsEventDto(event.type, event.payload))
    }

    override fun observeEvents(): Flow<WsEvent> =
        wsClient.events.map { WsEvent(it.type, it.payload) }

    override fun isConnected(): Boolean = wsClient.isConnected()
}
