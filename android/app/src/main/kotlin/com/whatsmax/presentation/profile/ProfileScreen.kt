/** Экран профиля текущего пользователя. */
package com.whatsmax.presentation.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.whatsmax.presentation.theme.OnlineIndicator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAvatarDialog by remember { mutableStateOf(false) }
    var cameraAvatarFile by remember { mutableStateOf<File?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val tempFile = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            viewModel.uploadAvatar(tempFile)
        }
    }

    val cameraAvatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraAvatarFile?.let { viewModel.uploadAvatar(it) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "avatar_cam_${System.currentTimeMillis()}.jpg")
            cameraAvatarFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraAvatarLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val file = File(context.cacheDir, "avatar_cam_${System.currentTimeMillis()}.jpg")
            cameraAvatarFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraAvatarLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
                title = { Text("Мой профиль") },
                actions = {
                    if (state.isEditing) {
                        IconButton(viewModel::saveProfile) {
                            if (state.isSaving)
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else
                                Icon(Icons.Default.Check, "Сохранить")
                        }
                        IconButton({ viewModel.setEditing(false) }) {
                            Icon(Icons.Default.Close, "Отмена")
                        }
                    } else {
                        IconButton({ viewModel.setEditing(true) }) {
                            Icon(Icons.Default.Edit, "Редактировать")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        if (state.user?.avatarUrl != null) {
                            AsyncImage(
                                model = state.user!!.avatarUrl,
                                contentDescription = "Аватар",
                                modifier = Modifier.size(96.dp).clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(96.dp).clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.user?.displayName?.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 38.sp, fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        if (state.isEditing) {
                            SmallFloatingActionButton(
                                onClick = { showAvatarDialog = true },
                                modifier = Modifier.size(34.dp),
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                if (state.isUploadingAvatar)
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                                else
                                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp), tint = Color.White)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (!state.isEditing) {
                        Text(
                            state.user?.displayName ?: "",
                            fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (!state.user?.username.isNullOrBlank()) {
                            Text(
                                "@${state.user!!.username}",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(8.dp).clip(CircleShape)
                                    .background(OnlineIndicator)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "в сети",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    } else {
                        Text(
                            "Редактирование профиля",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-16).dp)
            ) {
                if (state.isEditing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "ДАННЫЕ ПРОФИЛЯ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = state.editDisplayName,
                                onValueChange = viewModel::onEditDisplayNameChange,
                                label = { Text("Имя") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = state.editUsername,
                                onValueChange = viewModel::onEditUsernameChange,
                                label = { Text("Username") },
                                leadingIcon = { Icon(Icons.Default.AlternateEmail, null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = state.editBio,
                                onValueChange = viewModel::onEditBioChange,
                                label = { Text("О себе") },
                                leadingIcon = { Icon(Icons.Default.Notes, null) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 4
                            )
                        }
                    }
                } else {
                    if (!state.user?.bio.isNullOrBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "О СЕБЕ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = state.user!!.bio!!,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "КОНТАКТ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            if (!state.user?.username.isNullOrBlank()) {
                                ProfileInfoRow(Icons.Default.AlternateEmail, state.user!!.username.trimStart('@'))
                            }
                            if (!state.user?.phone.isNullOrBlank()) {
                                ProfileInfoRow(Icons.Default.Phone, state.user!!.phone!!)
                            }
                            if (!state.user?.email.isNullOrBlank()) {
                                ProfileInfoRow(Icons.Default.Email, state.user!!.email!!)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { viewModel.signOut(); onSignOut() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Выйти из аккаунта", fontWeight = FontWeight.Medium)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            icon = { Icon(Icons.Default.CameraAlt, null) },
            title = { Text("Фото профиля") },
            text = {
                Column {
                    TextButton(
                        onClick = { showAvatarDialog = false; imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Photo, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Выбрать из галереи")
                    }
                    TextButton(
                        onClick = { showAvatarDialog = false; launchCamera() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Сделать фото")
                    }
                }
            },
            confirmButton = { TextButton({ showAvatarDialog = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun ProfileInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
