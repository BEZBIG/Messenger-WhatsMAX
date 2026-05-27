/** Сборка зависимостей и регистрация HTTP/WS маршрутов. */
package com.whatsmax.plugins

import com.whatsmax.data.repositories.*
import com.whatsmax.routes.*
import com.whatsmax.utils.FirebaseAdmin
import com.whatsmax.utils.StorageConfig
import com.whatsmax.utils.StorageService
import com.whatsmax.websocket.RedisBroker
import com.whatsmax.websocket.WebSocketManager
import io.ktor.server.application.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(DefaultHeaders) {
        header("X-App-Name", "WhatsMAX-Backend")
    }

    val firebaseConfigPath = environment.config
        .config("firebase")
        .property("serviceAccountPath")
        .getString()
    FirebaseAdmin.initialize(firebaseConfigPath)

    val userRepository     = UserRepositoryImpl()
    val chatRepository     = ChatRepositoryImpl()
    val messageRepository  = MessageRepositoryImpl()
    val channelRepository  = ChannelRepositoryImpl()
    val fileRepository     = FileRepositoryImpl()
    val reactionRepository = com.whatsmax.data.repositories.ReactionRepositoryImpl()

    val redisCfg = environment.config.config("redis")
    val redisEnabled = redisCfg.property("enabled").getString().toBoolean()
    val redisBroker: RedisBroker? = if (redisEnabled) {
        RedisBroker(redisCfg.property("url").getString()).also {
            log.info("Redis pub/sub broker enabled — multi-instance mode")
        }
    } else null

    val wsManager = WebSocketManager(redisBroker)

    val uploadCfg = environment.config.config("upload")
    val uploadPath = uploadCfg.property("path").getString()
    val maxFileSizeBytes = uploadCfg.property("maxFileSizeMb").getString().toLong() * 1024L * 1024L

    val storageCfg = environment.config.config("storage").let {
        val endpoint = it.property("endpoint").getString()
        StorageConfig(
            endpoint  = endpoint,
            publicEndpoint = runCatching { it.property("publicEndpoint").getString() }
                .getOrDefault(endpoint),
            accessKey = it.property("accessKey").getString(),
            secretKey = it.property("secretKey").getString(),
            bucket    = it.property("bucket").getString(),
            presignedTtlSeconds = it.property("presignedTtlSeconds").getString().toInt()
        )
    }
    val storageService = StorageService(storageCfg)

    routing {
        authRoutes(userRepository)
        userRoutes(userRepository)
        chatRoutes(chatRepository, userRepository)
        messageRoutes(messageRepository, chatRepository, fileRepository, storageService, wsManager, uploadPath)
        channelRoutes(channelRepository, userRepository, fileRepository, storageService)
        fileRoutes(fileRepository, storageService, uploadPath, maxFileSizeBytes)
        reactionRoutes(reactionRepository, messageRepository, chatRepository, channelRepository)
        webSocketRoutes(wsManager, userRepository, messageRepository, chatRepository)
    }
}
