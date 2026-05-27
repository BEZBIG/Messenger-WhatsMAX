/** Compose Navigation-граф приложения. */
package com.whatsmax.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.whatsmax.presentation.auth.login.LoginScreen
import com.whatsmax.presentation.auth.register.RegisterScreen
import com.whatsmax.presentation.call.CallScreen
import com.whatsmax.presentation.channel.ChannelDetailScreen
import com.whatsmax.presentation.channel.ChannelListScreen
import com.whatsmax.presentation.chat.ChatScreen
import com.whatsmax.presentation.home.HomeScreen
import com.whatsmax.presentation.profile.ProfileScreen
import com.whatsmax.presentation.profile.UserProfileScreen

/** Имена маршрутов навигации. */
object Routes {
    const val LOGIN           = "login"
    const val REGISTER        = "register"
    const val HOME            = "home"
    const val CHAT            = "chat/{chatId}?chatName={chatName}"
    const val PROFILE         = "profile"
    const val CHANNEL_LIST    = "channels"
    const val CHANNEL_DETAIL  = "channel/{channelId}"
    const val CALL            = "call/{chatId}?isVideo={isVideo}&isIncoming={isIncoming}"
    const val USER_PROFILE    = "user/{userId}"

    fun chat(chatId: String, chatName: String = "") = "chat/$chatId?chatName=$chatName"
    fun channel(channelId: String) = "channel/$channelId"
    fun call(chatId: String, isVideo: Boolean, isIncoming: Boolean = false) =
        "call/$chatId?isVideo=$isVideo&isIncoming=$isIncoming"
    fun userProfile(userId: String) = "user/$userId"
}

/** Корневой NavHost со всеми экранами. */
@Composable
fun WhatsMAXNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onOpenChat    = { chatId, name -> navController.navigate(Routes.chat(chatId, name)) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenChannels = { navController.navigate(Routes.CHANNEL_LIST) }
            )
        }

        composable(
            route = "chat/{chatId}?chatName={chatName}",
            arguments = listOf(
                navArgument("chatId")   { type = NavType.StringType },
                navArgument("chatName") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStack ->
            ChatScreen(
                chatId   = backStack.arguments?.getString("chatId")!!,
                chatName = backStack.arguments?.getString("chatName") ?: "",
                onBack   = { navController.popBackStack() },
                onStartCall = { chatId -> navController.navigate(Routes.call(chatId, false)) },
                onStartVideoCall = { chatId -> navController.navigate(Routes.call(chatId, true)) },
                onViewProfile = { userId -> navController.navigate(Routes.userProfile(userId)) }
            )
        }

        composable(
            route = "user/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStack ->
            UserProfileScreen(
                userId = backStack.arguments?.getString("userId")!!,
                onBack = { navController.popBackStack() },
                onOpenChat = { chatId -> navController.navigate(Routes.chat(chatId)) },
                onStartCall = { chatId, isVideo -> navController.navigate(Routes.call(chatId, isVideo)) }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSignOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } } }
            )
        }

        composable(Routes.CHANNEL_LIST) {
            ChannelListScreen(
                onOpenChannel = { channelId -> navController.navigate(Routes.channel(channelId)) },
                onBack        = { navController.popBackStack() }
            )
        }

        composable(
            route = "channel/{channelId}",
            arguments = listOf(navArgument("channelId") { type = NavType.StringType })
        ) { backStack ->
            ChannelDetailScreen(
                channelId         = backStack.arguments?.getString("channelId")!!,
                onBack            = { navController.popBackStack() },
                onOpenUserProfile = { userId -> navController.navigate(Routes.userProfile(userId)) }
            )
        }

        composable(
            route = "call/{chatId}?isVideo={isVideo}&isIncoming={isIncoming}",
            arguments = listOf(
                navArgument("chatId")     { type = NavType.StringType },
                navArgument("isVideo")    { type = NavType.BoolType; defaultValue = false },
                navArgument("isIncoming") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStack ->
            CallScreen(
                chatId     = backStack.arguments?.getString("chatId")!!,
                isVideo    = backStack.arguments?.getBoolean("isVideo") ?: false,
                isIncoming = backStack.arguments?.getBoolean("isIncoming") ?: false,
                onEnd      = { navController.popBackStack() }
            )
        }
    }
}
