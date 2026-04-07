package com.app.radiosatelital.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.radiosatelital.data.firebase.CloudRadioDocument
import com.app.radiosatelital.data.firebase.RadioRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
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
        val normalizedAdminEmail = repository.adminDefaultEmail()
        val adminEmailSource = repository.adminDefaultEmailSource()
        Log.i(
            TAG,
            "[SettingsAdmin][OPEN] adminEmailEmpty=${normalizedAdminEmail.isBlank()} source='${adminEmailSource}'",
        )

        if (uiState.isAdminLoggedIn) {
            startPendingListener()
        }
    }

    fun loginAsAdmin(email: String, password: String) {
        viewModelScope.launch {
            Log.d(
                TAG,
                "[loginAsAdmin] Start. passwordLength=${password.length}",
            )
            uiState = uiState.copy(isBusy = true, infoMessage = null)
            repository.signInAdmin(email.trim(), password)
                .onSuccess {
                    Log.d(
                        TAG,
                        "[loginAsAdmin] Success.",
                    )
                    uiState = uiState.copy(
                        isBusy = false,
                        isAdminLoggedIn = true,
                        currentUserEmail = repository.currentUserEmail(),
                        infoMessage = "Sesion de administrador iniciada",
                    )
                    startPendingListener()
                }
                .onFailure {
                    Log.e(
                        TAG,
                        "[loginAsAdmin] Failure. type=${it::class.java.simpleName} message='${it.message.orEmpty()}'",
                        it,
                    )
                    uiState = uiState.copy(
                        isBusy = false,
                        isAdminLoggedIn = false,
                        currentUserEmail = repository.currentUserEmail(),
                        infoMessage = mapAdminLoginError(it),
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

    fun updatePendingRadio(radio: CloudRadioDocument) {
        viewModelScope.launch {
            uiState = uiState.copy(isBusy = true, infoMessage = null)
            repository.updateSubmittedRadio(radio)
                .onSuccess {
                    uiState = uiState.copy(isBusy = false, infoMessage = "Radio actualizada")
                }
                .onFailure {
                    uiState = uiState.copy(
                        isBusy = false,
                        infoMessage = "No se pudo actualizar: ${it.message ?: "error"}",
                    )
                }
        }
    }

    fun testStream(streamUrl: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isBusy = true, infoMessage = null)
            repository.testStreamAvailability(streamUrl)
                .onSuccess {
                    uiState = uiState.copy(isBusy = false, infoMessage = "Stream disponible")
                }
                .onFailure {
                    uiState = uiState.copy(
                        isBusy = false,
                        infoMessage = "Stream no disponible: ${it.message ?: "error"}",
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

    private fun mapAdminLoginError(error: Throwable): String {
        val detail = error.message.orEmpty()
        return when {
            detail.contains("Falta ADMIN_EMAIL", ignoreCase = true) -> {
                "No se pudo iniciar sesion: Falta configurar correo administrador"
            }
            detail.contains("No eres administrador", ignoreCase = true) ||
                detail.contains("correo autenticado no coincide", ignoreCase = true) -> {
                "No se pudo iniciar sesion: No eres administrador"
            }
            detail.contains("contrasena", ignoreCase = true) -> {
                "No se pudo iniciar sesion: Ingresa la contrasena"
            }
            error is FirebaseAuthInvalidUserException -> {
                "No se pudo iniciar sesion: El usuario no existe en Firebase Auth"
            }
            error is FirebaseAuthInvalidCredentialsException -> {
                "No se pudo iniciar sesion: Contrasena incorrecta o credencial invalida"
            }
            error is FirebaseAuthException -> {
                when (error.errorCode) {
                    "ERROR_WRONG_PASSWORD" -> {
                        "No se pudo iniciar sesion: Contrasena incorrecta"
                    }
                    "ERROR_USER_NOT_FOUND" -> {
                        "No se pudo iniciar sesion: Usuario no encontrado"
                    }
                    "ERROR_INVALID_EMAIL" -> {
                        "No se pudo iniciar sesion: Correo invalido"
                    }
                    "ERROR_TOO_MANY_REQUESTS" -> {
                        "No se pudo iniciar sesion: Demasiados intentos. Intenta mas tarde"
                    }
                    else -> "No se pudo iniciar sesion: ${error.message ?: "error"}"
                }
            }
            error is FirebaseNetworkException -> {
                "No se pudo iniciar sesion: Sin conexion a internet"
            }
            else -> "No se pudo iniciar sesion: ${error.message ?: "error"}"
        }
    }

    companion object {
        private const val TAG = "AdminLogin"
    }
}
