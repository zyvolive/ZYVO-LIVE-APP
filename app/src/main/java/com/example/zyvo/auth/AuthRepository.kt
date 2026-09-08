package com.example.zyvo.auth

import android.content.Context
import android.util.Log
import com.example.zyvo.data.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Authentication and User Profile repository backed by Firebase Authentication and Cloud Firestore.
 * Permanent user identity is bound to Firebase UID at `users/{uid}`.
 */
class AuthRepository(private val context: Context? = null) {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FirebaseAuth instance: ${e.message}", e)
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FirebaseFirestore instance: ${e.message}", e)
            null
        }
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        setupAuthStateListener()
    }

    private fun setupAuthStateListener() {
        val auth = firebaseAuth
        if (auth == null) {
            _authState.value = AuthState.Unauthenticated
            return
        }

        auth.addAuthStateListener { firebaseAuthInstance ->
            val currentFirebaseUser = firebaseAuthInstance.currentUser
            if (currentFirebaseUser != null) {
                repositoryScope.launch {
                    loadOrCreateFirestoreUserProfile(currentFirebaseUser)
                }
            } else {
                _currentUser.value = null
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    /**
     * Checks if a user session is active and loads/restores their persistent Firestore profile.
     */
    suspend fun checkSession(): AuthState {
        val auth = firebaseAuth ?: return AuthState.Unauthenticated
        val currentFirebaseUser = auth.currentUser ?: run {
            _authState.value = AuthState.Unauthenticated
            return AuthState.Unauthenticated
        }

        _authState.value = AuthState.Loading
        return loadOrCreateFirestoreUserProfile(currentFirebaseUser)
    }

    /**
     * Loads the persistent user profile at `users/{uid}`.
     * If the document does not exist yet, creates it using the Firebase UID.
     */
    private suspend fun loadOrCreateFirestoreUserProfile(
        firebaseUser: FirebaseUser,
        initialDisplayName: String? = null,
        initialUsername: String? = null,
        initialAvatar: String? = null
    ): AuthState {
        val uid = firebaseUser.uid
        val db = firestore

        if (db == null) {
            // Offline or unconfigured Firestore fallback using FirebaseUser attributes
            val fallbackUser = User(
                uid = uid,
                username = initialUsername ?: firebaseUser.email?.substringBefore("@") ?: "user_${uid.take(6)}",
                displayName = initialDisplayName ?: firebaseUser.displayName ?: "ZYVO Broadcaster",
                avatar = initialAvatar ?: "👑",
                createdAt = System.currentTimeMillis(),
                isOnline = true
            )
            _currentUser.value = fallbackUser
            val state = AuthState.Authenticated(fallbackUser)
            _authState.value = state
            return state
        }

        return try {
            val userDocRef = db.collection(USERS_COLLECTION).document(uid)
            val snapshot = userDocRef.get().awaitResult()

            val user: User = if (snapshot.exists()) {
                val data = snapshot.data ?: emptyMap()
                User(
                    uid = uid,
                    username = (data["username"] as? String)?.ifBlank { null }
                        ?: initialUsername
                        ?: firebaseUser.email?.substringBefore("@")
                        ?: "user_${uid.take(6)}",
                    displayName = (data["displayName"] as? String)?.ifBlank { null }
                        ?: initialDisplayName
                        ?: firebaseUser.displayName
                        ?: "ZYVO Broadcaster",
                    avatar = (data["avatar"] as? String)?.ifBlank { null }
                        ?: initialAvatar
                        ?: "👑",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    isOnline = true
                )
            } else {
                // First time sign-in: create user profile document in Firestore at users/{uid}
                val newUser = User(
                    uid = uid,
                    username = initialUsername
                        ?: firebaseUser.email?.substringBefore("@")
                        ?: "user_${uid.take(6)}",
                    displayName = initialDisplayName
                        ?: firebaseUser.displayName
                        ?: "ZYVO Broadcaster",
                    avatar = initialAvatar ?: "👑",
                    createdAt = System.currentTimeMillis(),
                    isOnline = true
                )
                userDocRef.set(newUser.toMap(), SetOptions.merge()).awaitResult()
                newUser
            }

            _currentUser.value = user
            val state = AuthState.Authenticated(user)
            _authState.value = state
            state
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching/creating Firestore user document for uid: $uid", e)
            // Even if Firestore read fails due to offline/rules, keep user logged in with local model
            val resilientUser = User(
                uid = uid,
                username = initialUsername ?: firebaseUser.email?.substringBefore("@") ?: "user_${uid.take(6)}",
                displayName = initialDisplayName ?: firebaseUser.displayName ?: "ZYVO Broadcaster",
                avatar = initialAvatar ?: "👑",
                createdAt = System.currentTimeMillis(),
                isOnline = true
            )
            _currentUser.value = resilientUser
            val state = AuthState.Authenticated(resilientUser)
            _authState.value = state
            state
        }
    }

    /**
     * Sign In with Email and Password using Firebase Authentication.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Firebase Authentication is not available"))
        _authState.value = AuthState.Loading

        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).awaitResult()
            val firebaseUser = authResult.user ?: throw Exception("Firebase user is null after sign in")
            when (val state = loadOrCreateFirestoreUserProfile(firebaseUser)) {
                is AuthState.Authenticated -> Result.success(state.user)
                is AuthState.Error -> Result.failure(Exception(state.message))
                else -> Result.failure(Exception("Failed to load user profile"))
            }
        } catch (e: Exception) {
            val friendlyError = mapAuthException(e)
            _authState.value = AuthState.Error(friendlyError)
            Result.failure(Exception(friendlyError, e))
        }
    }

    /**
     * Sign Up with Email and Password using Firebase Authentication and create Firestore profile.
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String,
        username: String,
        avatar: String = "👑"
    ): Result<User> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Firebase Authentication is not available"))
        _authState.value = AuthState.Loading

        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).awaitResult()
            val firebaseUser = authResult.user ?: throw Exception("Firebase user is null after signup")

            when (val state = loadOrCreateFirestoreUserProfile(
                firebaseUser = firebaseUser,
                initialDisplayName = displayName.trim(),
                initialUsername = username.trim(),
                initialAvatar = avatar
            )) {
                is AuthState.Authenticated -> Result.success(state.user)
                is AuthState.Error -> Result.failure(Exception(state.message))
                else -> Result.failure(Exception("Failed to create user profile"))
            }
        } catch (e: Exception) {
            val friendlyError = mapAuthException(e)
            _authState.value = AuthState.Error(friendlyError)
            Result.failure(Exception(friendlyError, e))
        }
    }

    /**
     * Anonymous or Quick Sign In using Firebase Authentication.
     */
    suspend fun signInAnonymously(
        displayName: String = "ZYVO Broadcaster",
        username: String = "broadcaster",
        avatar: String = "🚀"
    ): Result<User> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Firebase Authentication is not available"))
        _authState.value = AuthState.Loading

        return try {
            val authResult = auth.signInAnonymously().awaitResult()
            val firebaseUser = authResult.user ?: throw Exception("Firebase anonymous user is null")

            when (val state = loadOrCreateFirestoreUserProfile(
                firebaseUser = firebaseUser,
                initialDisplayName = displayName.trim(),
                initialUsername = username.trim(),
                initialAvatar = avatar
            )) {
                is AuthState.Authenticated -> Result.success(state.user)
                is AuthState.Error -> Result.failure(Exception(state.message))
                else -> Result.failure(Exception("Failed to initialize guest profile"))
            }
        } catch (e: Exception) {
            val friendlyError = mapAuthException(e)
            _authState.value = AuthState.Error(friendlyError)
            Result.failure(Exception(friendlyError, e))
        }
    }

    /**
     * Sign In with Google ID Token using Firebase Authentication GoogleAuthProvider.
     */
    suspend fun signInWithGoogle(
        idToken: String,
        displayName: String? = null,
        username: String? = null,
        avatar: String = "👑"
    ): Result<User> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Firebase Authentication is not available"))
        _authState.value = AuthState.Loading

        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).awaitResult()
            val firebaseUser = authResult.user ?: throw Exception("Firebase user is null after Google sign-in")

            when (val state = loadOrCreateFirestoreUserProfile(
                firebaseUser = firebaseUser,
                initialDisplayName = displayName ?: firebaseUser.displayName,
                initialUsername = username ?: firebaseUser.email?.substringBefore("@"),
                initialAvatar = avatar
            )) {
                is AuthState.Authenticated -> Result.success(state.user)
                is AuthState.Error -> Result.failure(Exception(state.message))
                else -> Result.failure(Exception("Failed to initialize Google user profile"))
            }
        } catch (e: Exception) {
            val friendlyError = mapAuthException(e)
            _authState.value = AuthState.Error(friendlyError)
            Result.failure(Exception(friendlyError, e))
        }
    }

    /**
     * Updates the current user's profile document at `users/{uid}`.
     * Enforces that the update only modifies the document belonging to the authenticated UID.
     */
    suspend fun updateUserProfile(updatedUser: User): Result<Unit> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Firebase Authentication not initialized"))
        val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("No authenticated user"))

        if (updatedUser.uid != currentUid) {
            return Result.failure(SecurityException("Cannot modify profile of another user"))
        }

        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))

        return try {
            db.collection(USERS_COLLECTION).document(currentUid)
                .set(updatedUser.toMap(), SetOptions.merge())
                .awaitResult()
            _currentUser.value = updatedUser
            _authState.value = AuthState.Authenticated(updatedUser)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile for $currentUid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches a public user profile from Firestore by UID.
     */
    suspend fun getUserProfileByUid(uid: String): Result<User?> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val snapshot = db.collection(USERS_COLLECTION).document(uid).get().awaitResult()
            if (snapshot.exists()) {
                val data = snapshot.data ?: emptyMap()
                Result.success(User.fromMap(data))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile $uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Signs out the user from Firebase Authentication and updates the local state.
     */
    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error during signOut: ${e.message}", e)
        } finally {
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
        }
    }

    /**
     * Maps Firebase Authentication and Firestore exceptions to user-friendly messages.
     */
    private fun mapAuthException(e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password. Please check your credentials."
            is FirebaseAuthInvalidUserException -> "Account not found. Please sign up first."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists. Please sign in."
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 6 characters."
            is FirebaseNetworkException -> "Network error. Please check your internet connection and try again."
            else -> e.localizedMessage ?: "Authentication failed. Please try again."
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
        const val USERS_COLLECTION = "users"
    }
}

/**
 * Awaits completion of a Google Play Services / Firebase Task without blocking.
 */
internal suspend fun <T> Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) {
                cont.resumeWith(Result.success(result))
            }
        }
        addOnFailureListener { exception ->
            if (cont.isActive) {
                cont.resumeWith(Result.failure(exception))
            }
        }
        addOnCanceledListener {
            if (cont.isActive) {
                cont.cancel()
            }
        }
    }
