/** Use Cases каналов: создание, поиск, подписка, посты, комментарии. */
package com.whatsmax.domain.usecase.channel

import com.whatsmax.domain.model.Channel
import com.whatsmax.domain.model.ChannelMessage
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.repository.ChannelRepository
import javax.inject.Inject

class GetMyChannelsUseCase @Inject constructor(private val repo: ChannelRepository) {
    suspend operator fun invoke(): Result<List<Channel>> = repo.getMyChannels()
}

class SearchChannelsUseCase @Inject constructor(private val repo: ChannelRepository) {
    suspend operator fun invoke(query: String): Result<List<Channel>> = repo.searchChannels(query)
}

class CreateChannelUseCase @Inject constructor(private val repo: ChannelRepository) {
    suspend operator fun invoke(handle: String, name: String, description: String?, isPublic: Boolean): Result<Channel> =
        repo.createChannel(handle, name, description, isPublic)
}

class SubscribeToChannelUseCase @Inject constructor(private val repo: ChannelRepository) {
    suspend operator fun invoke(channelId: String): Result<Unit> = repo.subscribeToChannel(channelId)
}

class UnsubscribeFromChannelUseCase @Inject constructor(private val repo: ChannelRepository) {
    suspend operator fun invoke(channelId: String): Result<Unit> = repo.unsubscribeFromChannel(channelId)
}

class GetChannelMessagesUseCase @Inject constructor(private val repo: ChannelRepository) {
    suspend operator fun invoke(channelId: String, limit: Int = 50): Result<List<ChannelMessage>> =
        repo.getChannelMessages(channelId, limit)
}

class PostToChannelUseCase @Inject constructor(private val repo: ChannelRepository) {
    suspend operator fun invoke(
        channelId: String, content: String?, type: String = "text",
        fileId: String? = null, durationMs: Long? = null, waveform: List<Int>? = null
    ): Result<ChannelMessage> =
        repo.postToChannel(channelId, content, type, fileId, durationMs, waveform)
}

class GetChannelByIdUseCase @Inject constructor(private val repo: ChannelRepository) {
    suspend operator fun invoke(channelId: String): Result<Channel> =
        repo.getChannelById(channelId)
}

class UpdateChannelUseCase @Inject constructor(private val repo: ChannelRepository) {
    suspend operator fun invoke(channelId: String, name: String?, description: String?): Result<Channel> =
        repo.updateChannel(channelId, name, description)
}

class DeleteChannelUseCase @Inject constructor(private val repo: ChannelRepository) {
    suspend operator fun invoke(channelId: String): Result<Unit> = repo.deleteChannel(channelId)
}

class GetChannelSubscribersUseCase @Inject constructor(private val repo: ChannelRepository) {
    suspend operator fun invoke(channelId: String): Result<List<com.whatsmax.domain.model.User>> =
        repo.getChannelSubscribers(channelId)
}

class GetCommentsUseCase @Inject constructor(private val repo: ChannelRepository) {
    suspend operator fun invoke(channelId: String, messageId: String): Result<List<com.whatsmax.domain.model.ChannelComment>> =
        repo.getComments(channelId, messageId)
}

class AddCommentUseCase @Inject constructor(private val repo: ChannelRepository) {
    suspend operator fun invoke(channelId: String, messageId: String, content: String): Result<com.whatsmax.domain.model.ChannelComment> =
        repo.addComment(channelId, messageId, content)
}
