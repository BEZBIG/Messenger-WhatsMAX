/** DTO сетевого слоя с маппингом в доменные модели. */
package com.whatsmax.data.remote.dto

import com.whatsmax.domain.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val uid: String = "",
    val username: String = "",
    val email: String? = null,
    val phone: String? = null,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false,
    @SerialName("last_seen") val lastSeen: String = "",
    @SerialName("created_at") val createdAt: String = ""
) {
    fun toModel() = User(uid, username, email, phone, displayName, avatarUrl, bio, isOnline, lastSeen)
}

@Serializable
data class CreateUserRequest(
    val username: String,
    @SerialName("display_name") val displayName: String,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("fcm_token") val fcmToken: String? = null
)

@Serializable
data class UpdateUserRequest(
    @SerialName("display_name") val displayName: String? = null,
    val bio: String? = null,
    val username: String? = null,
    @SerialName("fcm_token") val fcmToken: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class ChatDto(
    val id: String = "",
    val type: String = "direct",
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val description: String? = null,
    @SerialName("created_by") val createdBy: String = "",
    val members: List<ChatMemberDto> = emptyList(),
    @SerialName("last_message") val lastMessage: MessageDto? = null,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
) {
    fun toModel() = Chat(
        id          = id,
        type        = if (type == "group") ChatType.GROUP else ChatType.DIRECT,
        name        = name,
        avatarUrl   = avatarUrl,
        description = description,
        createdBy   = createdBy,
        members     = members.map { it.toModel() },
        lastMessage = lastMessage?.toModel(),
        unreadCount = unreadCount,
        createdAt   = createdAt,
        updatedAt   = updatedAt
    )
}

@Serializable
data class ChatMemberDto(
    @SerialName("user_id") val userId: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val role: String = "member",
    @SerialName("is_online") val isOnline: Boolean = false,
    @SerialName("joined_at") val joinedAt: String = ""
) {
    fun toModel() = ChatMember(userId, displayName, avatarUrl, role, isOnline)
}

@Serializable
data class CreateChatRequest(
    val type: String,
    @SerialName("member_uids") val memberUids: List<String>,
    val name: String? = null,
    val description: String? = null
)

@Serializable
data class MessageDto(
    val id: String = "",
    @SerialName("chat_id") val chatId: String = "",
    @SerialName("sender_id") val senderId: String = "",
    @SerialName("sender_name") val senderName: String = "",
    @SerialName("sender_avatar") val senderAvatar: String? = null,
    val content: String? = null,
    val type: String = "text",
    @SerialName("file_id") val fileId: String? = null,
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("thumb_url") val thumbUrl: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("read_by") val readBy: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("duration_ms") val durationMs: Long? = null,
    val waveform: List<Int>? = null
) {
    fun toModel() = Message(
        id = id, chatId = chatId, senderId = senderId, senderName = senderName,
        senderAvatar = senderAvatar, content = content,
        type = when(type) {
            "image" -> MessageType.IMAGE; "file" -> MessageType.FILE
            "audio" -> MessageType.AUDIO; "video" -> MessageType.VIDEO
            "voice" -> MessageType.VOICE; "call" -> MessageType.CALL
            else    -> MessageType.TEXT
        },
        fileId = fileId, fileUrl = fileUrl, fileName = fileName, thumbUrl = thumbUrl,
        replyToId = replyToId, isEdited = isEdited, isDeleted = isDeleted,
        readBy = readBy, createdAt = createdAt, editedAt = editedAt,
        durationMs = durationMs, waveform = waveform
    )
}

@Serializable
data class SendMessageRequest(
    val content: String? = null,
    val type: String = "text",
    @SerialName("file_id") val fileId: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("duration_ms") val durationMs: Long? = null,
    val waveform: List<Int>? = null
)

@Serializable
data class EditMessageRequest(val content: String)

@Serializable
data class ChannelDto(
    val id: String = "",
    val handle: String = "",
    val name: String = "",
    val description: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_public") val isPublic: Boolean = true,
    @SerialName("owner_id") val ownerId: String = "",
    @SerialName("members_count") val membersCount: Int = 0,
    @SerialName("is_subscribed") val isSubscribed: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
) {
    fun toModel() = Channel(id, handle, name, description, avatarUrl, isPublic, ownerId, membersCount, isSubscribed, createdAt)
}

@Serializable
data class ChannelMessageDto(
    val id: String = "",
    @SerialName("channel_id") val channelId: String = "",
    @SerialName("author_id") val authorId: String = "",
    @SerialName("author_name") val authorName: String = "",
    val content: String? = null,
    val type: String = "text",
    @SerialName("file_id") val fileId: String? = null,
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("thumb_url") val thumbUrl: String? = null,
    val views: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("duration_ms") val durationMs: Long? = null,
    val waveform: List<Int>? = null
) {
    fun toModel() = ChannelMessage(
        id = id, channelId = channelId, authorId = authorId, authorName = authorName,
        content = content,
        type = when(type) {
            "image" -> MessageType.IMAGE; "file" -> MessageType.FILE
            "audio" -> MessageType.AUDIO; "video" -> MessageType.VIDEO
            "voice" -> MessageType.VOICE; else  -> MessageType.TEXT
        },
        fileId = fileId, fileUrl = fileUrl, thumbUrl = thumbUrl, views = views,
        commentsCount = commentsCount, isEdited = isEdited, createdAt = createdAt,
        durationMs = durationMs, waveform = waveform
    )
}

@Serializable
data class ChannelCommentDto(
    val id: String = "",
    @SerialName("message_id") val messageId: String = "",
    @SerialName("author_id") val authorId: String = "",
    @SerialName("author_name") val authorName: String = "",
    @SerialName("author_avatar") val authorAvatar: String? = null,
    val content: String = "",
    @SerialName("created_at") val createdAt: String = ""
) {
    fun toModel() = ChannelComment(id, messageId, authorId, authorName, authorAvatar, content, createdAt)
}

@Serializable
data class PostCommentRequest(val content: String)

@Serializable
data class CreateChannelRequest(
    val handle: String,
    val name: String,
    val description: String? = null,
    @SerialName("is_public") val isPublic: Boolean = true
)

@Serializable
data class UpdateChannelRequest(
    val name: String? = null,
    val description: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class FileInfoDto(
    val id: String = "",
    @SerialName("original_name") val originalName: String = "",
    @SerialName("mime_type") val mimeType: String = "",
    @SerialName("size_bytes") val sizeBytes: Long = 0,
    val url: String = "",
    @SerialName("uploaded_at") val uploadedAt: String = "",
    @SerialName("thumb_url") val thumbUrl: String? = null
) {
    fun toModel() = FileInfo(id, originalName, mimeType, sizeBytes, url, uploadedAt, thumbUrl)
}

@Serializable
data class ReactionRequest(val emoji: String)

@Serializable
data class ReactionSummaryDto(
    val reactions: Map<String, Int> = emptyMap(),
    @SerialName("my_reaction") val myReaction: String? = null
)

@Serializable
data class WsEventDto(val type: String, val payload: String) {
    fun toModel() = WsEvent(type, payload)
}

@Serializable
data class CallSignalDto(
    @SerialName("call_id") val callId: String,
    @SerialName("from_user_id") val fromUserId: String,
    @SerialName("to_user_id") val toUserId: String,
    @SerialName("is_video") val isVideo: Boolean = false,
    val sdp: String? = null,
    val candidate: String? = null
)
