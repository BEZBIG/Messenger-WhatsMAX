/** Главная Activity приложения WhatsMAX. */
package com.whatsmax.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.whatsmax.data.remote.websocket.WebSocketClient
import com.whatsmax.domain.repository.AuthRepository
import com.whatsmax.presentation.theme.WhatsMAXTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var wsClient: WebSocketClient

    private var startDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        splashScreen.setKeepOnScreenCondition { startDestination == null }

        CoroutineScope(Dispatchers.Main).launch {
            val isLoggedIn = authRepository.isUserLoggedIn()
            startDestination = if (isLoggedIn) {
                wsClient.connect()
                Routes.HOME
            } else {
                Routes.LOGIN
            }
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WhatsMAXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val dest = startDestination
                    if (dest != null) {
                        val navController = rememberNavController()
                        WhatsMAXNavHost(navController = navController, startDestination = dest)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wsClient.disconnect()
    }
}
