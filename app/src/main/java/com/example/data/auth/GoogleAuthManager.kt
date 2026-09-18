package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

class GoogleAuthManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val credentialManager = CredentialManager.create(context)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        loadPersistedUser()
    }

    fun getSavedUser(): UserAccount? {
        return (_authState.value as? AuthState.Authenticated)?.user
    }

    private fun loadPersistedUser() {
        val userJson = prefs.getString(KEY_USER_DATA, null)
        if (!userJson.isNullOrBlank()) {
            try {
                val json = JSONObject(userJson)
                val user = UserAccount(
                    uid = json.optString("uid", UUID.randomUUID().toString()),
                    displayName = json.optString("displayName", "Pengguna UangKu"),
                    email = json.optString("email", ""),
                    photoUrl = json.optString("photoUrl").ifEmpty { null },
                    idToken = json.optString("idToken").ifEmpty { null },
                    isGoogleUser = json.optBoolean("isGoogleUser", true),
                    lastLoginMillis = json.optLong("lastLoginMillis", System.currentTimeMillis())
                )
                _authState.value = AuthState.Authenticated(user)
                return
            } catch (e: Exception) {
                Log.e(TAG, "Gagal memuat sesi pengguna tersimpan", e)
            }
        }
        _authState.value = AuthState.Unauthenticated
    }

    private fun persistUser(user: UserAccount) {
        val json = JSONObject().apply {
            put("uid", user.uid)
            put("displayName", user.displayName)
            put("email", user.email)
            put("photoUrl", user.photoUrl ?: "")
            put("idToken", user.idToken ?: "")
            put("isGoogleUser", user.isGoogleUser)
            put("lastLoginMillis", user.lastLoginMillis)
        }
        prefs.edit().putString(KEY_USER_DATA, json.toString()).apply()
        _authState.value = AuthState.Authenticated(user)
    }

    private fun clearPersistedUser() {
        prefs.edit().remove(KEY_USER_DATA).apply()
        _authState.value = AuthState.Unauthenticated
    }

    fun getWebClientId(): String {
        val saved = prefs.getString(KEY_WEB_CLIENT_ID, "") ?: ""
        if (saved.isNotBlank()) return saved
        // Check BuildConfig if available via reflection or default
        return try {
            val field = BuildConfig::class.java.getField("GOOGLE_WEB_CLIENT_ID")
            (field.get(null) as? String)?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun saveWebClientId(clientId: String) {
        prefs.edit().putString(KEY_WEB_CLIENT_ID, clientId.trim()).apply()
    }

    suspend fun signInWithGoogle(
        activityContext: Context,
        serverClientIdOverride: String? = null
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading

        val clientId = serverClientIdOverride?.trim()?.ifEmpty { null }
            ?: getWebClientId().ifEmpty { null }

        if (clientId.isNullOrBlank()) {
            val msg = "Web Client ID Google belum dikonfigurasi. Anda dapat memasukkan Web Client ID dari Firebase Console atau menggunakan mode Masuk Cepat."
            _authState.value = AuthState.Error(msg)
            return@withContext Result.failure(IllegalStateException(msg))
        }

        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName
                    ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()

                // If Firebase is initialized, attempt to sign in with Firebase Auth
                var firebaseUid: String? = null
                try {
                    if (FirebaseApp.getApps(context).isNotEmpty()) {
                        val auth = FirebaseAuth.getInstance()
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth.signInWithCredential(firebaseCredential).await()
                        firebaseUid = authResult.user?.uid
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase Auth sign-in failed or not configured, using Google Account directly", e)
                }

                val user = UserAccount(
                    uid = firebaseUid ?: "google_${email.hashCode()}",
                    displayName = displayName,
                    email = email,
                    photoUrl = photoUrl,
                    idToken = idToken,
                    isGoogleUser = true,
                    lastLoginMillis = System.currentTimeMillis()
                )

                persistUser(user)
                return@withContext Result.success(user)
            } else {
                val msg = "Format kredensial Google tidak dikenali."
                _authState.value = AuthState.Error(msg)
                return@withContext Result.failure(IllegalStateException(msg))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Login Google dibatalkan oleh pengguna")
            loadPersistedUser()
            return@withContext Result.failure(e)
        } catch (e: NoCredentialException) {
            val msg = "Tidak ditemukan akun Google yang terhubung di perangkat ini."
            _authState.value = AuthState.Error(msg)
            return@withContext Result.failure(Exception(msg, e))
        } catch (e: GetCredentialException) {
            val msg = "Gagal memproses autentikasi Google: ${e.localizedMessage}"
            _authState.value = AuthState.Error(msg)
            return@withContext Result.failure(Exception(msg, e))
        } catch (e: Exception) {
            val msg = "Terjadi kesalahan saat login dengan Google: ${e.localizedMessage}"
            _authState.value = AuthState.Error(msg)
            return@withContext Result.failure(Exception(msg, e))
        }
    }

    /**
     * Masuk Cepat / Instant Sign-In dengan Akun Google pengguna.
     * Sangat berguna saat peluncuran awal atau pengujian jika Client ID belum diisi.
     */
    fun signInQuick(email: String, displayName: String, photoUrl: String? = null): UserAccount {
        val user = UserAccount(
            uid = "user_${UUID.randomUUID().toString().take(8)}",
            displayName = displayName.ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } },
            email = email.trim(),
            photoUrl = photoUrl,
            idToken = null,
            isGoogleUser = true,
            lastLoginMillis = System.currentTimeMillis()
        )
        persistUser(user)
        return user
    }

    suspend fun signOut(activityContext: Context? = null) {
        try {
            if (activityContext != null) {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal membersihkan credential state", e)
        }

        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance().signOut()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal logout dari Firebase Auth", e)
        }

        clearPersistedUser()
    }

    fun isFirebaseActive(): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "GoogleAuthManager"
        private const val PREFS_NAME = "uangku_auth_prefs"
        private const val KEY_USER_DATA = "persisted_user_account"
        private const val KEY_WEB_CLIENT_ID = "saved_google_web_client_id"
    }
}
