/**
 * data/repository/AuthRepositoryImpl.kt
 * Реализация AuthRepository: Firebase Auth (email или Google),
 * затем POST /auth/register — создание профиля в PostgreSQL.
 */
package com.whatsmax.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.whatsmax.data.remote.api.ApiService
import com.whatsmax.data.remote.dto.CreateUserRequest
import com.whatsmax.data.remote.websocket.WebSocketClient
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.model.User
import com.whatsmax.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val apiService: ApiService,
    private val webSocketClient: WebSocketClient
) : AuthRepository {

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            // После входа — получаем профиль из бэкенда
            val response = apiService.getMe()
            if (response.isSuccessful) {
                Result.Success(response.bodyOrThrow().toModel())
            } else {
                Result.Error("Failed to fetch profile: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Sign in failed")
        }
    }

    override suspend fun signUpWithEmail(
        email: String, password: String,
        username: String, displayName: String
    ): Result<User> {
        return try {
            // 1. Создаём аккаунт в Firebase
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()

            // 2. Регистрируем профиль в PostgreSQL
            val response = apiService.registerUser(
                CreateUserRequest(
                    username    = username,
                    displayName = displayName,
                    email       = email
                )
            )
            if (response.isSuccessful) {
                Result.Success(response.bodyOrThrow().toModel())
            } else {
                // Если бэкенд вернул ошибку — откатываем Firebase-аккаунт
                firebaseAuth.currentUser?.delete()?.await()
                Result.Error("Registration failed: ${response.code()}")
            }
        } catch (e: Exception) {
            firebaseAuth.currentUser?.delete()?.await()
            Result.Error(e.message ?: "Sign up failed")
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()

            // Пробуем загрузить существующий профиль
            val meResponse = apiService.getMe()
            if (meResponse.isSuccessful) {
                return Result.Success(meResponse.bodyOrThrow().toModel())
            }

            // Если профиля нет (новый пользователь) — создаём
            val user = firebaseAuth.currentUser!!
            val regResponse = apiService.registerUser(
                CreateUserRequest(
                    username    = user.uid.take(16),       // временный username
                    displayName = user.displayName ?: "User",
                    email       = user.email
                )
            )
            if (regResponse.isSuccessful) {
                Result.Success(regResponse.bodyOrThrow().toModel())
            } else {
                Result.Error("Google sign in failed: ${regResponse.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Google sign in failed")
        }
    }

    override suspend fun signOut() {
        // Порядок важен:
        // 1) Отозвать токены на сервере — пока ещё есть валидный Authorization header
        // 2) Закрыть WS — серверный finally сбросит is_online=false
        // 3) Локальный signOut Firebase
        runCatching { apiService.signOutOnServer() }  // не валим logout если бэкенд недоступен
        webSocketClient.disconnect()
        firebaseAuth.signOut()
    }

    override suspend fun getCurrentUser(): User? {
        if (firebaseAuth.currentUser == null) return null
        return try {
            apiService.getMe().body()?.toModel()
        } catch (e: Exception) { null }
    }

    override fun getCurrentFirebaseUid(): String? = firebaseAuth.currentUser?.uid

    override fun getFirebaseIdToken(): Flow<String?> = flow {
        emit(firebaseAuth.currentUser?.getIdToken(false)?.await()?.token)
    }

    override fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null
}
