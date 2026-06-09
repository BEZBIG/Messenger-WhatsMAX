/** Главный экран: список чатов, поиск, создание новых чатов. */
package com.whatsmax.presentation.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.whatsmax.R
import com.whatsmax.presentation.theme.WhatsMAXTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.whatsmax.domain.model.Chat
import com.whatsmax.domain.model.ChatType
import com.whatsmax.domain.model.MessageType
import com.whatsmax.domain.model.User
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenChat: (chatId: String, chatName: String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenChannels: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadData()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showFabMenu by remember { mutableStateOf(false) }
    var showNewDirectDialog by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenChannels) { Icon(Icons.Default.Newspaper, stringResource(R.string.channels_title)) }
                    IconButton(onClick = onOpenProfile) { Icon(Icons.Default.Person, stringResource(R.string.profile_title)) }
                }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showFabMenu = true }) {
                    Icon(Icons.Default.Edit, stringResource(R.string.new_chat))
                }
                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_direct_chat)) },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        onClick = { showFabMenu = false; showNewDirectDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_group_chat)) },
                        leadingIcon = { Icon(Icons.Default.Group, null) },
                        onClick = { showFabMenu = false; showNewGroupDialog = true }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            SearchBar(
                query         = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.chats, key = { it.id }) { chat ->
                        val displayName = if (chat.type == ChatType.DIRECT) {
                            chat.members.firstOrNull { it.userId != state.currentUserId }?.displayName
                                ?: chat.name ?: stringResource(R.string.chat_default_name)
                        } else {
                            chat.name ?: chat.members.firstOrNull()?.displayName ?: stringResource(R.string.chat_default_name)
                        }
                        ChatListItem(
                            chat          = chat,
                            currentUserId = state.currentUserId,
                            displayName   = displayName,
                            onClick       = {
                                viewModel.markChatAsRead(chat.id)
                                onOpenChat(chat.id, displayName)
                            },
                            onDeleteForMe  = { viewModel.deleteChatForMe(chat.id) },
                            onDeleteForAll = { viewModel.deleteChatForAll(chat.id) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                    }
                }
            }
        }
    }

    if (showNewDirectDialog) {
        NewDirectChatDialog(
            searchResults = state.searchResults,
            searchQuery   = state.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            onSelectUser  = { userId ->
                showNewDirectDialog = false
                viewModel.openDirectChat(userId) { chatId -> onOpenChat(chatId, "") }
            },
            onDismiss = { showNewDirectDialog = false; viewModel.onSearchQueryChange("") }
        )
    }

    if (showNewGroupDialog) {
        NewGroupChatDialog(
            searchResults = state.searchResults,
            searchQuery   = state.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            onCreate      = { name, memberIds ->
                showNewGroupDialog = false
                viewModel.createGroupChat(name, memberIds) { chatId -> onOpenChat(chatId, name) }
            },
            onDismiss = { showNewGroupDialog = false; viewModel.onSearchQueryChange("") }
        )
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value         = query,
        onValueChange = onQueryChange,
        placeholder   = { Text(stringResource(R.string.home_search_hint)) },
        leadingIcon   = { Icon(Icons.Default.Search, null) },
        singleLine    = true,
        modifier      = modifier,
        shape         = MaterialTheme.shapes.extraLarge
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatListItem(
    chat: Chat,
    currentUserId: String,
    displayName: String,
    onClick: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForAll: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = { showMenu = true })
                .background(if (showMenu) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val peerAvatarUrl = if (chat.type == ChatType.DIRECT)
                chat.members.firstOrNull { it.userId != currentUserId }?.avatarUrl
            else
                chat.avatarUrl
            if (peerAvatarUrl != null) {
                AsyncImage(model = peerAvatarUrl, contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(CircleShape))
            } else {
                Box(Modifier.size(52.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape,
                        modifier = Modifier.fillMaxSize()) {}
                    Text(
                        text  = displayName.firstOrNull()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold, fontSize = 20.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text       = displayName,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f)
                    )
                    Text(
                        text  = chat.lastMessage?.createdAt?.toSmartTime() ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hasUnread = chat.unreadCount > 0
                    val lastMsg = chat.lastMessage
                    val lastMsgText = when {
                        lastMsg == null -> stringResource(R.string.no_messages)
                        lastMsg.isDeleted -> stringResource(R.string.message_deleted)
                        lastMsg.type == MessageType.CALL  -> lastMsg.content ?: stringResource(R.string.msg_call)
                        lastMsg.type == MessageType.IMAGE -> stringResource(R.string.msg_photo)
                        lastMsg.type == MessageType.VOICE -> stringResource(R.string.msg_voice)
                        lastMsg.type == MessageType.AUDIO -> stringResource(R.string.msg_audio)
                        lastMsg.type == MessageType.VIDEO -> stringResource(R.string.msg_video)
                        lastMsg.type == MessageType.FILE  -> lastMsg.fileName ?: stringResource(R.string.msg_file)
                        else -> lastMsg.content ?: stringResource(R.string.no_messages)
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (lastMsg != null && !lastMsg.isDeleted) {
                            val typeIcon = when (lastMsg.type) {
                                MessageType.CALL  -> Icons.Default.Phone
                                MessageType.IMAGE -> Icons.Default.Photo
                                MessageType.VOICE -> Icons.Default.Mic
                                MessageType.AUDIO -> Icons.Default.Audiotrack
                                MessageType.VIDEO -> Icons.Default.Videocam
                                MessageType.FILE  -> Icons.Default.AttachFile
                                else -> null
                            }
                            typeIcon?.let {
                                Icon(
                                    it, null,
                                    modifier = Modifier.size(13.dp).padding(end = 2.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                )
                            }
                        }
                        Text(
                            text       = lastMsgText,
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                            color      = if (hasUnread)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                    }
                    if (hasUnread) {
                        Spacer(Modifier.width(6.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_for_me)) },
                leadingIcon = { Icon(Icons.Default.DeleteOutline, null) },
                onClick = { showMenu = false; onDeleteForMe() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_for_all), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; onDeleteForAll() }
            )
        }
    }
}

private fun String.toSmartTime(): String {
    return try {
        val datePart = substringBefore("T").let { if (it.length == 10) it else take(10) }
        val timePart = substringAfter("T").take(5)
        val parts    = datePart.split("-")
        val msgDate  = LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        if (msgDate == LocalDate.now()) timePart
        else {
            val months = arrayOf("","янв","фев","мар","апр","май","июн","июл","авг","сен","окт","ноя","дек")
            "${parts[2].toInt()} ${months[parts[1].toInt()]}"
        }
    } catch (e: Exception) { "" }
}

@Composable
private fun NewDirectChatDialog(
    searchResults: List<User>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSelectUser: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_direct_chat)) },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery, onValueChange = onQueryChange,
                    placeholder = { Text(stringResource(R.string.search_users_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (searchQuery.length < 2) {
                    Text(
                        stringResource(R.string.search_min_chars),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(searchResults) { user ->
                            ListItem(
                                headlineContent = { Text(user.displayName) },
                                supportingContent = { Text("@${user.username}") },
                                modifier = Modifier.clickable { onSelectUser(user.uid) }
                            )
                        }
                        if (searchResults.isEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.users_not_found),
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    WhatsMAXTheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("WhatsMAX", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = {}) { Icon(Icons.Default.Newspaper, "Каналы") }
                        IconButton(onClick = {}) { Icon(Icons.Default.Person, "Профиль") }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {}) { Icon(Icons.Default.Edit, "Новый чат") }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = "", onValueChange = {},
                    placeholder = { Text("Поиск чатов и пользователей...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.extraLarge
                )
                val sampleChats = listOf(
                    Triple("Никита", "Привет! Как дела?", "20:37"),
                    Triple("Команда проекта", "Завтра созвон в 10:00", "19:15"),
                    Triple("Мария", "Фото", "18:40")
                )
                sampleChats.forEach { (name, msg, time) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(52.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape,
                                modifier = Modifier.fillMaxSize()) {}
                            Text(name.first().uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name, fontWeight = FontWeight.SemiBold)
                                Text(time, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            }
                            Text(msg, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.6f), maxLines = 1)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

@Composable
private fun NewGroupChatDialog(
    searchResults: List<User>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onCreate: (name: String, memberIds: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val selectedUsers = remember { mutableStateListOf<User>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_group_chat)) },
        text = {
            Column {
                OutlinedTextField(
                    value = groupName, onValueChange = { groupName = it },
                    label = { Text(stringResource(R.string.group_name_hint)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                if (selectedUsers.isNotEmpty()) {
                    Text(
                        stringResource(R.string.group_members, selectedUsers.joinToString { it.displayName }),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                OutlinedTextField(
                    value = searchQuery, onValueChange = onQueryChange,
                    placeholder = { Text(stringResource(R.string.add_member_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                if (searchQuery.length >= 2) {
                    LazyColumn(Modifier.heightIn(max = 240.dp)) {
                        items(searchResults) { user ->
                            val isSelected = selectedUsers.any { it.uid == user.uid }
                            ListItem(
                                headlineContent = { Text(user.displayName) },
                                supportingContent = { Text("@${user.username}") },
                                trailingContent = {
                                    if (isSelected)
                                        Icon(Icons.Default.CheckCircle, null,
                                            tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.clickable {
                                    if (isSelected) selectedUsers.removeAll { it.uid == user.uid }
                                    else selectedUsers.add(user)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank() && selectedUsers.isNotEmpty()) {
                        onCreate(groupName.trim(), selectedUsers.map { it.uid })
                    }
                },
                enabled = groupName.isNotBlank() && selectedUsers.isNotEmpty()
            ) { Text(stringResource(R.string.create)) }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
