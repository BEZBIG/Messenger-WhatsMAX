/** Upload/download файлов: MinIO + SHA-256 дедупликация + thumbnails. */
package com.whatsmax.routes

import com.whatsmax.domain.repositories.FileRepository
import com.whatsmax.plugins.FirebasePrincipal
import com.whatsmax.utils.MimeDetector
import com.whatsmax.utils.StorageService
import com.whatsmax.utils.ThumbnailGenerator
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.io.FileOutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.UUID

fun Route.fileRoutes(
    fileRepository: FileRepository,
    storageService: StorageService,
    uploadPath: String,
    maxFileSizeBytes: Long
) {
    val legacyUploadDir = File(uploadPath).also { it.mkdirs() }

    authenticate("firebase") {
      rateLimit(RateLimitName("uploads")) {
        post("/files/upload") {
            val principal = call.principal<FirebasePrincipal>()!!

            val declaredLength = call.request.header(HttpHeaders.ContentLength)?.toLongOrNull()
            if (declaredLength != null && declaredLength > maxFileSizeBytes) {
                throw IllegalArgumentException("File too large: $declaredLength bytes (max $maxFileSizeBytes)")
            }

            val multipart = call.receiveMultipart()
            var fileInfo: com.whatsmax.domain.models.FileInfo? = null

            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val originalName = part.originalFileName ?: "file"
                    val mimeType     = part.contentType?.toString() ?: "application/octet-stream"

                    val tempFile = File.createTempFile("upload_", ".bin", legacyUploadDir)
                    val digest = MessageDigest.getInstance("SHA-256")
                    var bytesWritten: Long = 0
                    try {
                        part.streamProvider().use { rawIn ->
                            DigestInputStream(rawIn, digest).use { hashIn ->
                                FileOutputStream(tempFile).use { out ->
                                    val buf = ByteArray(64 * 1024)
                                    while (true) {
                                        val n = hashIn.read(buf)
                                        if (n == -1) break
                                        bytesWritten += n
                                        if (bytesWritten > maxFileSizeBytes) {
                                            throw IllegalArgumentException(
                                                "File too large: $bytesWritten bytes (max $maxFileSizeBytes)"
                                            )
                                        }
                                        out.write(buf, 0, n)
                                    }
                                }
                            }
                        }
                        val sha256 = digest.digest().joinToString("") { "%02x".format(it) }

                        val headBytes = tempFile.inputStream().use { it.readNBytes(16) }
                        if (!MimeDetector.isConsistent(mimeType, headBytes)) {
                            throw IllegalArgumentException(
                                "File content does not match declared content-type '$mimeType'"
                            )
                        }

                        val existing = fileRepository.findBySha256(sha256)
                        if (existing != null) {
                            fileInfo = existing
                        } else {
                            val ext = originalName.substringAfterLast('.', missingDelimiterValue = "")
                            val objectKey = if (ext.isNotEmpty()) "$sha256.$ext" else sha256
                            tempFile.inputStream().use { fis ->
                                storageService.uploadStream(objectKey, fis, mimeType, bytesWritten)
                            }

                            val thumbKey: String? = if (ThumbnailGenerator.supports(mimeType)) {
                                val originalBytes = tempFile.readBytes()
                                ThumbnailGenerator.generate(originalBytes)?.let { thumbBytes ->
                                    val tk = "${sha256}_thumb.jpg"
                                    storageService.upload(tk, thumbBytes, "image/jpeg")
                                    tk
                                }
                            } else null

                            fileInfo = fileRepository.saveObjectInfo(
                                originalName   = originalName,
                                mimeType       = mimeType,
                                sizeBytes      = bytesWritten,
                                uploadedBy     = principal.uid,
                                sha256         = sha256,
                                objectKey      = objectKey,
                                thumbObjectKey = thumbKey
                            )
                        }
                    } finally {
                        tempFile.delete()
                    }
                }
                part.dispose()
            }

            val info = fileInfo ?: throw IllegalArgumentException("No file in request")
            call.respond(HttpStatusCode.Created, info)
        }
      }
    }

    get("/files/{id}") {
        val fileId = UUID.fromString(call.parameters["id"])
        val meta = fileRepository.getMeta(fileId)
            ?: throw NoSuchElementException("File not found")

        val objectKey = meta.objectKey
        if (objectKey != null) {
            call.respondRedirect(storageService.presignedGetUrl(objectKey), permanent = false)
            return@get
        }

        val storedName = meta.storedName ?: throw NoSuchElementException("File has no storage")
        val file = File(legacyUploadDir, storedName)
        if (!file.exists()) throw NoSuchElementException("File not found on disk")
        call.response.header(HttpHeaders.ContentType, meta.mimeType)
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.FileName, meta.originalName).toString()
        )
        call.respondFile(file)
    }

    get("/files/{id}/thumb") {
        val fileId = UUID.fromString(call.parameters["id"])
        val meta = fileRepository.getMeta(fileId)
            ?: throw NoSuchElementException("File not found")
        val thumbKey = meta.thumbObjectKey
            ?: throw NoSuchElementException("Thumbnail not available")
        call.respondRedirect(storageService.presignedGetUrl(thumbKey), permanent = false)
    }
}
