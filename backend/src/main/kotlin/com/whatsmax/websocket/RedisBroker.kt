/** Кросс-нодная доставка WS-событий через Redis pub/sub. */
package com.whatsmax.websocket

import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import org.slf4j.LoggerFactory

class RedisBroker(redisUrl: String) {

    private val logger = LoggerFactory.getLogger(RedisBroker::class.java)
    private val client: RedisClient = RedisClient.create(redisUrl)
    private val publishConn: StatefulRedisPubSubConnection<String, String> = client.connectPubSub()
    private val subscribeConn: StatefulRedisPubSubConnection<String, String> = client.connectPubSub()

    private fun userChannel(uid: String) = "ws:user:$uid"

    /** Публикует payload в Redis-канал пользователя. */
    fun publishToUser(uid: String, payload: String) {
        runCatching {
            publishConn.async().publish(userChannel(uid), payload)
        }.onFailure { logger.warn("Redis publish failed for $uid: ${it.message}") }
    }

    /** Подписывает handler на события для uid, возвращает функцию отписки. */
    fun subscribeForUser(uid: String, onMessage: (String) -> Unit): () -> Unit {
        val pattern = userChannel(uid)
        val listener = object : RedisPubSubAdapter<String, String>() {
            override fun message(channel: String, message: String) {
                if (channel == pattern) onMessage(message)
            }
        }
        subscribeConn.addListener(listener)
        subscribeConn.async().subscribe(pattern)
        return {
            runCatching {
                subscribeConn.async().unsubscribe(pattern)
                subscribeConn.removeListener(listener)
            }
        }
    }

    /** Закрытие при shutdown сервера. */
    fun close() {
        runCatching { publishConn.close() }
        runCatching { subscribeConn.close() }
        runCatching { client.shutdown() }
    }
}
