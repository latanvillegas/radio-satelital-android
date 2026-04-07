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
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Date
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
            "[signInAdmin][INPUT] passwordLength=${password.length} adminEmailEmpty=${isAdminEmailEmpty} adminEmailSource='${adminConfig.source}' projectId='${projectId}'",
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
                    "[signInAdmin][DURING_LOGIN_RESULT] success=true postValidation=${postValidationMatches}",
                )

                if (!postValidationMatches) {
                    Log.e(
                        TAG,
                        "${POST_LOGIN_ADMIN_EMAIL_MISMATCH} authenticatedEmailMismatch=true",
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
                        "${LOGIN_ADMIN_SUCCESS} finalValidation=true",
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

    suspend fun updateSubmittedRadio(radio: CloudRadioDocument): Result<Unit> {
        val db = firestoreOrNull()
            ?: return Result.failure(IllegalStateException("Firebase no esta configurado"))
        val auth = authOrNull()
            ?: return Result.failure(IllegalStateException("Firebase no esta configurado"))

        if (!isAdminSessionActive()) {
            return Result.failure(IllegalStateException("Sesion de administrador no iniciada"))
        }

        val normalizedName = radio.name.trim()
        val normalizedStream = radio.streamUrl.trim()
        if (normalizedName.isBlank() || normalizedStream.isBlank()) {
            return Result.failure(IllegalArgumentException("Nombre y stream son obligatorios"))
        }

        return runCatching {
            db.collection("submitted_radios")
                .document(radio.id)
                .update(
                    mapOf(
                        "name" to normalizedName,
                        "streamUrl" to normalizedStream,
                        "country" to radio.country.trim(),
                        "region" to radio.region.trim(),
                        "districtOrCity" to radio.districtOrCity.trim(),
                        "continent" to radio.continent.trim(),
                        "genre" to radio.genre.trim(),
                        "description" to radio.description.trim(),
                        "logoUrl" to radio.logoUrl?.trim(),
                        "homepageUrl" to radio.homepageUrl?.trim(),
                        "lastCheckedAt" to FieldValue.serverTimestamp(),
                        "lastCheckedBy" to auth.currentUser?.uid,
                    ),
                )
                .await()
        }
    }

    suspend fun testStreamAvailability(streamUrl: String): Result<Unit> {
        val normalizedStream = streamUrl.trim()
        if (normalizedStream.isBlank()) {
            return Result.failure(IllegalArgumentException("Ingresa un stream valido"))
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(normalizedStream).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Icy-MetaData", "1")
                }
                try {
                    connection.connect()
                    val responseCode = connection.responseCode
                    if (responseCode !in 200..399) {
                        throw IllegalStateException("Stream no disponible. HTTP $responseCode")
                    }

                    val contentType = connection.contentType.orEmpty().lowercase(Locale.ROOT)
                    val looksLikeAudio = contentType.contains("audio/") ||
                        contentType.contains("application/ogg") ||
                        contentType.contains("application/octet-stream") ||
                        contentType.contains("audio/aacp")

                    val bytesRead = runCatching {
                        connection.inputStream.use { input ->
                            val probe = ByteArray(512)
                            input.read(probe)
                        }
                    }.getOrDefault(-1)

                    if (bytesRead <= 0) {
                        throw IllegalStateException("Stream responde pero no entrega audio")
                    }

                    if (!looksLikeAudio && bytesRead < 16) {
                        throw IllegalStateException(
                            "Stream responde pero no parece audio. contentType=${contentType.ifBlank { "desconocido" }}",
                        )
                    }
                } finally {
                    connection.disconnect()
                }
            }
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

    fun observeLiveListenerCounts(
        onUpdate: (Map<String, Int>) -> Unit,
        onError: (Throwable) -> Unit,
    ): ListenerRegistration? {
        val db = firestoreOrNull() ?: run {
            onError(IllegalStateException("Firebase no esta configurado"))
            return null
        }

        return db.collection("radio_live_listeners")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val counts = snapshot?.documents
                    ?.mapNotNull { doc ->
                        val streamUrl = doc.getString("streamUrl")?.trim().orEmpty()
                        if (streamUrl.isBlank()) return@mapNotNull null
                        val updatedAt = doc.getTimestamp("updatedAt")
                        val now = Date()
                        val isFresh = updatedAt?.toDate()?.let {
                            now.time - it.time <= LISTENER_STALE_MS
                        } ?: false
                        val listeners = (doc.getLong("listeners") ?: 0L).coerceAtLeast(0L)
                        streamUrl to if (isFresh) listeners.toInt() else 0
                    }
                    ?.toMap()
                    .orEmpty()

                onUpdate(counts)
            }
    }

    suspend fun updateLiveListeners(streamUrl: String, delta: Int): Result<Unit> {
        val db = firestoreOrNull()
            ?: return Result.failure(IllegalStateException("Firebase no esta configurado"))
        val normalizedUrl = streamUrl.trim()
        if (normalizedUrl.isBlank()) return Result.success(Unit)

        return runCatching {
            val docId = stableStationId(normalizedUrl)
            val ref = db.collection("radio_live_listeners").document(docId)
            if (delta == 0) {
                db.collection("radio_live_listeners")
                    .document(docId)
                    .set(
                        mapOf(
                            "streamUrl" to normalizedUrl,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    )
                    .await()
                return@runCatching
            }

            db.runTransaction { transaction: Transaction ->
                val snapshot = transaction.get(ref)
                val current = (snapshot.getLong("listeners") ?: 0L).coerceAtLeast(0L)
                val updated = (current + delta).coerceAtLeast(0L)

                val payload = mapOf(
                    "streamUrl" to normalizedUrl,
                    "listeners" to updated,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                transaction.set(ref, payload, SetOptions.merge())
                null
            }.await()
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
        private const val LISTENER_STALE_MS = 90_000L
        private const val MANIFEST_ADMIN_EMAIL_KEY = "ADMIN_EMAIL"
        private const val PRE_LOGIN_FAIL_ADMIN_EMAIL_EMPTY = "PRE_LOGIN_FAIL_ADMIN_EMAIL_EMPTY"
        private const val DURING_LOGIN_FIREBASE_USER_NOT_FOUND = "DURING_LOGIN_FIREBASE_USER_NOT_FOUND"
        private const val DURING_LOGIN_FIREBASE_INVALID_CREDENTIALS = "DURING_LOGIN_FIREBASE_INVALID_CREDENTIALS"
        private const val DURING_LOGIN_FIREBASE_OTHER_ERROR = "DURING_LOGIN_FIREBASE_OTHER_ERROR"
        private const val POST_LOGIN_ADMIN_EMAIL_MISMATCH = "POST_LOGIN_ADMIN_EMAIL_MISMATCH"
        private const val LOGIN_ADMIN_SUCCESS = "LOGIN_ADMIN_SUCCESS"
    }

    private fun stableStationId(streamUrl: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(streamUrl.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
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
