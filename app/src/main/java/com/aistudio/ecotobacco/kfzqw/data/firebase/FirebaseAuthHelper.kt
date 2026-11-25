package com.aistudio.ecotobacco.kfzqw.data.firebase

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthHelper(private val context: Context) {
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    val uid: String? get() = try { auth.currentUser?.uid } catch (e: Exception) { null }

    init {
        try {
            _currentUser.value = auth.currentUser
            auth.addAuthStateListener { firebaseAuth ->
                _currentUser.value = firebaseAuth.currentUser
            }
        } catch (e: Exception) { }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<Unit> {
        _isLoading.value = true
        _authError.value = null
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            _isLoading.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            _isLoading.value = false
            _authError.value = e.localizedMessage ?: "Gagal masuk"
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Result<Unit> {
        _isLoading.value = true
        _authError.value = null
        return try {
            auth.createUserWithEmailAndPassword(email, pass).await()
            _isLoading.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            _isLoading.value = false
            _authError.value = e.localizedMessage ?: "Gagal pendaftaran"
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<Unit> {
        _isLoading.value = true
        _authError.value = null
        return try {
            auth.signInAnonymously().await()
            _isLoading.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            _isLoading.value = false
            _authError.value = e.localizedMessage ?: "Gagal masuk anonim"
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) { }
    }
}
