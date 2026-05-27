/**
 * domain/usecase/auth/AuthUseCases.kt
 * Use Cases аутентификации: вход/регистрация по email, Google Sign-In,
 * sign-out, получение текущего пользователя.
 */
package com.whatsmax.domain.usecase.auth

import com.whatsmax.domain.model.Result
import com.whatsmax.domain.model.User
import com.whatsmax.domain.repository.AuthRepository
import javax.inject.Inject

/** Вход по email и паролю через Firebase Auth */
class SignInWithEmailUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> =
        repo.signInWithEmail(email, password)
}

/** Регистрация нового пользователя */
class SignUpWithEmailUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(
        email: String, password: String,
        username: String, displayName: String
    ): Result<User> = repo.signUpWithEmail(email, password, username, displayName)
}

/** Вход через Google */
class SignInWithGoogleUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(idToken: String): Result<User> =
        repo.signInWithGoogle(idToken)
}

/** Выход из аккаунта */
class SignOutUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.signOut()
}

/** Получить текущего вошедшего пользователя */
class GetCurrentUserUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(): User? = repo.getCurrentUser()
}

/** Получить Firebase UID текущего пользователя без сетевого запроса */
class GetCurrentUserIdUseCase @Inject constructor(private val repo: AuthRepository) {
    operator fun invoke(): String? = repo.getCurrentFirebaseUid()
}

/** Проверить, вошёл ли пользователь */
class IsUserLoggedInUseCase @Inject constructor(private val repo: AuthRepository) {
    operator fun invoke(): Boolean = repo.isUserLoggedIn()
}
