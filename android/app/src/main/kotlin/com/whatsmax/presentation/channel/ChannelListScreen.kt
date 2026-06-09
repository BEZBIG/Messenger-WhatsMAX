/** Экран списка каналов: подписки, поиск, создание. */
package com.whatsmax.presentation.channel

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.whatsmax.R
import com.whatsmax.presentation.theme.WhatsMAXTheme
import coil.compose.AsyncImage
import com.whatsmax.domain.model.Channel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelListScreen(
    onOpenChannel: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ChannelViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
                title = { Text(stringResource(R.string.channels_title)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton({ showCreateDialog = true }) {
                if (state.isCreating)
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else
                    Icon(Icons.Default.Add, stringResource(R.string.cd_create_channel))
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery, onValueChange = viewModel::onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.search_channels_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = MaterialTheme.shapes.extraLarge, singleLine = true
            )

            val displayList = if (state.searchQuery.length >= 2) state.searchResults else state.myChannels

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (state.searchQuery.isEmpty()) {
                        item { Text(stringResource(R.string.my_channels), fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    }
                    items(displayList, key = { it.id }) { channel ->
                        ChannelItem(channel = channel, onClick = { onOpenChannel(channel.id) })
                        HorizontalDivider(Modifier.padding(start = 72.dp))
                    }
                    if (displayList.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_channels), color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateChannelDialog(
            onCreate = { handle, name, desc, pub ->
                viewModel.createChannel(handle, name, desc, pub)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun ChannelItem(channel: Channel, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (channel.avatarUrl != null) {
            AsyncImage(model = channel.avatarUrl, contentDescription = null,
                modifier = Modifier.size(52.dp).clip(CircleShape))
        } else {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape,
                modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Newspaper, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(channel.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.channel_subtitle, channel.handle.trimStart('@'), channel.membersCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
        }
        if (!channel.isSubscribed) {
            TextButton(onClick = {}) { Text(stringResource(R.string.subscribe)) }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChannelListScreenPreview() {
    WhatsMAXTheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    navigationIcon = { IconButton({}) { Icon(Icons.Default.ArrowBack, null) } },
                    title = { Text("Каналы") }
                )
            },
            floatingActionButton = {
                FloatingActionButton({}) { Icon(Icons.Default.Add, "Создать канал") }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = "", onValueChange = {},
                    placeholder = { Text("Поиск каналов...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    shape = MaterialTheme.shapes.extraLarge, singleLine = true
                )
                Text("Мои каналы", fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                val sampleChannels = listOf(
                    Triple("Новости WhatsMAX", "whatsmax_news", 1250),
                    Triple("Android Dev", "android_dev", 890),
                    Triple("Kotlin Tips", "kotlin_tips", 2100)
                )
                sampleChannels.forEach { (name, handle, members) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape,
                            modifier = Modifier.size(52.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Newspaper, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text("@$handle · $members подписчиков",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        }
                    }
                    HorizontalDivider(Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

@Composable
private fun CreateChannelDialog(
    onCreate: (handle: String, name: String, desc: String, isPublic: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var handle by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_channel)) },
        text = {
            Column {
                OutlinedTextField(value = handle, onValueChange = { handle = it },
                    label = { Text(stringResource(R.string.handle_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.channel_name_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it },
                    label = { Text(stringResource(R.string.channel_desc_hint)) }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPublic) stringResource(R.string.public_label) else stringResource(R.string.private_label))
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (handle.isNotBlank() && name.isNotBlank()) onCreate(handle.trimStart('@'), name, desc, isPublic) }) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
