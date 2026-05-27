/** Доменные DTO для API-слоёв. */
package com.whatsmax.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String,
    val username: String,
    val email: String? = null,
    val phone: String? = null,
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: String,
    val createdAt: String
)

@Serializable
data class CreateUserRequest(
    val username: String,
    val displayName: String,
    val email: String? = null,
    val phone: String? = null,
    val fcmToken: String? = null
)

@Serializable
data class UpdateUserRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val username: String? = null,
    val fcmToken: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class Chat(
    val id: String,
    val type: String,
    val name: String? = null,
    val avatarUrl: String? = null,
    val description: String? = null,
    val createdBy: String,
    val members: List<ChatMember> = emptyList(),
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ChatMember(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val role: String,
    val isOnline: Boolean = false,
    val joinedAt: String
)

@Serializable
data class CreateChatRequest(
    val type: String,
    val memberUids: List<String>,
    val name: String? = null,
    val description: String? = null
)

@Serializable
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String? = null,
    val content: String? = null,
    val type: String = "text",
    val fileId: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val thumbUrl: String? = null,
    val replyToId: String? = null,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val readBy: List<String> = emptyList(),
    val createdAt: String,
    val editedAt: String? = null,
    val durationMs: Long? = null,
    val waveform: List<Int>? = null
)

@Serializable
data class SendMessageRequest(
    val content: String? = null,
    val type: String = "text",
    val fileId: String? = null,
    val replyToId: String? = null,
    val durationMs: Long? = null,
    val waveform: List<Int>? = null
)

@Serializable
data class EditMessageRequest(val content: String)

@Serializable
data class Channel(
    val id: String,
    val handle: String,
    val name: String,
    val description: String? = null,
    val avatarUrl: String? = null,
    val isPublic: Boolean = true,
    val ownerId: String,
    val membersCount: Int = 0,
    val isSubscribed: Boolean = false,
    val createdAt: String
)

@Serializable
data class CreateChannelRequest(
    val handle: String,
    val name: String,
    val description: String? = null,
    val isPublic: Boolean = true
)

@Serializable
data class ChannelMessage(
    val id: String,
    val channelId: String,
    val authorId: String,
    val authorName: String,
    val content: String? = null,
    val type: String = "text",
    val fileId: String? = null,
    val fileUrl: String? = null,
    val thumbUrl: String? = null,
    val views: Int = 0,
    val commentsCount: Int = 0,
    val isEdited: Boolean = false,
    val createdAt: String,
    val durationMs: Long? = null,
    val waveform: List<Int>? = null
)

@Serializable
data class ChannelComment(
    val id: String,
    val messageId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String? = null,
    val content: String,
    val createdAt: String
)

@Serializable
data class PostCommentRequest(val content: String)

@Serializable
data class FileInfo(
    val id: String,
    val originalName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val url: String,
    val uploadedAt: String,
    val thumbUrl: String? = null
)

@Serializable
data class WsEvent(
    val type: String,
    val payload: String
)

object WsEventType {
    const val NEW_MESSAGE       = "new_message"
    const val MESSAGE_EDITED    = "message_edited"
    const val MESSAGE_DELETED   = "message_deleted"
    const val MESSAGE_READ      = "message_read"
    const val USER_ONLINE       = "user_online"
    const val USER_OFFLINE      = "user_offline"
    const val USER_TYPING       = "user_typing"
    const val CALL_OFFER        = "call_offer"
    const val CALL_ANSWER       = "call_answer"
    const val CALL_ICE          = "call_ice"
    const val CALL_END          = "call_end"
    const val CHANNEL_MESSAGE   = "channel_message"
}

@Serializable
data class ReactionRequest(val emoji: String)

@Serializable
data class ReactionSummary(
    val reactions: Map<String, Int> = emptyMap(),
    val myReaction: String? = null
)

@Serializable
data class TypingEvent(val chatId: String, val userId: String, val isTyping: Boolean)

@Serializable
data class ReadEvent(val chatId: String, val messageId: String, val userId: String)

@Serializable
data class CallSignal(
    val callId: String,
    val fromUserId: String,
    val toUserId: String,
    val isVideo: Boolean = false,
    val sdp: String? = null,
    val candidate: String? = null
)
