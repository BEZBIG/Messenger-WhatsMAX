/** Реализация FileRepository: multipart upload и URL файлов. */
package com.whatsmax.data.repository

import com.whatsmax.data.remote.api.ApiService
import com.whatsmax.domain.model.FileInfo
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.repository.FileRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val baseUrl: String
) : FileRepository {

    override suspend fun uploadFile(file: File, mimeType: String): Result<FileInfo> = safeApiCall {
        val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        apiService.uploadFile(part).bodyOrThrow().toModel()
    }

    override suspend fun getFileUrl(fileId: String): String = "$baseUrl/files/$fileId"
}
