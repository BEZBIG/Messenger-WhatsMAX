/** Реализация ChannelRepository для Telegram-подобных каналов. */
package com.whatsmax.data.repositories

import com.whatsmax.data.database.DatabaseFactory.dbQuery
import com.whatsmax.data.database.tables.*
import com.whatsmax.domain.models.*
import com.whatsmax.domain.repositories.ChannelRepository
import com.whatsmax.utils.Validation
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import java.time.LocalDateTime
import java.util.UUID

class ChannelRepositoryImpl : ChannelRepository {

    private fun parseWaveform(s: String?): List<Int>? =
        s?.let { runCatching { Json.decodeFromString<List<Int>>(it) }.getOrNull() }

    private fun encodeWaveform(w: List<Int>?): String? =
        w?.let { Json.encodeToString(it) }

    private fun ResultRow.toChannel(isSubscribed: Boolean = false) = Channel(
        id           = this[ChannelsTable.id].toString(),
        handle       = this[ChannelsTable.handle],
        name         = this[ChannelsTable.name],
        description  = this[ChannelsTable.description],
        avatarUrl    = this[ChannelsTable.avatarUrl],
        isPublic     = this[ChannelsTable.isPublic],
        ownerId      = this[ChannelsTable.ownerId],
        membersCount = this[ChannelsTable.membersCount],
        isSubscribed = isSubscribed,
        createdAt    = this[ChannelsTable.createdAt].toString()
    )

    private fun ResultRow.toChannelMessage(
        commentsCount: Int = 0, authorName: String = "",
        fileUrl: String? = null, thumbUrl: String? = null
    ) = ChannelMessage(
        id            = this[ChannelMessagesTable.id].toString(),
        channelId     = this[ChannelMessagesTable.channelId].toString(),
        authorId      = this[ChannelMessagesTable.authorId],
        authorName    = authorName,
        content       = this[ChannelMessagesTable.content],
        type          = this[ChannelMessagesTable.type],
        fileId        = this[ChannelMessagesTable.fileId]?.toString(),
        fileUrl       = fileUrl,
        thumbUrl      = thumbUrl,
        views         = this[ChannelMessagesTable.views],
        commentsCount = commentsCount,
        isEdited      = this[ChannelMessagesTable.isEdited],
        createdAt     = this[ChannelMessagesTable.createdAt].toString(),
        durationMs    = this[ChannelMessagesTable.durationMs],
        waveform      = parseWaveform(this[ChannelMessagesTable.waveform])
    )

    private fun fileUrls(fileId: UUID?): Pair<String?, String?> {
        if (fileId == null) return null to null
        val row = FilesTable.select { FilesTable.id eq fileId }.singleOrNull() ?: return null to null
        val url = "/files/${row[FilesTable.id]}"
        val thumb = if (row[FilesTable.thumbObjectKey] != null) "/files/${row[FilesTable.id]}/thumb" else null
        return url to thumb
    }

    override suspend fun createChannel(request: CreateChannelRequest, ownerId: String): Channel = dbQuery {
        val channelId = UUID.randomUUID()
        val now = LocalDateTime.now()
        ChannelsTable.insert {
            it[id]                    = channelId
            it[handle]                = request.handle
            it[name]                  = request.name
            it[description]           = request.description
            it[isPublic]              = request.isPublic
            it[ChannelsTable.ownerId] = ownerId
            it[membersCount]          = 1
            it[createdAt]             = now
        }
        ChannelMembersTable.insert {
            it[ChannelMembersTable.channelId] = channelId
            it[userId]                        = ownerId
            it[role]                          = "owner"
        }
        Channel(
            id           = channelId.toString(),
            handle       = request.handle,
            name         = request.name,
            description  = request.description,
            avatarUrl    = null,
            isPublic     = request.isPublic,
            ownerId      = ownerId,
            membersCount = 1,
            isSubscribed = true,
            createdAt    = now.toString()
        )
    }

    override suspend fun getChannelById(channelId: UUID, requesterId: String?): Channel? = dbQuery {
        val row = ChannelsTable.select { ChannelsTable.id eq channelId }.singleOrNull() ?: return@dbQuery null
        val isSubscribed = if (requesterId != null) {
            ChannelMembersTable.select {
                (ChannelMembersTable.channelId eq channelId) and (ChannelMembersTable.userId eq requesterId)
            }.count() > 0
        } else false
        row.toChannel(isSubscribed)
    }

    override suspend fun getChannelByHandle(handle: String): Channel? = dbQuery {
        ChannelsTable.select { ChannelsTable.handle eq handle }.singleOrNull()?.toChannel()
    }

    override suspend fun searchChannels(query: String, limit: Int): List<Channel> = dbQuery {
        val escaped = Validation.escapeLike(query)
        ChannelsTable.select {
            ((ChannelsTable.name like "%$escaped%") or (ChannelsTable.handle like "%$escaped%")) and
            (ChannelsTable.isPublic eq true)
        }.limit(limit).map { it.toChannel() }
    }

    override suspend fun getUserChannels(uid: String): List<Channel> = dbQuery {
        val channelIds = ChannelMembersTable.select { ChannelMembersTable.userId eq uid }
            .map { it[ChannelMembersTable.channelId] }
        if (channelIds.isEmpty()) return@dbQuery emptyList<Channel>()
        ChannelsTable.select { ChannelsTable.id inList channelIds }
            .map { it.toChannel(isSubscribed = true) }
    }

    override suspend fun subscribeToChannel(channelId: UUID, userId: String) = dbQuery {
        val exists = ChannelMembersTable.select {
            (ChannelMembersTable.channelId eq channelId) and (ChannelMembersTable.userId eq userId)
        }.count() > 0
        if (!exists) {
            ChannelMembersTable.insert {
                it[ChannelMembersTable.channelId] = channelId
                it[ChannelMembersTable.userId]    = userId
                it[role]                          = "subscriber"
            }
            ChannelsTable.update({ ChannelsTable.id eq channelId }) {
                with(SqlExpressionBuilder) { it.update(membersCount, membersCount + 1) }
            }
        }
        Unit
    }

    override suspend fun unsubscribeFromChannel(channelId: UUID, userId: String) = dbQuery {
        ChannelMembersTable.deleteWhere {
            (ChannelMembersTable.channelId eq channelId) and (ChannelMembersTable.userId eq userId)
        }
        ChannelsTable.update({ ChannelsTable.id eq channelId }) {
            with(SqlExpressionBuilder) { it.update(membersCount, membersCount - 1) }
        }
        Unit
    }

    override suspend fun getChannelSubscribers(channelId: UUID): List<User> = dbQuery {
        (ChannelMembersTable innerJoin UsersTable)
            .select { ChannelMembersTable.channelId eq channelId }
            .map { u ->
                User(
                    uid         = u[UsersTable.uid],
                    username    = u[UsersTable.username],
                    email       = u[UsersTable.email],
                    displayName = u[UsersTable.displayName],
                    avatarUrl   = u[UsersTable.avatarUrl],
                    bio         = u[UsersTable.bio],
                    isOnline    = u[UsersTable.isOnline],
                    lastSeen    = u[UsersTable.lastSeen]?.toString() ?: "",
                    createdAt   = u[UsersTable.createdAt].toString()
                )
            }
    }

    override suspend fun postMessage(channelId: UUID, authorId: String, request: SendMessageRequest): ChannelMessage = dbQuery {
        val msgId = UUID.randomUUID()
        ChannelMessagesTable.insert {
            it[id]                             = msgId
            it[ChannelMessagesTable.channelId] = channelId
            it[ChannelMessagesTable.authorId]  = authorId
            it[content]                        = request.content
            it[type]                           = request.type
            it[fileId]                         = request.fileId?.let { UUID.fromString(it) }
            it[durationMs]                     = request.durationMs
            it[waveform]                       = encodeWaveform(request.waveform)
        }
        val authorName = UsersTable.select { UsersTable.uid eq authorId }.singleOrNull()
            ?.get(UsersTable.displayName) ?: "Unknown"
        val (fileUrl, thumbUrl) = fileUrls(request.fileId?.let { UUID.fromString(it) })
        val row = ChannelMessagesTable.select { ChannelMessagesTable.id eq msgId }.single()
        row.toChannelMessage(commentsCount = 0, authorName = authorName, fileUrl = fileUrl, thumbUrl = thumbUrl)
    }

    override suspend fun getChannelMessages(channelId: UUID, limit: Int, before: UUID?): List<ChannelMessage> = dbQuery {
        val rows = ChannelMessagesTable.select {
            (ChannelMessagesTable.channelId eq channelId) and (ChannelMessagesTable.isDeleted eq false)
        }.orderBy(ChannelMessagesTable.createdAt, SortOrder.DESC).limit(limit).toList()
        if (rows.isEmpty()) return@dbQuery emptyList<ChannelMessage>()

        val msgIds    = rows.map { it[ChannelMessagesTable.id] }
        val authorIds = rows.map { it[ChannelMessagesTable.authorId] }.distinct()
        val fileIds   = rows.mapNotNull { it[ChannelMessagesTable.fileId] }.distinct()

        val authorNames: Map<String, String> = UsersTable
            .slice(UsersTable.uid, UsersTable.displayName)
            .select { UsersTable.uid inList authorIds }
            .associate { it[UsersTable.uid] to it[UsersTable.displayName] }

        val files: Map<UUID, ResultRow> = if (fileIds.isEmpty()) emptyMap()
                                          else FilesTable.select { FilesTable.id inList fileIds }
                                                  .associateBy { it[FilesTable.id] }

        val cntExpr = ChannelCommentsTable.id.count()
        val commentCounts: Map<UUID, Int> = ChannelCommentsTable
            .slice(ChannelCommentsTable.messageId, cntExpr)
            .select { ChannelCommentsTable.messageId inList msgIds }
            .groupBy(ChannelCommentsTable.messageId)
            .associate { it[ChannelCommentsTable.messageId] to it[cntExpr].toInt() }

        rows.map { row ->
            val authorName = authorNames[row[ChannelMessagesTable.authorId]] ?: "Unknown"
            val commentsCount = commentCounts[row[ChannelMessagesTable.id]] ?: 0
            val fileRow = row[ChannelMessagesTable.fileId]?.let { files[it] }
            val fileUrl  = fileRow?.let { "/files/${it[FilesTable.id]}" }
            val thumbUrl = fileRow?.takeIf { it[FilesTable.thumbObjectKey] != null }
                ?.let { "/files/${it[FilesTable.id]}/thumb" }
            row.toChannelMessage(commentsCount = commentsCount, authorName = authorName, fileUrl = fileUrl, thumbUrl = thumbUrl)
        }.reversed()
    }

    override suspend fun registerMessageView(messageId: UUID, userId: String) = dbQuery {
        val inserted = runCatching {
            ChannelMessageViewsTable.insert {
                it[ChannelMessageViewsTable.messageId] = messageId
                it[ChannelMessageViewsTable.userId]    = userId
            }
            true
        }.getOrDefault(false)
        if (inserted) {
            ChannelMessagesTable.update({ ChannelMessagesTable.id eq messageId }) {
                with(SqlExpressionBuilder) { it.update(views, views + 1) }
            }
        }
        Unit
    }

    override suspend fun deleteChannel(channelId: UUID): List<UUID> = dbQuery {
        val msgIds = ChannelMessagesTable.select { ChannelMessagesTable.channelId eq channelId }
            .map { it[ChannelMessagesTable.id] }
        val fileIds = ChannelMessagesTable.select { ChannelMessagesTable.channelId eq channelId }
            .mapNotNull { it[ChannelMessagesTable.fileId] }
        if (msgIds.isNotEmpty()) {
            ChannelCommentsTable.deleteWhere { ChannelCommentsTable.messageId inList msgIds }
            ChannelMessageViewsTable.deleteWhere { ChannelMessageViewsTable.messageId inList msgIds }
        }
        ChannelMessagesTable.deleteWhere { ChannelMessagesTable.channelId eq channelId }
        ChannelMembersTable.deleteWhere { ChannelMembersTable.channelId eq channelId }
        ChannelsTable.deleteWhere { ChannelsTable.id eq channelId }
        fileIds
    }

    override suspend fun getComments(messageId: UUID): List<ChannelComment> = dbQuery {
        (ChannelCommentsTable innerJoin UsersTable)
            .select { ChannelCommentsTable.messageId eq messageId }
            .orderBy(ChannelCommentsTable.createdAt, SortOrder.ASC)
            .map { row ->
                ChannelComment(
                    id           = row[ChannelCommentsTable.id].toString(),
                    messageId    = row[ChannelCommentsTable.messageId].toString(),
                    authorId     = row[ChannelCommentsTable.authorId],
                    authorName   = row[UsersTable.displayName],
                    authorAvatar = row[UsersTable.avatarUrl],
                    content      = row[ChannelCommentsTable.content],
                    createdAt    = row[ChannelCommentsTable.createdAt].toString()
                )
            }
    }

    override suspend fun getChannelIdOfPost(postId: UUID): UUID? = dbQuery {
        ChannelMessagesTable.slice(ChannelMessagesTable.channelId)
            .select { ChannelMessagesTable.id eq postId }
            .singleOrNull()?.get(ChannelMessagesTable.channelId)
    }

    override suspend fun getChannelIdOfComment(commentId: UUID): UUID? = dbQuery {
        val postId = ChannelCommentsTable.slice(ChannelCommentsTable.messageId)
            .select { ChannelCommentsTable.id eq commentId }
            .singleOrNull()?.get(ChannelCommentsTable.messageId) ?: return@dbQuery null
        ChannelMessagesTable.slice(ChannelMessagesTable.channelId)
            .select { ChannelMessagesTable.id eq postId }
            .singleOrNull()?.get(ChannelMessagesTable.channelId)
    }

    override suspend fun canViewChannel(channelId: UUID, userId: String): Boolean = dbQuery {
        val row = ChannelsTable.select { ChannelsTable.id eq channelId }.singleOrNull() ?: return@dbQuery false
        if (row[ChannelsTable.isPublic]) return@dbQuery true
        ChannelMembersTable.select {
            (ChannelMembersTable.channelId eq channelId) and (ChannelMembersTable.userId eq userId)
        }.count() > 0
    }

    override suspend fun addComment(messageId: UUID, authorId: String, content: String): ChannelComment = dbQuery {
        val commentId = UUID.randomUUID()
        val now = LocalDateTime.now()
        ChannelCommentsTable.insert {
            it[id]                           = commentId
            it[ChannelCommentsTable.messageId] = messageId
            it[ChannelCommentsTable.authorId]  = authorId
            it[ChannelCommentsTable.content]   = content
            it[createdAt]                    = now
        }
        val author = UsersTable.select { UsersTable.uid eq authorId }.singleOrNull()
        ChannelComment(
            id          = commentId.toString(),
            messageId   = messageId.toString(),
            authorId    = authorId,
            authorName  = author?.get(UsersTable.displayName) ?: "Unknown",
            authorAvatar = author?.get(UsersTable.avatarUrl),
            content     = content,
            createdAt   = now.toString()
        )
    }
}
