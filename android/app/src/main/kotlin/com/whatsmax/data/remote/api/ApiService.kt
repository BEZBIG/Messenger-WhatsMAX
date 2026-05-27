/** Retrofit-интерфейс REST API. */
package com.whatsmax.data.remote.api

import com.whatsmax.data.remote.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/register")
    suspend fun registerUser(@Body request: CreateUserRequest): Response<UserDto>

    @GET("auth/me")
    suspend fun getMe(): Response<UserDto>

    @DELETE("auth/me")
    suspend fun deleteAccount(): Response<Unit>

    @POST("auth/sign-out")
    suspend fun signOutOnServer(): Response<Map<String, String>>

    @GET("users/search")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): Response<List<UserDto>>

    @GET("users/{uid}")
    suspend fun getUserByUid(@Path("uid") uid: String): Response<UserDto>

    @PUT("users/me")
    suspend fun updateProfile(@Body request: UpdateUserRequest): Response<UserDto>

    @POST("users/me/fcm-token")
    suspend fun updateFcmToken(@Body body: Map<String, String>): Response<Map<String, String>>

    @POST("chats")
    suspend fun createChat(@Body request: CreateChatRequest): Response<ChatDto>

    @GET("chats")
    suspend fun getChats(): Response<List<ChatDto>>

    @GET("chats/{id}")
    suspend fun getChatById(@Path("id") id: String): Response<ChatDto>

    @PUT("chats/{id}")
    suspend fun updateChat(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<ChatDto>

    @GET("chats/{id}/members")
    suspend fun getChatMembers(@Path("id") id: String): Response<List<ChatMemberDto>>

    @POST("chats/{id}/members")
    suspend fun addMember(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<Map<String, String>>

    @DELETE("chats/{id}/members/{uid}")
    suspend fun removeMember(
        @Path("id") id: String,
        @Path("uid") uid: String
    ): Response<Map<String, String>>

    @DELETE("chats/{id}")
    suspend fun deleteChat(@Path("id") id: String): Response<Unit>

    @GET("chats/{chatId}/messages")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("limit") limit: Int = 50,
        @Query("before") before: String? = null
    ): Response<List<MessageDto>>

    @POST("chats/{chatId}/messages")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Body request: SendMessageRequest
    ): Response<MessageDto>

    @PUT("chats/{chatId}/messages/{msgId}")
    suspend fun editMessage(
        @Path("chatId") chatId: String,
        @Path("msgId") msgId: String,
        @Body request: EditMessageRequest
    ): Response<MessageDto>

    @DELETE("chats/{chatId}/messages/{msgId}")
    suspend fun deleteMessage(
        @Path("chatId") chatId: String,
        @Path("msgId") msgId: String
    ): Response<Unit>

    @POST("chats/{chatId}/messages/{msgId}/read")
    suspend fun markAsRead(
        @Path("chatId") chatId: String,
        @Path("msgId") msgId: String
    ): Response<Unit>

    @POST("channels")
    suspend fun createChannel(@Body request: CreateChannelRequest): Response<ChannelDto>

    @GET("channels/search")
    suspend fun searchChannels(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): Response<List<ChannelDto>>

    @GET("channels/my")
    suspend fun getMyChannels(): Response<List<ChannelDto>>

    @GET("channels/{id}")
    suspend fun getChannelById(@Path("id") id: String): Response<ChannelDto>

    @POST("channels/{id}/subscribe")
    suspend fun subscribeToChannel(@Path("id") id: String): Response<Map<String, String>>

    @DELETE("channels/{id}/subscribe")
    suspend fun unsubscribeFromChannel(@Path("id") id: String): Response<Map<String, String>>

    @GET("channels/{id}/messages")
    suspend fun getChannelMessages(
        @Path("id") id: String,
        @Query("limit") limit: Int = 50
    ): Response<List<ChannelMessageDto>>

    @POST("channels/{id}/messages")
    suspend fun postToChannel(
        @Path("id") id: String,
        @Body request: SendMessageRequest
    ): Response<ChannelMessageDto>

    @GET("channels/{id}/subscribers")
    suspend fun getChannelSubscribers(@Path("id") id: String): Response<List<UserDto>>

    @PUT("channels/{id}")
    suspend fun updateChannel(
        @Path("id") id: String,
        @Body request: com.whatsmax.data.remote.dto.UpdateChannelRequest
    ): Response<ChannelDto>

    @DELETE("channels/{id}")
    suspend fun deleteChannel(@Path("id") id: String): Response<Unit>

    @GET("channels/{id}/messages/{msgId}/comments")
    suspend fun getComments(
        @Path("id") id: String,
        @Path("msgId") msgId: String
    ): Response<List<ChannelCommentDto>>

    @POST("channels/{id}/messages/{msgId}/comments")
    suspend fun addComment(
        @Path("id") id: String,
        @Path("msgId") msgId: String,
        @Body request: PostCommentRequest
    ): Response<ChannelCommentDto>

    @GET("reactions/{type}/{entityId}")
    suspend fun getReactions(
        @Path("type") type: String,
        @Path("entityId") entityId: String
    ): Response<ReactionSummaryDto>

    @PUT("reactions/{type}/{entityId}")
    suspend fun setReaction(
        @Path("type") type: String,
        @Path("entityId") entityId: String,
        @Body request: ReactionRequest
    ): Response<ReactionSummaryDto>

    @DELETE("reactions/{type}/{entityId}")
    suspend fun removeReaction(
        @Path("type") type: String,
        @Path("entityId") entityId: String
    ): Response<ReactionSummaryDto>

    @Multipart
    @POST("files/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<FileInfoDto>
}
