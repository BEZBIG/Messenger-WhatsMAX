/** Hilt-модуль: привязки интерфейсов репозиториев к реализациям. */
package com.whatsmax.di

import com.whatsmax.data.remote.api.ApiService
import com.whatsmax.data.remote.websocket.WebSocketClient
import com.whatsmax.data.repository.*
import com.whatsmax.domain.repository.*
import com.whatsmax.domain.repository.ReactionRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        apiService: ApiService,
        webSocketClient: WebSocketClient
    ): AuthRepository = AuthRepositoryImpl(firebaseAuth, apiService, webSocketClient)

    @Provides @Singleton
    fun provideUserRepository(apiService: ApiService): UserRepository =
        UserRepositoryImpl(apiService)

    @Provides @Singleton
    fun provideChatRepository(apiService: ApiService): ChatRepository =
        ChatRepositoryImpl(apiService)

    @Provides @Singleton
    fun provideMessageRepository(
        apiService: ApiService,
        wsClient: WebSocketClient
    ): MessageRepository = MessageRepositoryImpl(apiService, wsClient)

    @Provides @Singleton
    fun provideChannelRepository(apiService: ApiService): ChannelRepository =
        ChannelRepositoryImpl(apiService)

    @Provides @Singleton
    fun provideWebSocketRepository(wsClient: WebSocketClient): WebSocketRepository =
        WebSocketRepositoryImpl(wsClient)

    @Provides @Singleton
    fun provideFileRepository(apiService: ApiService, baseUrl: String): FileRepository =
        FileRepositoryImpl(apiService, baseUrl)

    @Provides @Singleton
    fun provideReactionRepository(apiService: ApiService): ReactionRepository =
        ReactionRepositoryImpl(apiService)
}
