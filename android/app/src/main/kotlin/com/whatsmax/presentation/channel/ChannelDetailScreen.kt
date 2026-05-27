/** Экран ленты канала: посты, комментарии, реакции, подписка/отписка */
package com.whatsmax.presentation.channel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.whatsmax.BuildConfig
import com.whatsmax.domain.model.ChannelComment
import com.whatsmax.domain.model.ChannelMessage
import com.whatsmax.domain.model.MessageType
import com.whatsmax.domain.model.User
import com.whatsmax.presentation.theme.WhatsMAXTheme
import com.whatsmax.presentation.voice.VoiceMessagePlayer
import com.whatsmax.presentation.voice.VoiceRecorder

private val QUICK_EMOJIS = listOf("👍", "❤️", "😂", "😮", "😢", "🔥")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelDetailScreen(
    channelId: String,
    onBack: () -> Unit,
    onOpenUserProfile: (userId: String) -> Unit = {},
    viewModel: ChannelViewModel = hiltViewModel()
) {
    val state by viewModel.detailState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val voiceRecorder = remember { VoiceRecorder(context) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceRecorder.start()
            isRecordingVoice = true
        }
    }

    LaunchedEffect(channelId) { viewModel.loadChannelDetail(channelId) }

    val isOwner = state.channel != null && state.channel!!.ownerId == state.currentUserId

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
                title = {
                    Row(
                        modifier = Modifier.clickable {
                            if (isOwner) viewModel.openEditSheet()
                            else if (state.channel != null) viewModel.openInfoSheet()
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChannelAvatar(
                            name      = state.channel?.name ?: "",
                            avatarUrl = state.channel?.avatarUrl,
                            size      = 36
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(state.channel?.name ?: "Канал", fontWeight = FontWeight.SemiBold)
                            state.channel?.let {
                                Text("${it.membersCount} подписчиков", fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                            }
                        }
                    }
                },
                actions = {
                    if (isOwner) {
                        IconButton({ viewModel.loadSubscribers(channelId) }) {
                            Icon(Icons.Default.Group, "Подписчики")
                        }
                        IconButton({ showDeleteDialog = true }) {
                            if (state.isDeleting)
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else
                                Icon(Icons.Default.DeleteForever, "Удалить канал",
                                    tint = MaterialTheme.colorScheme.error)
                        }
                    } else if (state.channel != null) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                        } else if (state.channel!!.isSubscribed) {
                            TextButton({ viewModel.unsubscribe(channelId) }) { Text("Отписаться") }
                        } else {
                            Button({ viewModel.subscribe(channelId) }) { Text("Подписаться") }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isOwner) {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRecordingVoice) {
                            Row(
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(Color.Red))
                                Spacer(Modifier.width(8.dp))
                                Text("Запись... отпустите для отправки",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                            }
                        } else {
                            OutlinedTextField(
                                value = state.postText, onValueChange = viewModel::onPostTextChange,
                                placeholder = { Text("Написать пост...") },
                                modifier = Modifier.weight(1f), maxLines = 4,
                                shape = RoundedCornerShape(24.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        val showMic = state.postText.isBlank() && !state.isPosting
                        val containerColor = if (isRecordingVoice) Color.Red else MaterialTheme.colorScheme.primary
                        val baseModifier = Modifier.size(48.dp).clip(CircleShape).background(containerColor)
                        val interactiveModifier = if (showMic) {
                            baseModifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                            == PackageManager.PERMISSION_GRANTED) {
                                            voiceRecorder.start()
                                            isRecordingVoice = true
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                        val released = tryAwaitRelease()
                                        if (isRecordingVoice) {
                                            isRecordingVoice = false
                                            if (!released) {
                                                voiceRecorder.cancel()
                                            } else {
                                                val res = voiceRecorder.stop()
                                                if (res != null && res.durationMs >= 500) {
                                                    viewModel.postVoice(channelId, res.file, res.durationMs, res.waveform)
                                                } else {
                                                    res?.file?.delete()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        } else {
                            baseModifier.clickable(enabled = !state.isPosting) { viewModel.postMessage(channelId) }
                        }
                        Box(modifier = interactiveModifier, contentAlignment = Alignment.Center) {
                            when {
                                state.isPosting -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                showMic         -> Icon(Icons.Default.Mic, "Запись голосового", tint = Color.White)
                                else            -> Icon(Icons.Default.Send, null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading && state.messages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp,
                    start = 12.dp, end = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    ChannelMessageCard(
                        msg                 = msg,
                        channelId           = channelId,
                        isOwner             = isOwner,
                        currentUserId       = state.currentUserId,
                        comments            = state.expandedComments[msg.id],
                        isLoadingComments   = state.loadingComments.contains(msg.id),
                        commentText         = state.commentTexts[msg.id] ?: "",
                        isSendingComment    = state.sendingComment.contains(msg.id),
                        myReaction          = state.messageReactions[msg.id],
                        getCommentReaction  = { commentId -> state.commentReactions["${msg.id}:$commentId"] },
                        onToggleComments    = { viewModel.toggleComments(channelId, msg.id) },
                        onCommentTextChange = { viewModel.onCommentTextChange(msg.id, it) },
                        onSendComment       = { viewModel.sendComment(channelId, msg.id) },
                        onReact             = { emoji -> viewModel.toggleMessageReaction(msg.id, emoji) },
                        onCommentReact      = { commentId, emoji ->
                            viewModel.toggleCommentReaction(msg.id, commentId, emoji)
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить канал?") },
            text = { Text("Это действие нельзя отменить. Все посты и комментарии будут удалены.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; viewModel.deleteChannel(channelId, onBack) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Удалить") }
            },
            dismissButton = { TextButton({ showDeleteDialog = false }) { Text("Отмена") } }
        )
    }

    if (state.showSubscribersSheet) {
        SubscribersSheet(
            subscribers = state.subscribers,
            isLoading   = state.isLoadingSubscribers,
            onDismiss   = { viewModel.hideSubscribersSheet() },
            onUserClick = { userId -> viewModel.hideSubscribersSheet(); onOpenUserProfile(userId) }
        )
    }

    if (state.showEditSheet) {
        ChannelEditSheet(
            name         = state.editName,
            description  = state.editDescription,
            isUpdating   = state.isUpdating,
            onNameChange = viewModel::onEditNameChange,
            onDescChange = viewModel::onEditDescriptionChange,
            onSave       = { viewModel.saveChannelEdit(channelId) },
            onDismiss    = { viewModel.closeEditSheet() }
        )
    }

    if (state.showInfoSheet) {
        state.channel?.let { ch ->
            ChannelInfoSheet(channel = ch, onDismiss = { viewModel.closeInfoSheet() })
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChannelDetailScreenPreview() {
    WhatsMAXTheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    navigationIcon = { IconButton({}) { Icon(Icons.Default.ArrowBack, null) } },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) { Text("Н", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer) }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Новости WhatsMAX", fontWeight = FontWeight.SemiBold)
                                Text("1250 подписчиков", fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                            }
                        }
                    },
                    actions = {
                        TextButton({}) { Text("Отписаться") }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp,
                    start = 12.dp, end = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text("Обновление v2.0 уже доступно! Добавлены голосовые сообщения, реакции на сообщения и каналы.", fontSize = 15.sp)
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("2026-05-27 18:30", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Visibility, null, modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                        Spacer(Modifier.width(3.dp))
                                        Text("342", style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                    }
                                    TextButton({}, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                                        Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("5", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text("Приветствуем всех новых подписчиков! Здесь мы делимся новостями о проекте WhatsMAX.", fontSize = 15.sp)
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("2026-05-26 14:15", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                    Spacer(Modifier.width(3.dp))
                                    Text("1024", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelAvatar(name: String, avatarUrl: String?, size: Int) {
    if (avatarUrl != null) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier.size(size.dp).clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.firstOrNull()?.uppercase() ?: "#",
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.4f).sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelMessageCard(
    msg: ChannelMessage,
    channelId: String,
    isOwner: Boolean,
    currentUserId: String,
    comments: List<ChannelComment>?,
    isLoadingComments: Boolean,
    commentText: String,
    isSendingComment: Boolean,
    myReaction: String?,
    getCommentReaction: (commentId: String) -> String?,
    onToggleComments: () -> Unit,
    onCommentTextChange: (String) -> Unit,
    onSendComment: () -> Unit,
    onReact: (String) -> Unit,
    onCommentReact: (commentId: String, emoji: String) -> Unit
) {
    var showReactionPicker by remember { mutableStateOf(false) }

    Card(
        Modifier.fillMaxWidth().combinedClickable(
            onDoubleClick = { showReactionPicker = true },
            onClick       = {}
        )
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (msg.type == MessageType.VOICE && msg.fileId != null) {
                VoiceMessagePlayer(
                    audioUrl   = "${BuildConfig.BASE_URL}/files/${msg.fileId}",
                    durationMs = msg.durationMs,
                    waveform   = msg.waveform
                )
            }
            msg.content?.let { Text(it, fontSize = 15.sp) }

            if (myReaction != null) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { onReact(myReaction) } // повторный тап = снять
                ) {
                    Text(
                        text = "$myReaction 1",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    msg.createdAt.take(16).replace("T", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, null, modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        Spacer(Modifier.width(3.dp))
                        Text(msg.views.toString(), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                    IconButton(
                        onClick = { showReactionPicker = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("😊", fontSize = 16.sp)
                    }
                    TextButton(
                        onClick = onToggleComments,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            if (comments != null) Icons.Default.ExpandLess else Icons.Default.ChatBubbleOutline,
                            null, modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            if (msg.commentsCount > 0) "${msg.commentsCount}" else "Комментарии",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (isLoadingComments) {
                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            } else if (comments != null) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                if (comments.isEmpty()) {
                    Text("Нет комментариев", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        modifier = Modifier.padding(bottom = 4.dp))
                } else {
                    comments.forEach { comment ->
                        CommentItem(
                            comment         = comment,
                            currentUserId   = currentUserId,
                            myReaction      = getCommentReaction(comment.id),
                            onReact         = { emoji -> onCommentReact(comment.id, emoji) }
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Spacer(Modifier.height(2.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = onCommentTextChange,
                        placeholder = { Text("Написать комментарий...", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        shape = RoundedCornerShape(20.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = onSendComment,
                        enabled = commentText.isNotBlank() && !isSendingComment,
                        modifier = Modifier.size(40.dp)
                    ) {
                        if (isSendingComment)
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else
                            Icon(Icons.Default.Send, null,
                                tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface.copy(0.3f))
                    }
                }
            }
        }
    }

    if (showReactionPicker) {
        EmojiPickerDialog(
            onReact   = { emoji -> onReact(emoji); showReactionPicker = false },
            onDismiss = { showReactionPicker = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommentItem(
    comment: ChannelComment,
    currentUserId: String,
    myReaction: String?,
    onReact: (String) -> Unit
) {
    val isMine = comment.authorId == currentUserId
    var showReactionPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onDoubleClick = { showReactionPicker = true },
            onClick       = {}
        )
    ) {
        if (comment.authorAvatar != null) {
            AsyncImage(model = comment.authorAvatar, contentDescription = null,
                modifier = Modifier.size(28.dp).clip(CircleShape))
        } else {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape)
                    .background(
                        if (isMine) MaterialTheme.colorScheme.primary.copy(0.2f)
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    comment.authorName.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = if (isMine) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.authorName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if (isMine) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(6.dp))
                Text(comment.createdAt.take(16).replace("T", " "), fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
            }
            Text(comment.content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            if (myReaction != null) {
                Spacer(Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { onReact(myReaction) }
                ) {
                    Text(
                        text = "$myReaction 1",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }

    if (showReactionPicker) {
        EmojiPickerDialog(
            onReact   = { emoji -> onReact(emoji); showReactionPicker = false },
            onDismiss = { showReactionPicker = false }
        )
    }
}

@Composable
private fun EmojiPickerDialog(onReact: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QUICK_EMOJIS.forEach { emoji ->
                    Text(
                        text     = emoji,
                        fontSize = 26.sp,
                        modifier = Modifier.clickable { onReact(emoji) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelEditSheet(
    name: String,
    description: String,
    isUpdating: Boolean,
    onNameChange: (String) -> Unit,
    onDescChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Настройки канала",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Название канала") },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = onDescChange,
                label = { Text("Описание") },
                leadingIcon = { Icon(Icons.Default.Info, null) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && !isUpdating
            ) {
                if (isUpdating)
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else
                    Text("Сохранить")
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Отмена")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelInfoSheet(
    channel: com.whatsmax.domain.model.Channel,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            ChannelAvatar(name = channel.name, avatarUrl = channel.avatarUrl, size = 80)
            Spacer(Modifier.height(12.dp))
            Text(channel.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                "@${channel.handle}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            InfoRow(Icons.Default.Group, "${channel.membersCount} подписчиков")
            if (!channel.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                InfoRow(Icons.Default.Info, channel.description)
            }
            InfoRow(
                Icons.Default.Lock,
                if (channel.isPublic) "Публичный канал" else "Приватный канал"
            )
            if (channel.createdAt.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                InfoRow(Icons.Default.CalendarMonth, "Создан: ${channel.createdAt.take(10)}")
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Закрыть")
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(0.6f))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscribersSheet(
    subscribers: List<User>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onUserClick: (userId: String) -> Unit = {}
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "Подписчики (${subscribers.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            HorizontalDivider()
            if (isLoading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (subscribers.isEmpty()) {
                Text("Нет подписчиков",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            } else {
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(subscribers, key = { it.uid }) { user ->
                        SubscriberItem(user, onClick = { onUserClick(user.uid) })
                        HorizontalDivider(Modifier.padding(start = 60.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriberItem(user: User, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (user.avatarUrl != null) {
            AsyncImage(model = user.avatarUrl, contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape))
        } else {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(user.displayName.firstOrNull()?.uppercase() ?: "?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(user.displayName, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("@${user.username.trimStart('@')}", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(0.3f),
            modifier = Modifier.size(20.dp))
    }
}
