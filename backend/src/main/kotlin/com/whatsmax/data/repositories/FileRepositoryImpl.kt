/** Реализация FileRepository: метаданные файлов в PostgreSQL. */
package com.whatsmax.data.repositories

import com.whatsmax.data.database.DatabaseFactory.dbQuery
import com.whatsmax.data.database.tables.ChannelMessagesTable
import com.whatsmax.data.database.tables.FilesTable
import com.whatsmax.data.database.tables.MessagesTable
import com.whatsmax.domain.models.FileInfo
import com.whatsmax.domain.repositories.FileRepository
import com.whatsmax.domain.repositories.StoredFileMeta
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID

class FileRepositoryImpl : FileRepository {

    private fun ResultRow.toFileInfo() = FileInfo(
        id           = this[FilesTable.id].toString(),
        originalName = this[FilesTable.originalName],
        mimeType     = this[FilesTable.mimeType],
        sizeBytes    = this[FilesTable.sizeBytes],
        url          = "/files/${this[FilesTable.id]}",
        uploadedAt   = this[FilesTable.uploadedAt].toString(),
        thumbUrl     = this[FilesTable.thumbObjectKey]?.let { "/files/${this[FilesTable.id]}/thumb" }
    )

    override suspend fun saveFileInfo(
        originalName: String, storedName: String,
        mimeType: String, sizeBytes: Long, uploadedBy: String
    ): FileInfo = dbQuery {
        val fileId = UUID.randomUUID()
        FilesTable.insert {
            it[id]                      = fileId
            it[FilesTable.originalName] = originalName
            it[FilesTable.storedName]   = storedName
            it[FilesTable.mimeType]     = mimeType
            it[FilesTable.sizeBytes]    = sizeBytes
            it[FilesTable.uploadedBy]   = uploadedBy
        }
        FilesTable.select { FilesTable.id eq fileId }.single().toFileInfo()
    }

    override suspend fun saveObjectInfo(
        originalName: String, mimeType: String, sizeBytes: Long,
        uploadedBy: String, sha256: String, objectKey: String, thumbObjectKey: String?
    ): FileInfo = dbQuery {
        val fileId = UUID.randomUUID()
        FilesTable.insert {
            it[id]                      = fileId
            it[FilesTable.originalName] = originalName
            it[FilesTable.storedName]   = objectKey
            it[FilesTable.mimeType]     = mimeType
            it[FilesTable.sizeBytes]    = sizeBytes
            it[FilesTable.uploadedBy]   = uploadedBy
            it[FilesTable.sha256]       = sha256
            it[FilesTable.objectKey]    = objectKey
            it[FilesTable.thumbObjectKey] = thumbObjectKey
        }
        FilesTable.select { FilesTable.id eq fileId }.single().toFileInfo()
    }

    override suspend fun findBySha256(sha256: String): FileInfo? = dbQuery {
        FilesTable.select { FilesTable.sha256 eq sha256 }.limit(1).singleOrNull()?.toFileInfo()
    }

    override suspend fun getFileById(fileId: UUID): FileInfo? = dbQuery {
        FilesTable.select { FilesTable.id eq fileId }.singleOrNull()?.toFileInfo()
    }

    override suspend fun getStoredName(fileId: UUID): String? = dbQuery {
        FilesTable.select { FilesTable.id eq fileId }.singleOrNull()?.get(FilesTable.storedName)
    }

    override suspend fun getMeta(fileId: UUID): StoredFileMeta? = dbQuery {
        FilesTable.select { FilesTable.id eq fileId }.singleOrNull()?.let {
            StoredFileMeta(
                id             = it[FilesTable.id],
                originalName   = it[FilesTable.originalName],
                mimeType       = it[FilesTable.mimeType],
                sizeBytes      = it[FilesTable.sizeBytes],
                sha256         = it[FilesTable.sha256],
                objectKey      = it[FilesTable.objectKey],
                thumbObjectKey = it[FilesTable.thumbObjectKey],
                storedName     = it[FilesTable.storedName]
            )
        }
    }

    override suspend fun deleteFile(fileId: UUID) = dbQuery {
        FilesTable.deleteWhere { FilesTable.id eq fileId }
        Unit
    }

    override suspend fun isOrphan(fileId: UUID): Boolean = dbQuery {
        val chatRefs = MessagesTable.select {
            (MessagesTable.fileId eq fileId) and (MessagesTable.isDeleted eq false)
        }.count()
        val channelRefs = ChannelMessagesTable.select {
            (ChannelMessagesTable.fileId eq fileId) and (ChannelMessagesTable.isDeleted eq false)
        }.count()
        chatRefs + channelRefs == 0L
    }
}
