/** Реализация UserRepository: поиск и обновление профилей. */
package com.whatsmax.data.repository

import com.whatsmax.data.remote.api.ApiService
import com.whatsmax.data.remote.dto.UpdateUserRequest
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.model.User
import com.whatsmax.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : UserRepository {

    override suspend fun getUserByUid(uid: String): Result<User> = safeApiCall {
        apiService.getUserByUid(uid).bodyOrThrow().toModel()
    }

    override suspend fun searchUsers(query: String): Result<List<User>> = safeApiCall {
        apiService.searchUsers(query).bodyOrThrow().map { it.toModel() }
    }

    override suspend fun updateProfile(
        displayName: String?, bio: String?, username: String?, avatarUrl: String?
    ): Result<User> = safeApiCall {
        apiService.updateProfile(UpdateUserRequest(displayName, bio, username, avatarUrl = avatarUrl)).bodyOrThrow().toModel()
    }

    override suspend fun updateFcmToken(token: String): Result<Unit> = safeApiCall {
        apiService.updateFcmToken(mapOf("token" to token))
        Unit
    }
}
