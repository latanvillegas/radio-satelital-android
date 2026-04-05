package com.app.radiosatelital.data.firebase

import android.content.Context
import com.app.radiosatelital.ui.UserRadioStation
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class FirebaseRadioDataSource(private val context: Context) {

    suspend fun ensureAnonymousAuth(): Result<String> {
        val auth = authOrNull()
            ?: return Result.failure(IllegalStateException("Firebase no esta configurado"))

        auth.currentUser?.uid?.let { return Result.success(it) }

        return runCatching {
            auth.signInAnonymously().await().user?.uid
                ?: throw IllegalStateException("No se pudo autenticar anonimamente")
        }
    }

    suspend fun submitRadioForReview(radio: UserRadioStation): Result<Unit> {
        val db = firestoreOrNull()
            ?: return Result.failure(IllegalStateException("Firebase no esta configurado"))
        val uid = ensureAnonymousAuth().getOrElse { return Result.failure(it) }

        return runCatching {
            val doc = db.collection("submitted_radios").document()
            val payload = hashMapOf(
                "id" to doc.id,
                "name" to radio.name,
                "streamUrl" to radio.streamUrl,
                "country" to radio.country,
                "region" to radio.region,
                "districtOrCity" to radio.city,
                "continent" to radio.continent,
                "genre" to radio.genre,
                "description" to radio.description,
                "logoUrl" to radio.logoUrl.ifBlank { null },
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "status" to STATUS_PENDING,
                "lastCheckedAt" to null,
            )
            doc.set(payload).await()
        }
    }

    fun observeApprovedPublicRadios(
        onUpdate: (List<CloudRadioDocument>) -> Unit,
        onError: (Throwable) -> Unit,
    ): ListenerRegistration? {
        val db = firestoreOrNull() ?: run {
            onError(IllegalStateException("Firebase no esta configurado"))
            return null
        }

        return db.collection("public_radios")
            .whereEqualTo("status", STATUS_APPROVED)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents
                    ?.mapNotNull { it.toCloudRadioDocumentOrNull() }
                    .orEmpty()
                onUpdate(docs)
            }
    }

    private fun ensureFirebaseApp(): Boolean {
        if (FirebaseApp.getApps(context).isNotEmpty()) return true
        return FirebaseApp.initializeApp(context) != null
    }

    private fun authOrNull(): FirebaseAuth? {
        if (!ensureFirebaseApp()) return null
        return runCatching { FirebaseAuth.getInstance() }.getOrNull()
    }

    private fun firestoreOrNull(): FirebaseFirestore? {
        if (!ensureFirebaseApp()) return null
        return runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }
}
