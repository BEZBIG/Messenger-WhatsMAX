/** Use Cases аутентификации: email, Google, sign-out. */
package com.whatsmax.domain.usecase.auth

import com.whatsmax.domain.model.Result
import com.whatsmax.domain.model.User
import com.whatsmax.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithEmailUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> =
        repo.signInWithEmail(email, password)
}

class SignUpWithEmailUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(
        email: String, password: String,
        username: String, displayName: String
    ): Result<User> = repo.signUpWithEmail(email, password, username, displayName)
}

class SignInWithGoogleUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(idToken: String): Result<User> =
        repo.signInWithGoogle(idToken)
}

class SignOutUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.signOut()
}

class GetCurrentUserUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(): User? = repo.getCurrentUser()
}

class GetCurrentUserIdUseCase @Inject constructor(private val repo: AuthRepository) {
    operator fun invoke(): String? = repo.getCurrentFirebaseUid()
}

class IsUserLoggedInUseCase @Inject constructor(private val repo: AuthRepository) {
    operator fun invoke(): Boolean = repo.isUserLoggedIn()
}
