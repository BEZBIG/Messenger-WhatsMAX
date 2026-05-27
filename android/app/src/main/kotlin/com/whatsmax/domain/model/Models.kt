/** Доменные модели приложения. */
package com.whatsmax.domain.model

data class User(
    val uid: String,
    val username: String,
    val email: String? = null,
    val phone: String? = null,
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: String = ""
)

data class Chat(
    val id: String,
    val type: ChatType,
    val name: String? = null,
    val avatarUrl: String? = null,
    val description: String? = null,
    val createdBy: String = "",
    val members: List<ChatMember> = emptyList(),
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)

enum class ChatType { DIRECT, GROUP }

data class ChatMember(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val role: String = "member",
    val isOnline: Boolean = false
)

data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String? = null,
    val content: String? = null,
    val type: MessageType = MessageType.TEXT,
    val fileId: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val thumbUrl: String? = null,
    val replyToId: String? = null,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val readBy: List<String> = emptyList(),
    val createdAt: String = "",
    val editedAt: String? = null,
    val durationMs: Long? = null,
    val waveform: List<Int>? = null
)

enum class MessageType { TEXT, IMAGE, FILE, AUDIO, VIDEO, VOICE, CALL }

data class Channel(
    val id: String,
    val handle: String,
    val name: String,
    val description: String? = null,
    val avatarUrl: String? = null,
    val isPublic: Boolean = true,
    val ownerId: String = "",
    val membersCount: Int = 0,
    val isSubscribed: Boolean = false,
    val createdAt: String = ""
)

data class ChannelMessage(
    val id: String,
    val channelId: String,
    val authorId: String,
    val authorName: String,
    val content: String? = null,
    val type: MessageType = MessageType.TEXT,
    val fileId: String? = null,
    val fileUrl: String? = null,
    val thumbUrl: String? = null,
    val views: Int = 0,
    val commentsCount: Int = 0,
    val isEdited: Boolean = false,
    val createdAt: String = "",
    val durationMs: Long? = null,
    val waveform: List<Int>? = null
)

data class ChannelComment(
    val id: String,
    val messageId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String? = null,
    val content: String,
    val createdAt: String = ""
)

data class FileInfo(
    val id: String,
    val originalName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val url: String,
    val uploadedAt: String = "",
    val thumbUrl: String? = null
)

data class WsEvent(val type: String, val payload: String)

data class TypingEvent(val chatId: String, val userId: String, val isTyping: Boolean)

data class CallSignal(
    val callId: String,
    val fromUserId: String,
    val toUserId: String,
    val isVideo: Boolean = false,
    val sdp: String? = null,
    val candidate: String? = null
)

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int = 0) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
