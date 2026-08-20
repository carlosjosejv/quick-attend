package com.greenshark.quickattend

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Created by Carlos Jiménez on 01-Aug-24.
 */

class AuthViewModel : ViewModel() {

    val firebaseAuth = FirebaseAuth.getInstance()
    private val _authState = MutableStateFlow(firebaseAuth.currentUser)
    val authState: StateFlow<FirebaseUser?> = _authState

    private val _googleSignInError = MutableLiveData<String?>()
    val googleSignInError: LiveData<String?> = _googleSignInError

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            val idToken = requestGoogleIdToken(activity) ?: return@launch
            authWithGoogle(idToken)
        }
    }

    /** Consumes the last error so it is only shown once. */
    fun googleSignInErrorShown() {
        _googleSignInError.value = null
    }

    private suspend fun requestGoogleIdToken(activity: Activity): String? {
        val signInWithGoogle = GetSignInWithGoogleOption
            .Builder(activity.getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogle)
            .build()

        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (e: GetCredentialCancellationException) {
            _googleSignInError.value = "Sign-in was canceled. Please try again."
            return null
        } catch (e: NoCredentialException) {
            _googleSignInError.value = "No Google account is available on this device."
            return null
        } catch (e: GetCredentialException) {
            _googleSignInError.value = "Sign-in failed. Please check your connection and try again."
            return null
        }

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            _googleSignInError.value = "An unknown error occurred. Please try again."
            return null
        }

        return try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            _googleSignInError.value = "An unknown error occurred. Please try again."
            null
        }
    }

    private suspend fun authWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        try {
            firebaseAuth.signInWithCredential(credential).await()
            _authState.value = firebaseAuth.currentUser
        } catch (e: FirebaseException) {
            _authState.value = null
            _googleSignInError.value = "Sign-in failed. Please check your connection and try again."
        }
    }

    fun createUser(email: String, password: String) {
        viewModelScope.launch {
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _authState.value = firebaseAuth.currentUser
                    } else {
                        _authState.value = null
                    }
                }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _authState.value = firebaseAuth.currentUser
                    } else {
                        _authState.value = null
                    }
                }
        }
    }

    fun signOut(context: Context) {
        firebaseAuth.signOut()
        _authState.value = null
        viewModelScope.launch {
            // Without this the next sign-in silently reuses the same account
            // instead of showing the account picker.
            try {
                CredentialManager.create(context).clearCredentialState(
                    ClearCredentialStateRequest(ClearCredentialStateRequest.TYPE_CLEAR_CREDENTIAL_STATE)
                )
            } catch (e: ClearCredentialException) {
                // Nothing the user can do about it; they are already signed out.
            }
        }
    }
}
