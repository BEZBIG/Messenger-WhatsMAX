/** Маршруты Telegram-подобных каналов. */
package com.whatsmax.routes

import com.whatsmax.domain.models.*
import com.whatsmax.domain.repositories.ChannelRepository
import com.whatsmax.domain.repositories.FileRepository
import com.whatsmax.domain.repositories.UserRepository
import com.whatsmax.plugins.FirebasePrincipal
import com.whatsmax.utils.StorageService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.channelRoutes(
    channelRepository: ChannelRepository,
    userRepository: UserRepository,
    fileRepository: FileRepository,
    storageService: StorageService
) {

    authenticate("firebase") {
        route("/channels") {

            post {
                val principal = call.principal<FirebasePrincipal>()!!
                val request = call.receive<CreateChannelRequest>()
                val channel = channelRepository.createChannel(request, principal.uid)
                call.respond(HttpStatusCode.Created, channel)
            }

            get("/search") {
                val query = call.request.queryParameters["q"]
                    ?: throw IllegalArgumentException("Query parameter 'q' is required")
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                call.respond(channelRepository.searchChannels(query, limit))
            }

            get("/my") {
                val principal = call.principal<FirebasePrincipal>()!!
                call.respond(channelRepository.getUserChannels(principal.uid))
            }

            get("/{id}") {
                val principal = call.principal<FirebasePrincipal>()!!
                val channelId = UUID.fromString(call.parameters["id"])
                val channel = channelRepository.getChannelById(channelId, principal.uid)
                    ?: throw NoSuchElementException("Channel not found")
                call.respond(channel)
            }

            post("/{id}/subscribe") {
                val principal = call.principal<FirebasePrincipal>()!!
                val channelId = UUID.fromString(call.parameters["id"])
                channelRepository.subscribeToChannel(channelId, principal.uid)
                call.respond(HttpStatusCode.OK, mapOf("status" to "subscribed"))
            }

            delete("/{id}/subscribe") {
                val principal = call.principal<FirebasePrincipal>()!!
                val channelId = UUID.fromString(call.parameters["id"])
                channelRepository.unsubscribeFromChannel(channelId, principal.uid)
                call.respond(HttpStatusCode.OK, mapOf("status" to "unsubscribed"))
            }

            get("/{id}/subscribers") {
                val principal = call.principal<FirebasePrincipal>()!!
                val channelId = UUID.fromString(call.parameters["id"])
                val channel = channelRepository.getChannelById(channelId, principal.uid)
                    ?: throw NoSuchElementException("Channel not found")
                if (channel.ownerId != principal.uid) throw SecurityException("Only owner can view subscribers")
                call.respond(channelRepository.getChannelSubscribers(channelId))
            }

            get("/{id}/messages") {
                val channelId = UUID.fromString(call.parameters["id"])
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val messages = channelRepository.getChannelMessages(channelId, limit)
                call.respond(messages)
            }

            post("/{id}/messages/{msgId}/view") {
                val principal = call.principal<FirebasePrincipal>()!!
                val msgId = UUID.fromString(call.parameters["msgId"])
                channelRepository.registerMessageView(msgId, principal.uid)
                call.respond(HttpStatusCode.OK)
            }

            post("/{id}/messages") {
                val principal = call.principal<FirebasePrincipal>()!!
                val channelId = UUID.fromString(call.parameters["id"])
                val channel = channelRepository.getChannelById(channelId, principal.uid)
                    ?: throw NoSuchElementException("Channel not found")
                if (channel.ownerId != principal.uid)
                    throw SecurityException("Only channel owner can post")
                val request = call.receive<SendMessageRequest>()
                val message = channelRepository.postMessage(channelId, principal.uid, request)
                call.respond(HttpStatusCode.Created, message)
            }

            get("/{id}/messages/{msgId}/comments") {
                val channelId = UUID.fromString(call.parameters["id"])
                val msgId     = UUID.fromString(call.parameters["msgId"])
                val actualChannelId = channelRepository.getChannelIdOfPost(msgId)
                    ?: throw NoSuchElementException("Post not found")
                if (actualChannelId != channelId)
                    throw NoSuchElementException("Post not found in this channel")
                call.respond(channelRepository.getComments(msgId))
            }

            post("/{id}/messages/{msgId}/comments") {
                val principal = call.principal<FirebasePrincipal>()!!
                val channelId = UUID.fromString(call.parameters["id"])
                val msgId     = UUID.fromString(call.parameters["msgId"])
                val actualChannelId = channelRepository.getChannelIdOfPost(msgId)
                    ?: throw NoSuchElementException("Post not found")
                if (actualChannelId != channelId)
                    throw NoSuchElementException("Post not found in this channel")
                val request = call.receive<PostCommentRequest>()
                val comment = channelRepository.addComment(msgId, principal.uid, request.content)
                call.respond(HttpStatusCode.Created, comment)
            }

            delete("/{id}") {
                val principal = call.principal<FirebasePrincipal>()!!
                val channelId = UUID.fromString(call.parameters["id"])
                val channel = channelRepository.getChannelById(channelId, principal.uid)
                    ?: throw NoSuchElementException("Channel not found")
                if (channel.ownerId != principal.uid) throw SecurityException("Only owner can delete channel")

                val orphanCandidates = channelRepository.deleteChannel(channelId)
                orphanCandidates.forEach { fileId ->
                    if (fileRepository.isOrphan(fileId)) {
                        fileRepository.getMeta(fileId)?.let { meta ->
                            meta.objectKey?.let      { storageService.delete(it) }
                            meta.thumbObjectKey?.let { storageService.delete(it) }
                        }
                        fileRepository.deleteFile(fileId)
                    }
                }
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
