package com.whatsmax.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsmax.presentation.theme.OnlineIndicator
import com.whatsmax.presentation.theme.WhatsMAXTheme
import com.whatsmax.presentation.theme.messageBubbleOther
import com.whatsmax.presentation.theme.messageBubbleOwn

@Preview(showBackground = true, showSystemUi = true, name = "1. Вход")
@Composable
private fun PreviewLogin() {
    WhatsMAXTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("WhatsMAX", fontSize = 36.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Войдите в аккаунт", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
            Spacer(Modifier.height(40.dp))
            OutlinedTextField(value = "", onValueChange = {},
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = "", onValueChange = {},
                label = { Text("Пароль") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = { Icon(Icons.Default.Visibility, null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Войти", fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = {}) { Text("Нет аккаунта? Зарегистрироваться") }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "2. Регистрация")
@Composable
private fun PreviewRegister() {
    WhatsMAXTheme {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Создать аккаунт", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(value = "", onValueChange = {},
                label = { Text("Имя") }, leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = "", onValueChange = {},
                label = { Text("Username (@)") }, leadingIcon = { Icon(Icons.Default.AlternateEmail, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = "", onValueChange = {},
                label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = "", onValueChange = {},
                label = { Text("Пароль") }, leadingIcon = { Icon(Icons.Default.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = "", onValueChange = {},
                label = { Text("Подтвердите пароль") }, leadingIcon = { Icon(Icons.Default.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Зарегистрироваться", fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = {}) { Text("Уже есть аккаунт? Войти") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true, name = "3. Список чатов")
@Composable
private fun PreviewHome() {
    WhatsMAXTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("WhatsMAX", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton({}) { Icon(Icons.Default.Newspaper, "Каналы") }
                        IconButton({}) { Icon(Icons.Default.Person, "Профиль") }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton({}) { Icon(Icons.Default.Edit, "Новый чат") }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(value = "", onValueChange = {},
                    placeholder = { Text("Поиск чатов и пользователей...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.extraLarge)
                listOf(
                    Triple("Никита", "Привет! Как дела?", "20:37"),
                    Triple("Команда проекта", "Завтра созвон в 10:00", "19:15"),
                    Triple("Мария", "Фото", "18:40")
                ).forEach { (name, msg, time) ->
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    HorizontalDivider(Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true, name = "4. Чат")
@Composable
private fun PreviewChat() {
    WhatsMAXTheme {
        val bubbleOwn = messageBubbleOwn
        val bubbleOther = messageBubbleOther
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { IconButton({}) { Icon(Icons.Default.ArrowBack, null) } },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center) {
                                Text("Н", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Никита", fontWeight = FontWeight.SemiBold)
                                Text("в сети", fontSize = 12.sp, color = OnlineIndicator)
                            }
                        }
                    },
                    actions = {
                        IconButton({}) { Icon(Icons.Default.Phone, null) }
                        IconButton({}) { Icon(Icons.Default.Videocam, null) }
                    }
                )
            },
            bottomBar = {
                Surface(shadowElevation = 8.dp) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton({}) { Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.primary) }
                        OutlinedTextField(value = "", onValueChange = {},
                            placeholder = { Text("Сообщение...") },
                            modifier = Modifier.weight(1f), maxLines = 5, shape = RoundedCornerShape(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Mic, null, tint = Color.White)
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp, start = 8.dp, end = 8.dp),
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
                        Column(Modifier.widthIn(max = 260.dp)
                            .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                            .background(bubbleOther).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("Никита", fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            Text("Привет! Как дела?", fontSize = 15.sp)
                            Text("20:35", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))
                        }
                    }
                }
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Column(Modifier.widthIn(max = 260.dp)
                            .clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                            .background(bubbleOwn).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("Hello World!", fontSize = 15.sp)
                            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                                Text("20:37", fontSize = 11.sp, color = Color.Gray)
                                Spacer(Modifier.width(2.dp))
                                Icon(Icons.Default.DoneAll, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "5. Аудиозвонок")
@Composable
private fun PreviewCallAudio() {
    WhatsMAXTheme {
        Box(Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {
            Row(Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Phone, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(16.dp))
                Text("Аудиозвонок", color = Color.White.copy(0.7f), fontSize = 14.sp)
            }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                    Surface(color = Color(0xFF0088CC), shape = CircleShape, modifier = Modifier.fillMaxSize()) {}
                    Text("Н", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(24.dp))
                Text("Никита", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Вызов...", color = Color.White.copy(0.7f), fontSize = 16.sp)
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                PreviewCallBtn(Icons.Default.Mic, "Выкл. мик.", Color.White.copy(0.2f))
                PreviewCallBtn(Icons.Default.CallEnd, "Завершить", Color.Red, large = true)
                PreviewCallBtn(Icons.Default.VolumeUp, "Динамик", Color.White.copy(0.2f))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "6. Видеозвонок")
@Composable
private fun PreviewCallVideo() {
    WhatsMAXTheme {
        Box(Modifier.fillMaxSize().background(Color(0xFF0D1B2A))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.35f)))
            Row(Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Videocam, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(16.dp))
                Text("Видеозвонок", color = Color.White.copy(0.7f), fontSize = 14.sp)
            }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Никита", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Вызов...", color = Color.White.copy(0.7f), fontSize = 16.sp)
            }
            Box(Modifier.offset(x = 16.dp, y = 120.dp).size(108.dp, 144.dp)
                .clip(RoundedCornerShape(14.dp)).background(Color(0xFF1C2E40)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.VideocamOff, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(32.dp))
            }
            Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)) {
                    PreviewCallBtn(Icons.Default.Videocam, "Камера вкл.", OnlineIndicator)
                    PreviewCallBtn(Icons.Default.Cameraswitch, "Перевернуть", Color.White.copy(0.2f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    PreviewCallBtn(Icons.Default.Mic, "Выкл. мик.", Color.White.copy(0.2f))
                    PreviewCallBtn(Icons.Default.CallEnd, "Завершить", Color.Red, large = true)
                    Box(Modifier.size(56.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true, name = "7. Мой профиль")
@Composable
private fun PreviewProfile() {
    WhatsMAXTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { IconButton({}) { Icon(Icons.Default.ArrowBack, null) } },
                    title = { Text("Мой профиль") },
                    actions = { IconButton({}) { Icon(Icons.Default.Edit, null) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(240.dp)
                    .background(Brush.verticalGradient(listOf(
                        MaterialTheme.colorScheme.primary.copy(0.85f),
                        MaterialTheme.colorScheme.primaryContainer
                    ))).padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(96.dp).clip(CircleShape).background(Color.White.copy(0.3f)),
                            contentAlignment = Alignment.Center) {
                            Text("М", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Максим", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("@maxim", fontSize = 14.sp, color = Color.White.copy(0.85f))
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(OnlineIndicator))
                            Spacer(Modifier.width(5.dp))
                            Text("в сети", fontSize = 12.sp, color = Color.White.copy(0.8f))
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).offset(y = (-16).dp)) {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("КОНТАКТ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(8.dp))
                            PreviewInfoRow(Icons.Default.AlternateEmail, "maxim")
                            PreviewInfoRow(Icons.Default.Email, "maxim@example.com")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.5f))) {
                        Icon(Icons.Default.Logout, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Выйти из аккаунта", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true, name = "8. Профиль пользователя")
@Composable
private fun PreviewUserProfile() {
    WhatsMAXTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { IconButton({}) { Icon(Icons.Default.ArrowBack, null) } },
                    title = { Text("Профиль") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().height(260.dp)
                    .background(Brush.verticalGradient(listOf(
                        MaterialTheme.colorScheme.primary.copy(0.8f),
                        MaterialTheme.colorScheme.primaryContainer
                    ))).padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 24.dp)) {
                        Box(Modifier.size(90.dp).clip(CircleShape).background(Color.White.copy(0.3f)),
                            contentAlignment = Alignment.Center) {
                            Text("Н", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
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
                Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp).offset(y = (-28).dp),
                    shape = RoundedCornerShape(16.dp), tonalElevation = 4.dp, shadowElevation = 4.dp) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly) {
                        PreviewActionBtn(Icons.Default.Message, "Написать")
                        PreviewActionBtn(Icons.Default.Phone, "Позвонить")
                        PreviewActionBtn(Icons.Default.Videocam, "Видео")
                        PreviewActionBtn(Icons.Default.Block, "Заблок.", MaterialTheme.colorScheme.error)
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp)) {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))) {
                        Column(Modifier.padding(16.dp)) {
                            Text("О себе", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Разработчик мобильных приложений", fontSize = 15.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Контакт", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(8.dp))
                            PreviewInfoRow(Icons.Default.AlternateEmail, "nikita")
                            PreviewInfoRow(Icons.Default.Email, "nikita@example.com")
                            PreviewInfoRow(Icons.Default.Circle, "Сейчас в сети", OnlineIndicator)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true, name = "9. Список каналов")
@Composable
private fun PreviewChannelList() {
    WhatsMAXTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { IconButton({}) { Icon(Icons.Default.ArrowBack, null) } },
                    title = { Text("Каналы") }
                )
            },
            floatingActionButton = { FloatingActionButton({}) { Icon(Icons.Default.Add, null) } }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(value = "", onValueChange = {},
                    placeholder = { Text("Поиск каналов...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    shape = MaterialTheme.shapes.extraLarge, singleLine = true)
                Text("Мои каналы", fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                listOf(
                    Triple("Новости WhatsMAX", "whatsmax_news", 1250),
                    Triple("Android Dev", "android_dev", 890),
                    Triple("Kotlin Tips", "kotlin_tips", 2100)
                ).forEach { (name, handle, members) ->
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true, name = "10. Лента канала")
@Composable
private fun PreviewChannelDetail() {
    WhatsMAXTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { IconButton({}) { Icon(Icons.Default.ArrowBack, null) } },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center) {
                                Text("Н", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Новости WhatsMAX", fontWeight = FontWeight.SemiBold)
                                Text("1250 подписчиков", fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                            }
                        }
                    },
                    actions = { TextButton({}) { Text("Отписаться") } }
                )
            }
        ) { padding ->
            LazyColumn(Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp, start = 12.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text("Обновление v2.0 уже доступно! Добавлены голосовые сообщения, реакции и каналы.", fontSize = 15.sp)
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("2026-05-27 18:30", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Visibility, null, Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                        Spacer(Modifier.width(3.dp))
                                        Text("342", style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                    }
                                    TextButton({}, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                                        Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(14.dp))
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
                            Text("Приветствуем новых подписчиков! Здесь мы делимся новостями проекта.", fontSize = 15.sp)
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("2026-05-26 14:15", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Visibility, null, Modifier.size(14.dp),
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
private fun PreviewCallBtn(icon: ImageVector, label: String, color: Color, large: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(color = color, shape = CircleShape, modifier = Modifier.size(if (large) 72.dp else 56.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(if (large) 32.dp else 24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(0.7f), fontSize = 11.sp)
    }
}

@Composable
private fun PreviewActionBtn(icon: ImageVector, label: String, tint: Color = MaterialTheme.colorScheme.primary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = {}, modifier = Modifier.size(52.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = tint.copy(0.12f))) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = tint, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 13.sp)
    }
}

@Composable
private fun PreviewInfoRow(icon: ImageVector, text: String, tint: Color = MaterialTheme.colorScheme.onSurface.copy(0.6f)) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = tint)
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
