/**
 * data/repository/ChatRepositoryImpl.kt
 * Реализация ChatRepository: CRUD-операции с чатами через Ktor REST API.
 */
package com.whatsmax.data.repository

import com.whatsmax.data.remote.api.ApiService
import com.whatsmax.data.remote.dto.CreateChatRequest
import com.whatsmax.domain.model.Chat
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ChatRepository {

    override suspend fun createDirectChat(otherUserId: String): Result<Chat> = safeApiCall {
        apiService.createChat(CreateChatRequest("direct", listOf(otherUserId))).bodyOrThrow().toModel()
    }

    override suspend fun createGroupChat(name: String, memberUids: List<String>): Result<Chat> = safeApiCall {
        apiService.createChat(CreateChatRequest("group", memberUids, name)).bodyOrThrow().toModel()
    }

    override suspend fun getChats(): Result<List<Chat>> = safeApiCall {
        apiService.getChats().bodyOrThrow().map { it.toModel() }
    }

    override suspend fun getChatById(chatId: String): Result<Chat> = safeApiCall {
        apiService.getChatById(chatId).bodyOrThrow().toModel()
    }

    override suspend fun addMember(chatId: String, userId: String): Result<Unit> = safeApiCall {
        apiService.addMember(chatId, mapOf("userId" to userId))
        Unit
    }

    override suspend fun removeMember(chatId: String, userId: String): Result<Unit> = safeApiCall {
        apiService.removeMember(chatId, userId)
        Unit
    }

    override suspend fun deleteChat(chatId: String): Result<Unit> = safeApiCall {
        apiService.deleteChat(chatId)
        Unit
    }

    override suspend fun updateChat(chatId: String, name: String?, description: String?): Result<Chat> = safeApiCall {
        val body = buildMap {
            name?.let { put("name", it) }
            description?.let { put("description", it) }
        }
        apiService.updateChat(chatId, body).bodyOrThrow().toModel()
    }
}

/** Безопасный вызов API с понятными сообщениями об ошибках */
suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: java.net.ConnectException) {
        Result.Error("Сервер недоступен. Проверьте, что бэкенд запущен.")
    } catch (e: java.net.UnknownHostException) {
        Result.Error("Нет подключения к интернету")
    } catch (e: java.net.SocketTimeoutException) {
        Result.Error("Превышено время ожидания")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Неизвестная ошибка")
    }
}

/** Возвращает тело ответа или кидает исключение с HTTP-кодом и телом ошибки */
fun <T> retrofit2.Response<T>.bodyOrThrow(): T {
    if (isSuccessful) return body() ?: error("Пустой ответ от сервера (${code()})")
    val errBody = errorBody()?.string()?.take(300)
    error("Ошибка ${code()}: ${errBody ?: message()}")
}
