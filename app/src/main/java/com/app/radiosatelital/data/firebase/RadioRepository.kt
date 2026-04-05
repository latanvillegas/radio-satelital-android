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
}
