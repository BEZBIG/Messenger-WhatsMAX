/** Реализация AuthRepository: Firebase Auth + backend. */
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
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
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

            val meResponse = apiService.getMe()
            if (meResponse.isSuccessful) {
                return Result.Success(meResponse.bodyOrThrow().toModel())
            }

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
