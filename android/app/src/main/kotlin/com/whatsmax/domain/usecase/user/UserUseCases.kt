/** Use Cases профилей пользователей: поиск, получение, обновление. */
package com.whatsmax.domain.usecase.user

import com.whatsmax.domain.model.Result
import com.whatsmax.domain.model.User
import com.whatsmax.domain.repository.UserRepository
import javax.inject.Inject

class SearchUsersUseCase @Inject constructor(private val repo: UserRepository) {
    suspend operator fun invoke(query: String): Result<List<User>> = repo.searchUsers(query)
}

class GetUserByUidUseCase @Inject constructor(private val repo: UserRepository) {
    suspend operator fun invoke(uid: String): Result<User> = repo.getUserByUid(uid)
}

class UpdateProfileUseCase @Inject constructor(private val repo: UserRepository) {
    suspend operator fun invoke(displayName: String?, bio: String?, username: String?, avatarUrl: String? = null): Result<User> =
        repo.updateProfile(displayName, bio, username, avatarUrl)
}
