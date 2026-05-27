/** Экран переписки с пузырями сообщений. */
package com.whatsmax.presentation.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.whatsmax.BuildConfig
import com.whatsmax.domain.model.Chat
import com.whatsmax.domain.model.ChatType
import com.whatsmax.domain.model.Message
import com.whatsmax.domain.model.MessageType
import com.whatsmax.presentation.theme.OnlineIndicator
import com.whatsmax.presentation.theme.WhatsMAXTheme
import com.whatsmax.presentation.theme.messageBubbleOther
import com.whatsmax.presentation.theme.messageBubbleOwn
import com.whatsmax.presentation.voice.VoiceMessagePlayer
import com.whatsmax.presentation.voice.VoiceRecorder
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    chatId: String,
    chatName: String,
    onBack: () -> Unit,
    onStartCall: (String) -> Unit,
    onStartVideoCall: (String) -> Unit,
    onViewProfile: (String) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showMessageMenu by remember { mutableStateOf<Message?>(null) }
    var showReactionPicker by remember { mutableStateOf<Message?>(null) }
    var showAttachDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var cameraImageFile by remember { mutableStateOf<File?>(null) }

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

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val tempFile = File(context.cacheDir, "chat_img_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            viewModel.uploadAndSendImage(chatId, tempFile)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageFile?.let { viewModel.uploadAndSendImage(chatId, it) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "chat_cam_${System.currentTimeMillis()}.jpg")
            cameraImageFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(chatId) { viewModel.init(chatId) }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    val peer = state.chat?.members?.firstOrNull { it.userId != state.currentUserId }
    val displayName = when {
        state.chat?.type == ChatType.DIRECT && peer != null -> peer.displayName
        chatName.isNotEmpty() -> chatName
        else -> state.chat?.name ?: "Чат"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") }
                },
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (state.chat?.type == ChatType.DIRECT && peer != null)
                                    Modifier.clickable { onViewProfile(peer.userId) }
                                else Modifier
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (peer?.avatarUrl != null) {
                            AsyncImage(
                                model = peer.avatarUrl,
                                contentDescription = "Аватар $displayName",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayName.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(displayName, fontWeight = FontWeight.SemiBold)
                            if (state.isTyping) Text("печатает...", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary)
                            else if (peer?.isOnline == true)
                                Text("в сети", fontSize = 12.sp, color = OnlineIndicator)
                        }
                    }
                },
                actions = {
                    IconButton({ onStartCall(chatId) }) { Icon(Icons.Default.Phone, "Звонок") }
                    IconButton({ onStartVideoCall(chatId) }) { Icon(Icons.Default.Videocam, "Видеозвонок") }
                }
            )
        },
        bottomBar = {
            Column {
                state.replyToMessage?.let { reply ->
                    ReplyBar(
                        message  = reply,
                        onCancel = { viewModel.setReplyTo(null) }
                    )
                }
                MessageInputBar(
                    text        = state.inputText,
                    onTextChange = viewModel::onInputChange,
                    onSend      = { viewModel.sendMessage(chatId) },
                    onAttach    = { showAttachDialog = true },
                    isSending   = state.isSending,
                    isRecording = isRecordingVoice,
                    onStartRecord = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED) {
                            voiceRecorder.start()
                            isRecordingVoice = true
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onFinishRecord = { cancelled ->
                        if (!isRecordingVoice) return@MessageInputBar
                        isRecordingVoice = false
                        if (cancelled) {
                            voiceRecorder.cancel()
                        } else {
                            val res = voiceRecorder.stop()
                            if (res != null && res.durationMs >= 500) {
                                viewModel.uploadAndSendVoice(chatId, res.file, res.durationMs, res.waveform)
                            } else {
                                res?.file?.delete()
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val chatItems = buildChatItems(state.messages)

            LazyColumn(
                state         = listState,
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp,
                    start = 8.dp, end = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(chatItems, key = { item ->
                    when (item) {
                        is ChatListItem.Header -> "header_${item.dateStr}"
                        is ChatListItem.Msg    -> item.message.id
                    }
                }) { item ->
                    when (item) {
                        is ChatListItem.Header -> DateSeparator(item.dateStr, item.serverToday)
                        is ChatListItem.Msg    -> {
                            val isOwn = item.message.senderId == state.currentUserId
                            MessageBubble(
                                message      = item.message,
                                isOwn        = isOwn,
                                myReaction   = state.messageReactions[item.message.id],
                                resolveFileUrl = { fileId -> "${BuildConfig.BASE_URL}/files/$fileId" },
                                onLongClick  = { showMessageMenu = item.message },
                                onDoubleClick = { showReactionPicker = item.message },
                                onReply      = { viewModel.setReplyTo(item.message) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAttachDialog) {
        AlertDialog(
            onDismissRequest = { showAttachDialog = false },
            title = { Text("Прикрепить фото") },
            text = {
                Column {
                    TextButton(
                        onClick = { showAttachDialog = false; galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Photo, null); Spacer(Modifier.width(8.dp)); Text("Выбрать из галереи")
                    }
                    TextButton(
                        onClick = {
                            showAttachDialog = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                                val file = File(context.cacheDir, "chat_cam_${System.currentTimeMillis()}.jpg")
                                cameraImageFile = file
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Сделать фото")
                    }
                }
            },
            confirmButton = { TextButton({ showAttachDialog = false }) { Text("Отмена") } }
        )
    }

    showReactionPicker?.let { msg ->
        ReactionPickerDialog(
            onReact   = { emoji -> viewModel.toggleReaction(msg.id, emoji); showReactionPicker = null },
            onDismiss = { showReactionPicker = null }
        )
    }

    showMessageMenu?.let { msg ->
        MessageContextMenu(
            message          = msg,
            isOwn            = msg.senderId == state.currentUserId,
            onReply          = { viewModel.setReplyTo(msg); showMessageMenu = null },
            onEdit           = { showMessageMenu = null },
            onDelete         = { viewModel.deleteMessage(chatId, msg.id); showMessageMenu = null },
            onReactWithEmoji = { emoji -> viewModel.toggleReaction(msg.id, emoji) },
            onReact          = { showMessageMenu = null; showReactionPicker = msg },
            onDismiss        = { showMessageMenu = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isOwn: Boolean,
    myReaction: String?,
    resolveFileUrl: (String) -> String,
    onLongClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onReply: () -> Unit
) {
    val bubbleColor = if (isOwn) messageBubbleOwn else messageBubbleOther
    val shape = if (isOwn)
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    else
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

    Box(Modifier.fillMaxWidth(), contentAlignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
        Column(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(shape)
                .background(bubbleColor)
                .combinedClickable(
                    onLongClick   = onLongClick,
                    onDoubleClick = onDoubleClick,
                    onClick       = {}
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (!isOwn) {
                Text(message.senderName, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }

            message.replyToId?.let {
                Surface(color = Color.Black.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp)) {
                    Text("↩ Ответ", modifier = Modifier.padding(6.dp), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
                Spacer(Modifier.height(4.dp))
            }

            when {
                message.isDeleted -> Text("Сообщение удалено", fontStyle = FontStyle.Italic,
                    color = Color.Gray, fontSize = 14.sp)
                message.type == MessageType.CALL -> {
                    val rawContent = message.content ?: ""
                    val displayContent = if (!isOwn && rawContent.startsWith("Нет ответа")) {
                        if (rawContent.contains("видеозвонок")) "Пропущенный видеозвонок"
                        else "Пропущенный звонок"
                    } else rawContent
                    val isMissed = displayContent.startsWith("Пропущенный") ||
                                   (isOwn && rawContent.startsWith("Нет ответа"))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (displayContent.contains("видео", ignoreCase = true) ||
                                              displayContent.contains("Видео")) Icons.Default.Videocam
                                          else Icons.Default.Phone,
                            contentDescription = null,
                            tint = if (isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(displayContent, fontSize = 14.sp,
                            color = if (isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    }
                }
                message.type == MessageType.IMAGE && message.fileId != null -> {
                    val imageUrl = if (message.thumbUrl != null)
                        resolveFileUrl(message.fileId) + "/thumb"
                    else
                        resolveFileUrl(message.fileId)
                    coil.compose.AsyncImage(
                        model = imageUrl,
                        contentDescription = "Изображение",
                        modifier = Modifier.widthIn(max = 240.dp).heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
                message.type == MessageType.VOICE && message.fileId != null -> {
                    VoiceMessagePlayer(
                        audioUrl   = resolveFileUrl(message.fileId),
                        durationMs = message.durationMs,
                        waveform   = message.waveform,
                        tint       = if (isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
                message.type == MessageType.FILE -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(message.fileName ?: "Файл", fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                else -> Text(message.content ?: "", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.End)
            ) {
                if (message.isEdited) Text("изм. ", fontSize = 11.sp, color = Color.Gray)
                Text(
                    text     = message.createdAt.toHHmm(),
                    fontSize = 11.sp,
                    color    = Color.Gray
                )
                if (isOwn) {
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = if (message.readBy.isNotEmpty()) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (message.readBy.isNotEmpty()) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
        if (myReaction != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .offset(y = (-4).dp)
                    .padding(horizontal = 4.dp)
                    .clickable { onDoubleClick() }
            ) {
                Text(
                    text = "$myReaction 1",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        }
    }
}

@Composable
private fun MessageInputBar(
    text: String, onTextChange: (String) -> Unit,
    onSend: () -> Unit, onAttach: () -> Unit, isSending: Boolean,
    isRecording: Boolean,
    onStartRecord: () -> Unit,
    onFinishRecord: (cancelled: Boolean) -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAttach) {
                Icon(Icons.Default.AttachFile, "Прикрепить", tint = MaterialTheme.colorScheme.primary)
            }
            if (isRecording) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Запись... отпустите для отправки", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                }
            } else {
                OutlinedTextField(
                    value         = text,
                    onValueChange = onTextChange,
                    placeholder   = { Text("Сообщение...") },
                    modifier      = Modifier.weight(1f),
                    maxLines      = 5,
                    shape         = RoundedCornerShape(24.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            val showMic = text.isBlank() && !isSending
            val containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
            val baseModifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(containerColor)
            val interactiveModifier = if (showMic) {
                baseModifier.pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onStartRecord()
                            val released = tryAwaitRelease()
                            onFinishRecord(!released)
                        }
                    )
                }
            } else {
                baseModifier.clickable(enabled = !isSending) { onSend() }
            }
            Box(modifier = interactiveModifier, contentAlignment = Alignment.Center) {
                when {
                    isSending -> CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    showMic   -> Icon(Icons.Default.Mic, "Запись голосового", tint = Color.White)
                    else      -> Icon(Icons.Default.Send, "Отправить", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ReplyBar(message: Message, onCancel: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Reply, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(message.senderName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary)
                Text(message.content ?: "...", fontSize = 13.sp, maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Отмена", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MessageContextMenu(
    message: Message, isOwn: Boolean,
    onReply: () -> Unit, onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReactWithEmoji: (String) -> Unit,
    onReact: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                QuickReactionRow(
                    onReact = { emoji -> onReactWithEmoji(emoji); onDismiss() },
                    onMore  = { onReact() }
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                TextButton({ onReply(); onDismiss() }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Reply, null); Spacer(Modifier.width(8.dp)); Text("Ответить")
                }
                if (isOwn && !message.isDeleted) {
                    TextButton({ onEdit(); onDismiss() }, Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, null); Spacer(Modifier.width(8.dp)); Text("Редактировать")
                    }
                    TextButton({ onDelete() }, Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Закрыть") } }
    )
}

private val QUICK_EMOJIS = listOf("👍", "❤️", "😂", "😮", "😢", "🔥")

@Composable
private fun QuickReactionRow(onReact: (String) -> Unit, onMore: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        QUICK_EMOJIS.forEach { emoji ->
            Text(
                text     = emoji,
                fontSize = 24.sp,
                modifier = Modifier.clickable { onReact(emoji) }
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(32.dp).clickable { onMore() }
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("＋", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun ReactionPickerDialog(
    onReact: (String) -> Unit,
    onDismiss: () -> Unit
) {
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

@Composable
private fun DateSeparator(dateStr: String, serverToday: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ) {
            Text(
                text     = dateStr.toDateLabel(serverToday),
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

private sealed class ChatListItem {
    data class Header(val dateStr: String, val serverToday: String) : ChatListItem()
    data class Msg(val message: Message)   : ChatListItem()
}

private fun buildChatItems(messages: List<Message>): List<ChatListItem> = buildList {
    if (messages.isEmpty()) return@buildList
    val today = LocalDate.now().toString()
    var lastDate = ""
    for (msg in messages) {
        val date = msg.createdAt.take(10)
        if (date.length == 10 && date != lastDate) {
            add(ChatListItem.Header(date, today))
            lastDate = date
        }
        add(ChatListItem.Msg(msg))
    }
}

private fun String.toHHmm(): String = try {
    val sep = indexOfFirst { it == 'T' || it == ' ' }
    if (sep >= 0 && length > sep + 5) substring(sep + 1, sep + 6) else ""
} catch (e: Exception) { "" }

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChatScreenPreview() {
    WhatsMAXTheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    navigationIcon = { IconButton(onClick = {}) { Icon(Icons.Default.ArrowBack, "Назад") } },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) { Text("Н", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer) }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Никита", fontWeight = FontWeight.SemiBold)
                                Text("в сети", fontSize = 12.sp, color = OnlineIndicator)
                            }
                        }
                    },
                    actions = {
                        IconButton({}) { Icon(Icons.Default.Phone, "Звонок") }
                        IconButton({}) { Icon(Icons.Default.Videocam, "Видеозвонок") }
                    }
                )
            },
            bottomBar = {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.AttachFile, "Прикрепить", tint = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedTextField(
                            value = "", onValueChange = {},
                            placeholder = { Text("Сообщение...") },
                            modifier = Modifier.weight(1f), maxLines = 5,
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Mic, "Запись", tint = Color.White) }
                    }
                }
            }
        ) { padding ->
            val bubbleOwn = messageBubbleOwn
            val bubbleOther = messageBubbleOther
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp,
                    start = 8.dp, end = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Surface(shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(0.8f)) {
                            Text("Сегодня", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                        }
                    }
                }
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        Column(
                            modifier = Modifier.widthIn(max = 260.dp).clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                                .background(bubbleOther).padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Никита", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            Text("Привет! Как дела?", fontSize = 15.sp)
                            Text("20:35", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))
                        }
                    }
                }
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Column(
                            modifier = Modifier.widthIn(max = 260.dp).clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                                .background(bubbleOwn).padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Hello World!", fontSize = 15.sp)
                            Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                                Text("20:37", fontSize = 11.sp, color = Color.Gray)
                                Spacer(Modifier.width(2.dp))
                                Icon(Icons.Default.DoneAll, null, modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String.toDateLabel(serverToday: String): String {
    return try {
        val msgDate = LocalDate.parse(take(10))
        val today   = try { LocalDate.parse(serverToday) } catch (e: Exception) { LocalDate.now() }
        when (msgDate) {
            today               -> "Сегодня"
            today.minusDays(1)  -> "Вчера"
            else -> {
                val months = arrayOf("","января","февраля","марта","апреля","мая","июня",
                    "июля","августа","сентября","октября","ноября","декабря")
                "${msgDate.dayOfMonth} ${months[msgDate.monthValue]} ${msgDate.year}"
            }
        }
    } catch (e: Exception) { this }
}
