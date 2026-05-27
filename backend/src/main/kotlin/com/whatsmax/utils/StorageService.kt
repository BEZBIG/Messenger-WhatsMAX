/** Обёртка над MinIO SDK для хранения файлов. */
package com.whatsmax.utils

import io.minio.*
import io.minio.http.Method
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class StorageConfig(
    val endpoint: String,
    val publicEndpoint: String,
    val accessKey: String,
    val secretKey: String,
    val bucket: String,
    val presignedTtlSeconds: Int
)

class StorageService(private val cfg: StorageConfig) {

    private val client: MinioClient = MinioClient.builder()
        .endpoint(cfg.endpoint)
        .credentials(cfg.accessKey, cfg.secretKey)
        .build()

    suspend fun upload(objectKey: String, bytes: ByteArray, contentType: String): String =
        withContext(Dispatchers.IO) {
            ByteArrayInputStream(bytes).use { input ->
                client.putObject(
                    PutObjectArgs.builder()
                        .bucket(cfg.bucket)
                        .`object`(objectKey)
                        .stream(input, bytes.size.toLong(), -1)
                        .contentType(contentType)
                        .build()
                )
            }
            objectKey
        }

    suspend fun uploadStream(
        objectKey: String, input: InputStream, contentType: String, totalSize: Long
    ): String = withContext(Dispatchers.IO) {
        client.putObject(
            PutObjectArgs.builder()
                .bucket(cfg.bucket)
                .`object`(objectKey)
                .stream(input, totalSize, -1)
                .contentType(contentType)
                .build()
        )
        objectKey
    }

    fun presignedGetUrl(objectKey: String): String =
        "${cfg.publicEndpoint.trimEnd('/')}/${cfg.bucket}/$objectKey"

    suspend fun delete(objectKey: String) = withContext(Dispatchers.IO) {
        runCatching {
            client.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(cfg.bucket)
                    .`object`(objectKey)
                    .build()
            )
        }
        Unit
    }
}
