/** Экран профиля пользователя: аватар, контакты, действия */
package com.whatsmax.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.whatsmax.R
import com.whatsmax.presentation.theme.OnlineIndicator
import com.whatsmax.presentation.theme.WhatsMAXTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.model.User
import com.whatsmax.domain.usecase.auth.GetCurrentUserIdUseCase
import com.whatsmax.domain.usecase.chat.CreateDirectChatUseCase
import com.whatsmax.domain.usecase.user.GetUserByUidUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isStartingChat: Boolean = false,
    val error: String? = null,
    val currentUserId: String = ""
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val getUserByUidUseCase: GetUserByUidUseCase,
    private val createDirectChatUseCase: CreateDirectChatUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState

    fun load(uid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentUserId = getCurrentUserIdUseCase() ?: "") }
            when (val r = getUserByUidUseCase(uid)) {
                is Result.Success -> _uiState.update { it.copy(user = r.data, isLoading = false) }
                is Result.Error   -> _uiState.update { it.copy(error = r.message, isLoading = false) }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun openChat(onSuccess: (chatId: String) -> Unit) {
        val uid = _uiState.value.user?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isStartingChat = true) }
            when (val r = createDirectChatUseCase(uid)) {
                is Result.Success -> { _uiState.update { it.copy(isStartingChat = false) }; onSuccess(r.data.id) }
                is Result.Error   -> _uiState.update { it.copy(isStartingChat = false, error = r.message) }
                else -> _uiState.update { it.copy(isStartingChat = false) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onOpenChat: (chatId: String) -> Unit = {},
    onStartCall: (chatId: String, isVideo: Boolean) -> Unit = { _, _ -> },
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isOwnProfile = state.currentUserId.isEmpty() || state.currentUserId == userId
    var showBlockDialog by remember { mutableStateOf(false) }
    var isBlocked by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userId) { viewModel.load(userId) }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
                title = { Text(stringResource(R.string.profile_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.user != null -> {
                val user = state.user!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            )
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            if (user.avatarUrl != null) {
                                AsyncImage(
                                    model = user.avatarUrl,
                                    contentDescription = stringResource(R.string.cd_avatar),
                                    modifier = Modifier.size(90.dp).clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(90.dp).clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.displayName.firstOrNull()?.uppercase() ?: "?",
                                        fontSize = 36.sp, fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                user.displayName,
                                fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (user.username.isNotBlank()) {
                                Text(
                                    "@${user.username.trimStart('@')}",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(8.dp).clip(CircleShape)
                                        .background(if (user.isOnline) OnlineIndicator else Color.White.copy(0.4f))
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (user.isOnline) stringResource(R.string.online) else stringResource(R.string.offline),
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    if (!isOwnProfile) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .offset(y = (-28).dp),
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 4.dp,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ActionButton(
                                    icon  = if (state.isStartingChat) null else Icons.Default.Message,
                                    label = stringResource(R.string.action_message),
                                    isLoading = state.isStartingChat,
                                    onClick = { viewModel.openChat { chatId -> onOpenChat(chatId) } }
                                )
                                ActionButton(
                                    icon  = Icons.Default.Phone,
                                    label = stringResource(R.string.call_btn),
                                    onClick = {
                                        viewModel.openChat { chatId -> onStartCall(chatId, false) }
                                    }
                                )
                                ActionButton(
                                    icon  = Icons.Default.Videocam,
                                    label = stringResource(R.string.action_video),
                                    onClick = {
                                        viewModel.openChat { chatId -> onStartCall(chatId, true) }
                                    }
                                )
                                ActionButton(
                                    icon  = if (isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                                    label = if (isBlocked) stringResource(R.string.unblock) else stringResource(R.string.block),
                                    tint  = if (isBlocked) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.error,
                                    onClick = { showBlockDialog = true }
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                    ) {
                        if (!user.bio.isNullOrBlank()) {
                            InfoCard(title = stringResource(R.string.about_label)) {
                                Text(user.bio!!, fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        InfoCard(title = stringResource(R.string.contact_label)) {
                            if (user.username.isNotBlank()) {
                                InfoRow(Icons.Default.AlternateEmail, user.username)
                            }
                            if (!user.phone.isNullOrBlank()) {
                                InfoRow(Icons.Default.Phone, user.phone!!)
                            }
                            if (!user.email.isNullOrBlank()) {
                                InfoRow(Icons.Default.Email, user.email!!)
                            }
                            InfoRow(
                                icon  = Icons.Default.Circle,
                                text  = if (user.isOnline) stringResource(R.string.online_now) else stringResource(R.string.last_seen_recently),
                                tint  = if (user.isOnline) OnlineIndicator
                                        else MaterialTheme.colorScheme.onSurface.copy(0.5f)
                            )
                        }

                        if (isBlocked) {
                            Spacer(Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Block, null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.user_blocked),
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 14.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.user_not_found), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            icon = { Icon(
                if (isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                null,
                tint = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            ) },
            title = { Text(if (isBlocked) stringResource(R.string.unblock_q) else stringResource(R.string.block_q)) },
            text = {
                Text(
                    if (isBlocked)
                        stringResource(R.string.unblock_message, state.user?.displayName ?: "")
                    else
                        stringResource(R.string.block_message, state.user?.displayName ?: "")
                )
            },
            confirmButton = {
                Button(
                    onClick = { isBlocked = !isBlocked; showBlockDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBlocked) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error
                    )
                ) { Text(if (isBlocked) stringResource(R.string.unblock) else stringResource(R.string.block)) }
            },
            dismissButton = {
                TextButton({ showBlockDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun UserProfileScreenPreview() {
    WhatsMAXTheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    navigationIcon = { IconButton({}) { Icon(Icons.Default.ArrowBack, null) } },
                    title = { Text("Профиль") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                        .background(Brush.verticalGradient(listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.primaryContainer
                        ))).padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 24.dp)) {
                        Box(
                            modifier = Modifier.size(90.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) { Text("Н", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                        Spacer(Modifier.height(10.dp))
                        Text("Никита", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("@nikita", fontSize = 14.sp, color = Color.White.copy(0.85f))
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(OnlineIndicator))
                            Spacer(Modifier.width(5.dp))
                            Text("в сети", fontSize = 12.sp, color = Color.White.copy(0.8f))
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).offset(y = (-28).dp),
                    shape = RoundedCornerShape(16.dp), tonalElevation = 4.dp, shadowElevation = 4.dp
                ) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ActionButton(icon = Icons.Default.Message, label = "Написать", onClick = {})
                        ActionButton(icon = Icons.Default.Phone, label = "Позвонить", onClick = {})
                        ActionButton(icon = Icons.Default.Videocam, label = "Видео", onClick = {})
                        ActionButton(icon = Icons.Default.Block, label = "Заблокировать",
                            tint = MaterialTheme.colorScheme.error, onClick = {})
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp)) {
                    InfoCard(title = "О себе") {
                        Text("Разработчик мобильных приложений", fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(8.dp))
                    InfoCard(title = "Контакт") {
                        InfoRow(Icons.Default.AlternateEmail, "nikita")
                        InfoRow(Icons.Default.Email, "nikita@example.com")
                        InfoRow(Icons.Default.Circle, "Сейчас в сети", tint = OnlineIndicator)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector?,
    label: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(52.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = tint.copy(alpha = 0.12f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else if (icon != null) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = tint, textAlign = TextAlign.Center,
            maxLines = 2, lineHeight = 13.sp)
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String, tint: Color = MaterialTheme.colorScheme.onSurface.copy(0.6f)) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = tint)
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
