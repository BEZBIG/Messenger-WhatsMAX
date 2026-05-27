/** ViewModel экрана входа. */
package com.whatsmax.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.usecase.auth.SignInWithEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Состояние UI экрана входа. */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

/** Управляет формой входа и вызывает SignInWithEmailUseCase. */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInUseCase: SignInWithEmailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEmailChange(email: String)       = _uiState.update { it.copy(email = email, error = null) }
    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password, error = null) }

    fun signIn() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Заполните все поля") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = signInUseCase(state.email.trim(), state.password)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> Unit
            }
        }
    }
}
