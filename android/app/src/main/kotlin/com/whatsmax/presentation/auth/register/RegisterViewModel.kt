/** ViewModel экрана регистрации. */
package com.whatsmax.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.usecase.auth.SignUpWithEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val username: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val signUpUseCase: SignUpWithEmailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    fun onEmailChange(v: String)          = _uiState.update { it.copy(email = v, error = null) }
    fun onPasswordChange(v: String)       = _uiState.update { it.copy(password = v, error = null) }
    fun onConfirmPasswordChange(v: String) = _uiState.update { it.copy(confirmPassword = v, error = null) }
    fun onUsernameChange(v: String)       = _uiState.update { it.copy(username = v, error = null) }
    fun onDisplayNameChange(v: String)    = _uiState.update { it.copy(displayName = v, error = null) }

    fun signUp() {
        val s = _uiState.value
        when {
            s.email.isBlank() || s.password.isBlank() || s.username.isBlank() || s.displayName.isBlank() ->
                _uiState.update { it.copy(error = "Заполните все поля") }
            s.password != s.confirmPassword ->
                _uiState.update { it.copy(error = "Пароли не совпадают") }
            s.password.length < 6 ->
                _uiState.update { it.copy(error = "Пароль должен быть не менее 6 символов") }
            s.username.length < 3 ->
                _uiState.update { it.copy(error = "Username должен быть не менее 3 символов") }
            else -> viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                when (val r = signUpUseCase(s.email.trim(), s.password, s.username.trim(), s.displayName.trim())) {
                    is Result.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                    else -> Unit
                }
            }
        }
    }
}
