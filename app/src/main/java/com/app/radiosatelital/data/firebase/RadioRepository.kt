package com.app.radiosatelital.data.firebase

import android.content.Context
import com.app.radiosatelital.RadioStation
import com.app.radiosatelital.ui.UserRadioStation
import com.google.firebase.firestore.ListenerRegistration

class RadioRepository(context: Context) {
    private val dataSource = FirebaseRadioDataSource(context)

    suspend fun authenticateAnonymously(): Result<String> = dataSource.ensureAnonymousAuth()

    suspend fun submitRadioForModeration(radio: UserRadioStation): Result<Unit> {
        return dataSource.submitRadioForReview(radio)
    }

    fun observePublicApprovedRadios(
        onUpdate: (List<RadioStation>) -> Unit,
        onError: (Throwable) -> Unit,
    ): ListenerRegistration? {
        return dataSource.observeApprovedPublicRadios(
            onUpdate = { docs -> onUpdate(docs.map { it.toRadioStation() }) },
            onError = onError,
        )
    }

    fun observeLiveListenerCounts(
        onUpdate: (Map<String, Int>) -> Unit,
        onError: (Throwable) -> Unit,
    ): ListenerRegistration? {
        return dataSource.observeLiveListenerCounts(onUpdate = onUpdate, onError = onError)
    }

    suspend fun updateLiveListeners(streamUrl: String, delta: Int): Result<Unit> {
        return dataSource.updateLiveListeners(streamUrl = streamUrl, delta = delta)
    }

    fun adminDefaultEmail(): String = dataSource.adminConfiguredEmail()

    fun adminDefaultEmailSource(): String = dataSource.adminConfiguredEmailSource()

    fun isAdminSessionActive(): Boolean = dataSource.isAdminSessionActive()

    fun currentUserEmail(): String? = dataSource.currentUserEmail()

    suspend fun signInAdmin(email: String, password: String): Result<Unit> {
        return dataSource.signInAdmin(email = email, password = password)
    }

    suspend fun sendAdminPasswordReset(email: String): Result<Unit> {
        return dataSource.sendAdminPasswordReset(email)
    }

    fun signOutAdmin() {
        dataSource.signOutAdmin()
    }

    fun observePendingRadios(
        onUpdate: (List<CloudRadioDocument>) -> Unit,
        onError: (Throwable) -> Unit,
    ): ListenerRegistration? {
        return dataSource.observePendingRadios(onUpdate = onUpdate, onError = onError)
    }

    fun observeSubmittedRadiosByUser(
        createdBy: String,
        onUpdate: (Map<String, String>) -> Unit,
        onError: (Throwable) -> Unit,
    ): ListenerRegistration? {
        return dataSource.observeSubmittedRadiosByUser(
            createdBy = createdBy,
            onUpdate = onUpdate,
            onError = onError,
        )
    }

    suspend fun approveSubmittedRadio(radio: CloudRadioDocument): Result<Unit> {
        return dataSource.approveSubmittedRadio(radio)
    }

    suspend fun rejectSubmittedRadio(radioId: String): Result<Unit> {
        return dataSource.rejectSubmittedRadio(radioId)
    }

    suspend fun updateSubmittedRadio(radio: CloudRadioDocument): Result<Unit> {
        return dataSource.updateSubmittedRadio(radio)
    }

    suspend fun testStreamAvailability(streamUrl: String): Result<Unit> {
        return dataSource.testStreamAvailability(streamUrl)
    }
}
