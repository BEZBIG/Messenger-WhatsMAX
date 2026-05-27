/** ViewModel экрана профиля текущего пользователя. */
package com.whatsmax.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.model.User
import com.whatsmax.domain.repository.FileRepository
import com.whatsmax.domain.usecase.auth.GetCurrentUserUseCase
import com.whatsmax.domain.usecase.auth.SignOutUseCase
import com.whatsmax.domain.usecase.user.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val editDisplayName: String = "",
    val editBio: String = "",
    val editUsername: String = "",
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = getCurrentUserUseCase()
            _uiState.update {
                it.copy(
                    user            = user,
                    editDisplayName = user?.displayName ?: "",
                    editBio         = user?.bio ?: "",
                    editUsername    = user?.username ?: "",
                    isLoading       = false
                )
            }
        }
    }

    fun onEditDisplayNameChange(v: String) = _uiState.update { it.copy(editDisplayName = v) }
    fun onEditBioChange(v: String)         = _uiState.update { it.copy(editBio = v) }
    fun onEditUsernameChange(v: String)    = _uiState.update { it.copy(editUsername = v) }
    fun setEditing(editing: Boolean)       = _uiState.update { it.copy(isEditing = editing) }

    fun saveProfile() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val r = updateProfileUseCase(s.editDisplayName.trim(), s.editBio.trim(), s.editUsername.trim())) {
                is Result.Success -> _uiState.update { it.copy(user = r.data, isEditing = false, isSaving = false) }
                is Result.Error   -> _uiState.update { it.copy(error = r.message, isSaving = false) }
                else -> Unit
            }
        }
    }

    fun uploadAvatar(file: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true, error = null) }
            when (val upload = fileRepository.uploadFile(file, "image/jpeg")) {
                is Result.Success -> {
                    val url = upload.data.url
                    when (val r = updateProfileUseCase(null, null, null, avatarUrl = url)) {
                        is Result.Success -> _uiState.update { it.copy(user = r.data, isUploadingAvatar = false) }
                        is Result.Error   -> _uiState.update { it.copy(error = r.message, isUploadingAvatar = false) }
                        else -> Unit
                    }
                }
                is Result.Error -> _uiState.update { it.copy(error = upload.message, isUploadingAvatar = false) }
                else -> Unit
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { signOutUseCase() }
    }
}
