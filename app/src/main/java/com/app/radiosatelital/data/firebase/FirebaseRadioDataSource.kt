package com.app.radiosatelital.data.firebase

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.app.radiosatelital.BuildConfig
import com.app.radiosatelital.ui.UserRadioStation
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Locale

class FirebaseRadioDataSource(private val context: Context) {

    fun adminConfiguredEmail(): String = resolveAdminEmailConfig().email

    fun adminConfiguredEmailSource(): String = resolveAdminEmailConfig().source

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
        val adminEmail = adminConfiguredEmail()
        if (adminEmail.isBlank()) return false
        val currentEmail = normalizeEmail(auth.currentUser?.email)
        return currentEmail == adminEmail
    }

    fun currentUserEmail(): String? {
        val auth = authOrNull() ?: return null
        return auth.currentUser?.email
    }

    suspend fun signInAdmin(email: String, password: String): Result<Unit> {
        val auth = authOrNull()
            ?: return Result.failure(IllegalStateException("Firebase no esta configurado"))
        val adminConfig = resolveAdminEmailConfig()
        val adminEmail = adminConfig.email
        val normalizedInputEmail = normalizeEmail(email)
        val projectId = FirebaseApp.getInstance().options.projectId.orEmpty()
        val isAdminEmailEmpty = adminEmail.isBlank()

        Log.d(
            TAG,
            "[signInAdmin][INPUT] inputEmail='${normalizedInputEmail}' passwordLength=${password.length} adminEmail='${adminEmail}' adminEmailEmpty=${isAdminEmailEmpty} adminEmailSource='${adminConfig.source}' projectId='${projectId}'",
        )

        if (isAdminEmailEmpty) {
            Log.e(
                TAG,
                "${PRE_LOGIN_FAIL_ADMIN_EMAIL_EMPTY} ADMIN_EMAIL vacio. source='${adminConfig.source}'",
            )
            return Result.failure(
                IllegalStateException(
                    "Falta ADMIN_EMAIL. Configuralo como propiedad Gradle local para habilitar acceso admin.",
                ),
            )
        }

        if (password.isBlank()) {
            Log.e(TAG, "[signInAdmin][PRE_LOGIN_FAIL] Password vacio")
            return Result.failure(IllegalArgumentException("La contrasena es obligatoria"))
        }

        Log.d(TAG, "[signInAdmin][DURING_LOGIN] Invocando signInWithEmailAndPassword")
        return runCatching {
            auth.signInWithEmailAndPassword(normalizedInputEmail, password).await()
            Unit
        }.fold(
            onSuccess = {
                val currentEmail = normalizeEmail(auth.currentUser?.email)
                val postValidationMatches = currentEmail == adminEmail
                Log.d(
                    TAG,
                    "[signInAdmin][DURING_LOGIN_RESULT] success=true authenticatedEmail='${currentEmail}' adminEmail='${adminEmail}'",
                )

                if (!postValidationMatches) {
                    Log.e(
                        TAG,
                        "${POST_LOGIN_ADMIN_EMAIL_MISMATCH} authenticatedEmail='${currentEmail}' adminEmail='${adminEmail}'",
                    )
                    auth.signOut()
                    Result.failure(
                        IllegalArgumentException(
                            "No eres administrador. El correo autenticado no coincide con ADMIN_EMAIL",
                        ),
                    )
                } else {
                    Log.d(
                        TAG,
                        "${LOGIN_ADMIN_SUCCESS} authenticatedEmail='${currentEmail}' finalValidation=true",
                    )
                    Result.success(Unit)
                }
            },
            onFailure = { throwable ->
                val authException = throwable as? FirebaseAuthException
                val errorCode = authException?.errorCode ?: "N/A"
                val normalizedCode = errorCode.uppercase(Locale.ROOT)
                val label = when {
                    throwable is FirebaseAuthInvalidUserException ||
                        normalizedCode == "ERROR_USER_NOT_FOUND" -> DURING_LOGIN_FIREBASE_USER_NOT_FOUND

                    throwable is FirebaseAuthInvalidCredentialsException ||
                        normalizedCode == "ERROR_WRONG_PASSWORD" ||
                        normalizedCode == "ERROR_INVALID_CREDENTIAL" ||
                        normalizedCode == "ERROR_INVALID_EMAIL" -> DURING_LOGIN_FIREBASE_INVALID_CREDENTIALS

                    else -> DURING_LOGIN_FIREBASE_OTHER_ERROR
                }

                Log.e(
                    TAG,
                    "${label} type=${throwable::class.java.simpleName} errorCode='${errorCode}' message='${throwable.message.orEmpty()}'",
                    throwable,
                )
                Result.failure(throwable)
            },
        )
    }

    suspend fun sendAdminPasswordReset(email: String): Result<Unit> {
        val auth = authOrNull()
            ?: return Result.failure(IllegalStateException("Firebase no esta configurado"))
        val adminEmail = adminConfiguredEmail()
        val normalizedInputEmail = normalizeEmail(email)

        if (adminEmail.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    "Falta ADMIN_EMAIL. Configuralo como propiedad Gradle local para restablecer clave.",
                ),
            )
        }

        if (normalizedInputEmail != adminEmail) {
            return Result.failure(IllegalArgumentException("Solo el correo administrador puede restablecerse"))
        }

        return runCatching {
            auth.sendPasswordResetEmail(adminEmail).await()
            Unit
        }
    }

    private fun normalizeEmail(raw: String?): String {
        val trimmed = raw.orEmpty().trim().trim('"')
        return trimmed.lowercase(Locale.ROOT)
    }

    private fun resolveAdminEmailConfig(): AdminEmailConfig {
        val fromBuildConfig = normalizeEmail(BuildConfig.ADMIN_EMAIL)
        if (fromBuildConfig.isNotBlank()) {
            return AdminEmailConfig(fromBuildConfig, "BuildConfig(${BuildConfig.ADMIN_EMAIL_SOURCE})")
        }

        val fromManifest = readAdminEmailFromManifest()
        if (fromManifest.isNotBlank()) {
            return AdminEmailConfig(fromManifest, "ManifestMetaData(ADMIN_EMAIL)")
        }

        return AdminEmailConfig("", "EMPTY(BuildConfigSource=${BuildConfig.ADMIN_EMAIL_SOURCE})")
    }

    private fun readAdminEmailFromManifest(): String {
        return runCatching {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA,
            )
            normalizeEmail(appInfo.metaData?.getString(MANIFEST_ADMIN_EMAIL_KEY))
        }.getOrDefault("")
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

    companion object {
        private const val TAG = "AdminLogin"
        private const val MANIFEST_ADMIN_EMAIL_KEY = "ADMIN_EMAIL"
        private const val PRE_LOGIN_FAIL_ADMIN_EMAIL_EMPTY = "PRE_LOGIN_FAIL_ADMIN_EMAIL_EMPTY"
        private const val DURING_LOGIN_FIREBASE_USER_NOT_FOUND = "DURING_LOGIN_FIREBASE_USER_NOT_FOUND"
        private const val DURING_LOGIN_FIREBASE_INVALID_CREDENTIALS = "DURING_LOGIN_FIREBASE_INVALID_CREDENTIALS"
        private const val DURING_LOGIN_FIREBASE_OTHER_ERROR = "DURING_LOGIN_FIREBASE_OTHER_ERROR"
        private const val POST_LOGIN_ADMIN_EMAIL_MISMATCH = "POST_LOGIN_ADMIN_EMAIL_MISMATCH"
        private const val LOGIN_ADMIN_SUCCESS = "LOGIN_ADMIN_SUCCESS"
    }

    private data class AdminEmailConfig(
        val email: String,
        val source: String,
    )

    private fun firestoreOrNull(): FirebaseFirestore? {
        if (!ensureFirebaseApp()) return null
        return runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }
}
