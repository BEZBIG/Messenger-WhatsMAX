/** Реализация ChannelRepository через REST API. */
package com.whatsmax.data.repository

import com.whatsmax.data.remote.api.ApiService
import com.whatsmax.data.remote.dto.CreateChannelRequest
import com.whatsmax.data.remote.dto.PostCommentRequest
import com.whatsmax.data.remote.dto.SendMessageRequest
import com.whatsmax.data.remote.dto.UpdateChannelRequest
import com.whatsmax.domain.model.Channel
import com.whatsmax.domain.model.ChannelComment
import com.whatsmax.domain.model.ChannelMessage
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.model.User
import com.whatsmax.domain.repository.ChannelRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ChannelRepository {

    override suspend fun createChannel(
        handle: String, name: String, description: String?, isPublic: Boolean
    ): Result<Channel> = safeApiCall {
        apiService.createChannel(CreateChannelRequest(handle, name, description, isPublic))
            .bodyOrThrow().toModel()
    }

    override suspend fun searchChannels(query: String): Result<List<Channel>> = safeApiCall {
        apiService.searchChannels(query).bodyOrThrow().map { it.toModel() }
    }

    override suspend fun getMyChannels(): Result<List<Channel>> = safeApiCall {
        apiService.getMyChannels().bodyOrThrow().map { it.toModel() }
    }

    override suspend fun getChannelById(channelId: String): Result<Channel> = safeApiCall {
        apiService.getChannelById(channelId).bodyOrThrow().toModel()
    }

    override suspend fun subscribeToChannel(channelId: String): Result<Unit> = safeApiCall {
        apiService.subscribeToChannel(channelId); Unit
    }

    override suspend fun unsubscribeFromChannel(channelId: String): Result<Unit> = safeApiCall {
        apiService.unsubscribeFromChannel(channelId); Unit
    }

    override suspend fun getChannelSubscribers(channelId: String): Result<List<User>> = safeApiCall {
        apiService.getChannelSubscribers(channelId).bodyOrThrow().map { it.toModel() }
    }

    override suspend fun getChannelMessages(channelId: String, limit: Int): Result<List<ChannelMessage>> =
        safeApiCall {
            apiService.getChannelMessages(channelId, limit).bodyOrThrow().map { it.toModel() }
        }

    override suspend fun postToChannel(
        channelId: String, content: String?, type: String,
        fileId: String?, durationMs: Long?, waveform: List<Int>?
    ): Result<ChannelMessage> = safeApiCall {
        apiService.postToChannel(
            channelId,
            SendMessageRequest(
                content = content, type = type, fileId = fileId,
                durationMs = durationMs, waveform = waveform
            )
        ).bodyOrThrow().toModel()
    }

    override suspend fun updateChannel(channelId: String, name: String?, description: String?): Result<Channel> =
        safeApiCall {
            apiService.updateChannel(channelId, UpdateChannelRequest(name, description)).bodyOrThrow().toModel()
        }

    override suspend fun deleteChannel(channelId: String): Result<Unit> = safeApiCall {
        apiService.deleteChannel(channelId); Unit
    }

    override suspend fun getComments(channelId: String, messageId: String): Result<List<ChannelComment>> = safeApiCall {
        apiService.getComments(channelId, messageId).bodyOrThrow().map { it.toModel() }
    }

    override suspend fun addComment(channelId: String, messageId: String, content: String): Result<ChannelComment> =
        safeApiCall {
            apiService.addComment(channelId, messageId, PostCommentRequest(content)).bodyOrThrow().toModel()
        }
}
