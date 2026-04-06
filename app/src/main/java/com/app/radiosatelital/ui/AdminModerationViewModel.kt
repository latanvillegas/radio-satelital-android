package com.app.radiosatelital.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.radiosatelital.data.firebase.CloudRadioDocument
import com.app.radiosatelital.data.firebase.RadioRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

data class AdminModerationUiState(
    val adminEmail: String = "",
    val isAdminLoggedIn: Boolean = false,
    val currentUserEmail: String? = null,
    val pendingRadios: List<CloudRadioDocument> = emptyList(),
    val infoMessage: String? = null,
    val isBusy: Boolean = false,
)

class AdminModerationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RadioRepository(application.applicationContext)
    private var pendingListener: ListenerRegistration? = null

    var uiState by mutableStateOf(
        AdminModerationUiState(
        adminEmail = "",
        isAdminLoggedIn = repository.isAdminSessionActive(),
        currentUserEmail = repository.currentUserEmail(),
    )
    )
        private set

    init {
        if (uiState.isAdminLoggedIn) {
            startPendingListener()
        }
    }

    fun loginAsAdmin(email: String, password: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isBusy = true, infoMessage = null)
            repository.signInAdmin(email.trim(), password)
                .onSuccess {
                    uiState = uiState.copy(
                        isBusy = false,
                        isAdminLoggedIn = true,
                        currentUserEmail = repository.currentUserEmail(),
                        infoMessage = "Sesion de administrador iniciada",
                    )
                    startPendingListener()
                }
                .onFailure {
                    uiState = uiState.copy(
                        isBusy = false,
                        isAdminLoggedIn = false,
                        currentUserEmail = repository.currentUserEmail(),
                        infoMessage = "No se pudo iniciar sesion: ${it.message ?: "error"}",
                    )
                }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isBusy = true, infoMessage = null)
            repository.sendAdminPasswordReset(email.trim())
                .onSuccess {
                    uiState = uiState.copy(
                        isBusy = false,
                        infoMessage = "Se envio correo de restablecimiento",
                    )
                }
                .onFailure {
                    uiState = uiState.copy(
                        isBusy = false,
                        infoMessage = "No se pudo enviar correo: ${it.message ?: "error"}",
                    )
                }
        }
    }

    fun logoutAdmin() {
        pendingListener?.remove()
        pendingListener = null
        repository.signOutAdmin()
        uiState = uiState.copy(
            isAdminLoggedIn = false,
            currentUserEmail = repository.currentUserEmail(),
            pendingRadios = emptyList(),
            infoMessage = "Sesion cerrada",
            isBusy = false,
        )
    }

    fun approveRadio(radio: CloudRadioDocument) {
        viewModelScope.launch {
            uiState = uiState.copy(isBusy = true, infoMessage = null)
            repository.approveSubmittedRadio(radio)
                .onSuccess {
                    uiState = uiState.copy(isBusy = false, infoMessage = "Radio aprobada")
                }
                .onFailure {
                    uiState = uiState.copy(
                        isBusy = false,
                        infoMessage = "No se pudo aprobar: ${it.message ?: "error"}",
                    )
                }
        }
    }

    fun rejectRadio(radioId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isBusy = true, infoMessage = null)
            repository.rejectSubmittedRadio(radioId)
                .onSuccess {
                    uiState = uiState.copy(isBusy = false, infoMessage = "Radio rechazada")
                }
                .onFailure {
                    uiState = uiState.copy(
                        isBusy = false,
                        infoMessage = "No se pudo rechazar: ${it.message ?: "error"}",
                    )
                }
        }
    }

    fun clearInfoMessage() {
        if (!uiState.infoMessage.isNullOrBlank()) {
            uiState = uiState.copy(infoMessage = null)
        }
    }

    private fun startPendingListener() {
        pendingListener?.remove()
        pendingListener = repository.observePendingRadios(
            onUpdate = { docs ->
                uiState = uiState.copy(pendingRadios = docs)
            },
            onError = {
                uiState = uiState.copy(infoMessage = "No se pudieron cargar radios pendientes")
            },
        )
    }

    override fun onCleared() {
        pendingListener?.remove()
        pendingListener = null
        super.onCleared()
    }
}
