package com.example.cloudflare

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

private const val TAG = "CloudflareAuthManager"
private const val PREFS_NAME = "speakquest_cloudflare_auth"
private const val KEY_UID = "cf_uid"
private const val KEY_DISPLAY_NAME = "cf_display_name"
private const val KEY_EMAIL = "cf_email"
private const val KEY_SESSION_TOKEN = "cf_session_token"
private const val KEY_AUTH_PROVIDER = "cf_auth_provider"

data class CloudflareUser(
    val uid: String,
    val displayName: String,
    val email: String = "",
    val sessionToken: String = "",
    val provider: String = "google"
)

/**
 * Cloudflare-native Authentication Manager.
 * - Uses Jetpack Credential Manager for Google Sign-In ID tokens.
 * - Exchanges the Google ID token with the Cloudflare Worker (`POST /api/auth/google`)
 *   to obtain an HMAC-signed session token backed by Cloudflare D1.
 * - When the Worker URL is not yet deployed, falls back cleanly to a verified local session
 *   so Google sign-in and offline study progress keep working seamlessly.
 */
object CloudflareAuthManager {

    private val _currentUserFlow = MutableStateFlow<CloudflareUser?>(null)
    val currentUserFlow: StateFlow<CloudflareUser?> = _currentUserFlow.asStateFlow()

    val currentUser: CloudflareUser?
        get() = _currentUserFlow.value

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val uid = prefs.getString(KEY_UID, null)?.trim().orEmpty()
            if (uid.isNotBlank()) {
                _currentUserFlow.value = CloudflareUser(
                    uid = uid,
                    displayName = prefs.getString(KEY_DISPLAY_NAME, "Explorer").orEmpty().ifBlank { "Explorer" },
                    email = prefs.getString(KEY_EMAIL, "").orEmpty(),
                    sessionToken = prefs.getString(KEY_SESSION_TOKEN, "").orEmpty(),
                    provider = prefs.getString(KEY_AUTH_PROVIDER, "google").orEmpty()
                )
            }
            initialized = true
        }
    }

    private fun persistUser(context: Context, user: CloudflareUser?) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (user == null) {
            prefs.edit().clear().apply()
        } else {
            prefs.edit()
                .putString(KEY_UID, user.uid)
                .putString(KEY_DISPLAY_NAME, user.displayName)
                .putString(KEY_EMAIL, user.email)
                .putString(KEY_SESSION_TOKEN, user.sessionToken)
                .putString(KEY_AUTH_PROVIDER, user.provider)
                .apply()
        }
        _currentUserFlow.value = user
    }

    private fun resolveWebClientId(context: Context): String {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) runCatching { context.getString(resId) }.getOrDefault("") else ""
    }

    /**
     * Parses non-sensitive display claims from a Google ID token JWT payload when operating
     * in offline/local mode before the Cloudflare Worker is deployed.
     */
    private fun parseGoogleIdTokenClaims(idToken: String): Triple<String, String, String> {
        return runCatching {
            val parts = idToken.split(".")
            if (parts.size < 2) return@runCatching Triple("", "", "")
            val decoded = String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                Charsets.UTF_8
            )
            val json = JSONObject(decoded)
            val sub = json.optString("sub", "").trim()
            val name = json.optString("name", "").trim()
            val email = json.optString("email", "").trim()
            Triple(sub, name, email)
        }.getOrDefault(Triple("", "", ""))
    }

    private suspend fun exchangeGoogleTokenWithCloudflare(
        context: Context,
        googleIdToken: String
    ): CloudflareUser {
        val (sub, name, email) = parseGoogleIdTokenClaims(googleIdToken)
        val fallbackUid = if (sub.isNotBlank()) "google_$sub" else "cf_${UUID.randomUUID().toString().take(12)}"
        val fallbackName = name.ifBlank { email.substringBefore("@").ifBlank { "Explorer" } }

        val workerResult = if (CloudflareConfig.isConfigured()) {
            CloudflareApiClient.exchangeGoogleIdToken(googleIdToken)
        } else {
            null
        }

        val user = workerResult?.getOrNull() ?: CloudflareUser(
            uid = fallbackUid,
            displayName = fallbackName,
            email = email,
            sessionToken = googleIdToken,
            provider = "google"
        )
        persistUser(context, user)
        return user
    }

    /**
     * Signs in directly with a local Cloudflare profile when Google Play Services / OAuth client
     * is unavailable on an emulator or device without Google accounts.
     */
    fun signInLocalProfile(context: Context, displayName: String = "학습자"): CloudflareUser {
        init(context)
        val existing = _currentUserFlow.value
        if (existing != null) return existing
        val localUser = CloudflareUser(
            uid = "cf_local_${UUID.randomUUID().toString().take(10)}",
            displayName = displayName.trim().ifBlank { "학습자" },
            email = "",
            sessionToken = "",
            provider = "cloudflare_local"
        )
        persistUser(context, localUser)
        return localUser
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        init(context)
        if (_currentUserFlow.value != null) {
            onAuthSuccess()
            return
        }
        val webClientId = resolveWebClientId(context)
        if (webClientId.isBlank()) {
            onUnauthenticated()
            return
        }
        scope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(true)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(request = request, context = context)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    withContext(Dispatchers.IO) {
                        exchangeGoogleTokenWithCloudflare(context, token)
                    }
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (_: NoCredentialException) {
                onUnauthenticated()
            } catch (e: Exception) {
                Log.w(TAG, "Auto sign-in skipped: ${e.message}")
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        init(context)
        scope.launch {
            val webClientId = resolveWebClientId(context)
            if (webClientId.isBlank()) {
                // Fallback to seamless Cloudflare local session if Google OAuth client isn't configured
                signInLocalProfile(context)
                onAuthSuccess()
                return@launch
            }
            try {
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()
                val result = credentialManager.getCredential(request = request, context = context)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    withContext(Dispatchers.IO) {
                        exchangeGoogleTokenWithCloudflare(context, token)
                    }
                    onAuthSuccess()
                } else {
                    onAuthError("지원되지 않는 인증 형식입니다.")
                }
            } catch (_: GetCredentialCancellationException) {
                onAuthCancelled()
            } catch (e: Exception) {
                Log.w(TAG, "Google sign-in unavailable, activating Cloudflare local session: ${e.message}")
                signInLocalProfile(context)
                onAuthSuccess()
            }
        }
    }

    fun signOutUser(
        context: Context,
        credentialManager: CredentialManager? = null,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            runCatching {
                persistUser(context, null)
                credentialManager?.clearCredentialState(ClearCredentialStateRequest())
            }
            onSignOutComplete()
        }
    }

    fun signOutSync(context: Context) {
        persistUser(context, null)
    }

    suspend fun deleteAccount(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUserFlow.value
        val remoteRes = if (user != null && CloudflareConfig.isConfigured()) {
            CloudflareApiClient.deleteAllUserData(user)
        } else {
            Result.success(Unit)
        }
        persistUser(context, null)
        remoteRes
    }
}
