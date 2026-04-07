package com.app.radiosatelital.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.radiosatelital.RadioStation
import com.app.radiosatelital.data.firebase.RadioRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

data class RadioCloudUiState(
    val publicRadios: List<RadioStation> = emptyList(),
    val liveListenersByUrl: Map<String, Int> = emptyMap(),
    val authUid: String? = null,
    val infoMessage: String? = null,
)

class RadioCloudViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RadioRepository(application.applicationContext)
    private var listenerRegistration: ListenerRegistration? = null
    private var listenerCountsRegistration: ListenerRegistration? = null

    var uiState = RadioCloudUiState()
        private set

    init {
        viewModelScope.launch {
            repository.authenticateAnonymously()
                .onSuccess { uid ->
                    uiState = uiState.copy(authUid = uid)
                }
                .onFailure {
                    uiState = uiState.copy(infoMessage = "Firebase no disponible: trabajando solo en modo local")
                }

            listenerRegistration = repository.observePublicApprovedRadios(
                onUpdate = { radios ->
                    uiState = uiState.copy(publicRadios = radios)
                },
                onError = {
                    uiState = uiState.copy(infoMessage = "No se pudieron sincronizar radios publicas")
                },
            )

            listenerCountsRegistration = repository.observeLiveListenerCounts(
                onUpdate = { counts ->
                    uiState = uiState.copy(liveListenersByUrl = counts)
                },
                onError = {
                    uiState = uiState.copy(infoMessage = "No se pudo sincronizar conteo de oyentes")
                },
            )
        }
    }

    fun submitUserRadio(station: UserRadioStation) {
        viewModelScope.launch {
            repository.submitRadioForModeration(station)
                .onSuccess {
                    uiState = uiState.copy(infoMessage = "Radio enviada para revision")
                }
                .onFailure {
                    uiState = uiState.copy(infoMessage = "No se pudo enviar a la nube: ${it.message ?: "error"}")
                }
        }
    }

    fun clearInfoMessage() {
        if (uiState.infoMessage != null) {
            uiState = uiState.copy(infoMessage = null)
        }
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        listenerRegistration = null
        listenerCountsRegistration?.remove()
        listenerCountsRegistration = null
        super.onCleared()
    }
}
