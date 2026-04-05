package com.app.radiosatelital.data.firebase

import android.content.Context
import com.app.radiosatelital.ui.UserRadioStation
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirebaseRadioDataSource(private val context: Context) {

    companion object {
        const val ADMIN_EMAIL = "alv.oficial123@gmail.com"
        private const val ADMIN_PASSWORD = "Avelinopas26"
    }

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

    fun isAdminSessionActive(): Boolean {
        val auth = authOrNull() ?: return false
        return auth.currentUser?.email.equals(ADMIN_EMAIL, ignoreCase = true)
    }

    fun currentUserEmail(): String? {
        val auth = authOrNull() ?: return null
        return auth.currentUser?.email
    }

    suspend fun signInAdmin(email: String, password: String): Result<Unit> {
        val auth = authOrNull()
            ?: return Result.failure(IllegalStateException("Firebase no esta configurado"))

        if (!email.equals(ADMIN_EMAIL, ignoreCase = true) || password != ADMIN_PASSWORD) {
            return Result.failure(IllegalArgumentException("Credenciales de administrador invalidas"))
        }

        return runCatching {
            auth.signInWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD).await()
            Unit
        }
    }

    suspend fun sendAdminPasswordReset(email: String): Result<Unit> {
        val auth = authOrNull()
            ?: return Result.failure(IllegalStateException("Firebase no esta configurado"))

        if (!email.equals(ADMIN_EMAIL, ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("Solo el correo administrador puede restablecerse"))
        }

        return runCatching {
            auth.sendPasswordResetEmail(ADMIN_EMAIL).await()
            Unit
        }
    }

    fun signOutAdmin() {
        authOrNull()?.signOut()
    }

    fun observePendingRadios(
        onUpdate: (List<CloudRadioDocument>) -> Unit,
        onError: (Throwable) -> Unit,
    ): ListenerRegistration? {
        val db = firestoreOrNull() ?: run {
            onError(IllegalStateException("Firebase no esta configurado"))
            return null
        }

        return db.collection("submitted_radios")
            .whereEqualTo("status", STATUS_PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val docs = snapshot?.documents
                    ?.mapNotNull { it.toCloudRadioDocumentOrNull() }
                    .orEmpty()
                    .sortedBy { it.name.lowercase() }
                onUpdate(docs)
            }
    }

    suspend fun approveSubmittedRadio(radio: CloudRadioDocument): Result<Unit> {
        val db = firestoreOrNull()
            ?: return Result.failure(IllegalStateException("Firebase no esta configurado"))
        val auth = authOrNull()
            ?: return Result.failure(IllegalStateException("Firebase no esta configurado"))

        if (!isAdminSessionActive()) {
            return Result.failure(IllegalStateException("Sesion de administrador no iniciada"))
        }

        return runCatching {
            val submittedRef = db.collection("submitted_radios").document(radio.id)
            val publicRef = db.collection("public_radios").document(radio.id)
            val approvedBy = auth.currentUser?.uid.orEmpty()

            val publicPayload = hashMapOf(
                "id" to radio.id,
                "name" to radio.name,
                "streamUrl" to radio.streamUrl,
                "country" to radio.country,
                "region" to radio.region,
                "districtOrCity" to radio.districtOrCity,
                "continent" to radio.continent,
                "genre" to radio.genre,
                "description" to radio.description,
                "logoUrl" to radio.logoUrl,
                "faviconUrl" to radio.faviconUrl,
                "homepageUrl" to radio.homepageUrl,
                "createdBy" to radio.createdBy,
                "status" to STATUS_APPROVED,
                "approvedAt" to FieldValue.serverTimestamp(),
                "approvedBy" to approvedBy,
            )

            val submittedPayload = hashMapOf(
                "status" to STATUS_APPROVED,
                "reviewedAt" to FieldValue.serverTimestamp(),
                "reviewedBy" to approvedBy,
            )

            val batch = db.batch()
            batch.set(publicRef, publicPayload, SetOptions.merge())
            batch.update(submittedRef, submittedPayload as Map<String, Any>)
            batch.commit().await()
        }
    }

    suspend fun rejectSubmittedRadio(radioId: String): Result<Unit> {
        val db = firestoreOrNull()
            ?: return Result.failure(IllegalStateException("Firebase no esta configurado"))
        val auth = authOrNull()
            ?: return Result.failure(IllegalStateException("Firebase no esta configurado"))

        if (!isAdminSessionActive()) {
            return Result.failure(IllegalStateException("Sesion de administrador no iniciada"))
        }

        return runCatching {
            db.collection("submitted_radios")
                .document(radioId)
                .update(
                    mapOf(
                        "status" to STATUS_REJECTED,
                        "reviewedAt" to FieldValue.serverTimestamp(),
                        "reviewedBy" to auth.currentUser?.uid,
                    ),
                )
                .await()
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
