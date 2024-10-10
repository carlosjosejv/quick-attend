package com.greenshark.quickattend

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Created by Carlos Jiménez on 01-Aug-24.
 */

class AuthViewModel : ViewModel() {

    val firebaseAuth = FirebaseAuth.getInstance()
    private val _authState = MutableStateFlow(firebaseAuth.currentUser)
    val authState: StateFlow<FirebaseUser?> = _authState

    private val _googleSignInResult = MutableLiveData<Result<GoogleSignInAccount>>()
    val googleSignInResult: LiveData<Result<GoogleSignInAccount>> = _googleSignInResult

    private lateinit var googleSignInClient: GoogleSignInClient

    fun initGoogleSignInClient(client: GoogleSignInClient) {
        googleSignInClient = client
    }

    fun signInWithGoogle(activity: Activity, requestCode: Int) {
        val signInIntent = googleSignInClient.signInIntent
        activity.startActivityForResult(signInIntent, requestCode)
    }

    fun handleGoogleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            _googleSignInResult.value = Result.success(account)
        } catch (e: ApiException) {
            val errorMessage = when (e.statusCode) {
                CommonStatusCodes.CANCELED -> "Sign-in was canceled. Please try again."
                CommonStatusCodes.ERROR -> "Sign-in failed. Please check your connection and try again."
                CommonStatusCodes.NETWORK_ERROR -> "Network error occurred. Please check your internet connection."
                CommonStatusCodes.DEVELOPER_ERROR -> "Developer error. Please check your app's configuration."
                else -> "An unknown error occurred. Please try again."
            }
            _googleSignInResult.value = Result.failure(Exception(errorMessage))
        }
    }

    fun authWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = firebaseAuth.currentUser
                } else {
                    _authState.value = null
                }
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

    fun signOut() {
        firebaseAuth.signOut()
        _authState.value = null
    }
}