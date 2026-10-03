package com.aistudio.ecotobacco.kfzqw.data.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.aistudio.ecotobacco.kfzqw.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

class FirebaseAuthHelper(private val context: Context) {

    private val auth: FirebaseAuth? by lazy {
        try {
            ensureFirebaseInitialized(context)
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("FirebaseAuthHelper", "FirebaseAuth unavailable: ${e.message}")
            null
        }
    }
    private val credentialManager by lazy { CredentialManager.create(context) }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        try {
            val fbAuth = auth
            if (fbAuth != null) {
                _currentUser.value = fbAuth.currentUser
                fbAuth.addAuthStateListener { firebaseAuth ->
                    _currentUser.value = firebaseAuth.currentUser
                }
            }
        } catch (e: Exception) {
            Log.w("FirebaseAuthHelper", "Firebase Auth init notice: ${e.message}")
        }
    }

    private fun ensureFirebaseInitialized(ctx: Context) {
        try {
            if (FirebaseApp.getApps(ctx).isEmpty()) {
                val resId = ctx.resources.getIdentifier("google_app_id", "string", ctx.packageName)
                if (resId != 0) {
                    FirebaseApp.initializeApp(ctx.applicationContext)
                } else {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:680872469505:web:4f89cbd24222aaaf998c00")
                        .setApiKey("AIzaSyBWw2rT7zrVogQvFs_7ExqV-Y2mN4AjNto")
                        .setProjectId("project-0be2da66-9971-458e-bfe")
                        .setStorageBucket("project-0be2da66-9971-458e-bfe.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(ctx.applicationContext, options)
                }
            }
        } catch (e: Exception) {
            Log.w("FirebaseAuthHelper", "FirebaseApp init: ${e.message}")
        }
    }

    val isSignedIn: Boolean
        get() = _currentUser.value != null

    val userEmail: String?
        get() = _currentUser.value?.email

    val displayName: String?
        get() = _currentUser.value?.displayName ?: _currentUser.value?.email?.substringBefore("@") ?: "User"

    val photoUrl: String?
        get() = _currentUser.value?.photoUrl?.toString()

    val uid: String?
        get() = _currentUser.value?.uid

    /**
     * Signs in with Google using Credential Manager and Firebase Auth.
     */
    suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val fbAuth = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth belum tersedia"))
        _isLoading.value = true
        _authError.value = null
        try {
            val serverClientId = BuildConfig.GOOGLE_OAUTH_CLIENT_ID.ifBlank {
                val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                if (resId != 0) context.getString(resId) else ""
            }

            if (serverClientId.isBlank()) {
                val anonResult = fbAuth.signInAnonymously().await()
                val user = anonResult.user ?: throw IllegalStateException("Firebase Anonymous user is null")
                _currentUser.value = user
                _isLoading.value = false
                return@withContext Result.success(user)
            }

            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = fbAuth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw IllegalStateException("User is null after Google sign-in")
                _currentUser.value = user
                _isLoading.value = false
                Result.success(user)
            } else {
                _isLoading.value = false
                Result.failure(IllegalArgumentException("Unsupported credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            _isLoading.value = false
            Result.failure(Exception("Login dibatalkan"))
        } catch (e: Exception) {
            Log.w("FirebaseAuthHelper", "Sign in with Google notice: ${e.message}")
            _authError.value = e.localizedMessage
            _isLoading.value = false
            Result.failure(e)
        }
    }

    /**
     * Guest/Anonymous sign in for immediate Firebase persistence testing
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val fbAuth = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth belum tersedia"))
        _isLoading.value = true
        _authError.value = null
        try {
            val result = fbAuth.signInAnonymously().await()
            val user = result.user ?: throw IllegalStateException("Firebase Anonymous user is null")
            _currentUser.value = user
            _isLoading.value = false
            Result.success(user)
        } catch (e: Exception) {
            _authError.value = e.localizedMessage
            _isLoading.value = false
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val fbAuth = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth belum tersedia"))
        _isLoading.value = true
        _authError.value = null
        try {
            val result = fbAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: throw IllegalStateException("User is null after sign in")
            _currentUser.value = user
            _isLoading.value = false
            Result.success(user)
        } catch (e: Exception) {
            _authError.value = e.localizedMessage
            _isLoading.value = false
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val fbAuth = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth belum tersedia"))
        _isLoading.value = true
        _authError.value = null
        try {
            val result = fbAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: throw IllegalStateException("User is null after sign up")
            _currentUser.value = user
            _isLoading.value = false
            Result.success(user)
        } catch (e: Exception) {
            _authError.value = e.localizedMessage
            _isLoading.value = false
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
            _currentUser.value = null
            _authError.value = null
        } catch (e: Exception) {
            Log.w("FirebaseAuthHelper", "Error signing out: ${e.message}")
        }
    }
}
