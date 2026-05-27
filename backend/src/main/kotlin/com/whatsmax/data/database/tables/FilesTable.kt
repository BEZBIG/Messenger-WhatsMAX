/** Метаданные загруженных файлов. */
package com.whatsmax.data.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object FilesTable : Table("files") {
    val id           = uuid("id").autoGenerate()
    val originalName = varchar("original_name", 255)
    val storedName   = varchar("stored_name", 255)
    val mimeType     = varchar("mime_type", 128)
    val sizeBytes    = long("size_bytes")
    val uploadedBy   = varchar("uploaded_by", 128).references(UsersTable.uid)
    val uploadedAt   = datetime("uploaded_at").default(LocalDateTime.now())
    val sha256         = varchar("sha256", 64).nullable()
    val objectKey      = varchar("object_key", 255).nullable()
    val thumbObjectKey = varchar("thumb_object_key", 255).nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, sha256)
    }
}
