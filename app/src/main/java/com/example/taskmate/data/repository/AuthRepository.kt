package com.example.taskmate.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.taskmate.data.model.User
import com.example.taskmate.utils.Constants
import com.example.taskmate.utils.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthRepository.kt
 * Location: data/repository/AuthRepository.kt
 *
 * The ONLY class in the app that calls Firebase Auth.
 * ViewModels call this — they never import FirebaseAuth themselves.
 *
 * All suspend functions must be called from a coroutine
 * (viewModelScope inside ViewModel handles this automatically).
 *
 * .await() converts Firebase's callback-based Task into a
 * value we can return directly — no nested callbacks needed.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val TAG = "AuthRepository"
    }

    // ── Current user state ─────────────────────────────────────────

    /**
     * Returns the currently signed-in Firebase user.
     * Returns null if no user is signed in.
     * Called as a property — refreshes every time it is accessed.
     */
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    /**
     * Returns the UID of the currently signed-in user.
     * Returns empty string if no user is signed in.
     */
    val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: ""

    /**
     * Returns true if a user is currently signed in.
     * Used in LoginActivity to skip the login screen on app launch.
     */
    val isLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null

    // ══════════════════════════════════════════════════════════════
    //  REGISTER WITH EMAIL + PASSWORD
    // ══════════════════════════════════════════════════════════════

    /**
     * Creates a new Firebase Auth account with email and password.
     *
     * Step 1: Creates the Firebase Auth account (email + password).
     * Step 2: Saves the user profile (name, streak, etc.) to Firestore.
     *
     * The Firestore save is wrapped in a separate try-catch so that
     * even if saving the profile fails, the Auth account is still
     * created and the user can log in successfully.
     *
     * Firebase Auth only stores: UID, email, displayName, photoUrl.
     * Everything else (name, streak, study minutes) goes to Firestore.
     */
    suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String
    ): Resource<FirebaseUser> {
        return try {
            // Step 1 — Create Firebase Auth account
            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()

            val firebaseUser = authResult.user
                ?: return Resource.Error("Registration failed. Please try again.")

            // Step 2 — Save user profile to Firestore
            // Wrapped in separate try-catch so Auth success is not
            // affected by a Firestore write failure
            try {
                saveUserToFirestore(
                    User(
                        userId    = firebaseUser.uid,
                        name      = name,
                        email     = email,
                        createdAt = System.currentTimeMillis()
                    )
                )
                Log.d(TAG, "User profile saved to Firestore: ${firebaseUser.uid}")
            } catch (firestoreError: Exception) {
                // Auth account was created successfully.
                // Firestore profile will be created on next login.
                Log.e(TAG, "Firestore save failed: ${firestoreError.message}")
            }

            Log.d(TAG, "Registration successful: ${firebaseUser.email}")
            Resource.Success(firebaseUser)

        } catch (e: com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
            Log.e(TAG, "Weak password: ${e.message}")
            Resource.Error("Password is too weak. Use at least 6 characters.")

        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Log.e(TAG, "Invalid credentials: ${e.message}")
            Resource.Error("The email address format is not valid.")

        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            Log.e(TAG, "Email already in use: ${e.message}")
            Resource.Error("An account with this email already exists.")

        } catch (e: Exception) {
            Log.e(TAG, "Registration error: ${e.message}")
            Resource.Error(e.message ?: "Registration failed. Please try again.")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  LOGIN WITH EMAIL + PASSWORD
    // ══════════════════════════════════════════════════════════════

    /**
     * Signs in an existing user with email and password.
     *
     * Catches specific Firebase exceptions to provide user-friendly
     * error messages instead of raw Firebase error strings.
     */
    suspend fun loginWithEmail(
        email: String,
        password: String
    ): Resource<FirebaseUser> {
        return try {
            val authResult = firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .await()

            val firebaseUser = authResult.user
                ?: return Resource.Error("Login failed. Please try again.")

            // Create Firestore profile if it does not exist yet
            // (handles edge case where profile save failed during registration)
            try {
                ensureUserProfileExists(firebaseUser)
            } catch (e: Exception) {
                Log.e(TAG, "Profile check failed: ${e.message}")
            }

            Log.d(TAG, "Login successful: ${firebaseUser.email}")
            Resource.Success(firebaseUser)

        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            Log.e(TAG, "User not found: ${e.message}")
            Resource.Error("No account found with this email address.")

        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Log.e(TAG, "Wrong password: ${e.message}")
            Resource.Error("Incorrect password. Please try again.")

        } catch (e: com.google.firebase.FirebaseTooManyRequestsException) {
            Log.e(TAG, "Too many requests: ${e.message}")
            Resource.Error("Too many failed attempts. Please try again later.")

        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}")
            Resource.Error(e.message ?: "Login failed. Please try again.")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  GOOGLE SIGN-IN
    // ══════════════════════════════════════════════════════════════

    /**
     * Signs in a user with a Google ID token.
     *
     * Flow:
     * 1. LoginActivity launches Google Sign-In intent
     * 2. User picks their Google account
     * 3. Google returns an idToken
     * 4. LoginActivity passes the idToken to AuthViewModel
     * 5. AuthViewModel calls this function
     * 6. Firebase verifies the token and signs the user in
     * 7. If first login, creates a Firestore user profile
     */
    suspend fun loginWithGoogle(idToken: String): Resource<FirebaseUser> {
        return try {
            // Convert Google token to Firebase credential
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            val authResult = firebaseAuth
                .signInWithCredential(credential)
                .await()

            val firebaseUser = authResult.user
                ?: return Resource.Error("Google sign-in failed. Please try again.")

            // isNewUser is true only on the very first login
            // with this Google account on this Firebase project
            if (authResult.additionalUserInfo?.isNewUser == true) {
                Log.d(TAG, "New Google user — creating Firestore profile")
                try {
                    saveUserToFirestore(
                        User(
                            userId    = firebaseUser.uid,
                            name      = firebaseUser.displayName ?: "Student",
                            email     = firebaseUser.email ?: "",
                            photoUrl  = firebaseUser.photoUrl?.toString() ?: "",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                } catch (firestoreError: Exception) {
                    Log.e(TAG, "Firestore save failed for Google user: ${firestoreError.message}")
                }
            } else {
                Log.d(TAG, "Existing Google user signed in: ${firebaseUser.email}")
            }

            Resource.Success(firebaseUser)

        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Log.e(TAG, "Invalid Google credential: ${e.message}")
            Resource.Error("Invalid Google credential. Please try again.")

        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            Log.e(TAG, "Account collision: ${e.message}")
            Resource.Error("This email is already registered with a different sign-in method.")

        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in error: ${e.message}")
            Resource.Error(e.message ?: "Google sign-in failed. Please try again.")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PASSWORD RESET
    // ══════════════════════════════════════════════════════════════

    /**
     * Sends a password reset email to the given address.
     * Firebase sends the email — we just trigger the request.
     * Returns Unit on success (no data returned, just confirmation).
     */
    suspend fun sendPasswordResetEmail(email: String): Resource<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "Password reset email sent to: $email")
            Resource.Success(Unit)

        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            Log.e(TAG, "No account for reset email: ${e.message}")
            Resource.Error("No account found with this email address.")

        } catch (e: Exception) {
            Log.e(TAG, "Reset email error: ${e.message}")
            Resource.Error(e.message ?: "Failed to send reset email. Please try again.")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  GET USER PROFILE
    // ══════════════════════════════════════════════════════════════

    /**
     * Fetches the user's profile document from Firestore.
     * Called from DashboardViewModel to load the user's name,
     * streak, and study stats for the Dashboard header.
     */
    suspend fun getUserProfile(userId: String): Resource<User> {
        return try {
            val document = firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            val user = document.toObject(User::class.java)
                ?: return Resource.Error("User profile not found.")

            Log.d(TAG, "User profile loaded: ${user.name}")
            Resource.Success(user)

        } catch (e: Exception) {
            Log.e(TAG, "Get profile error: ${e.message}")
            Resource.Error(e.message ?: "Failed to load profile.")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  UPDATE USER PROFILE
    // ══════════════════════════════════════════════════════════════

    /**
     * Updates specific fields in the user's Firestore document.
     * Uses a Map so only the provided fields are updated —
     * other fields are left unchanged.
     *
     * Example usage:
     *   updateUserProfile(userId, mapOf("currentStreak" to 7))
     */
    suspend fun updateUserProfile(
        userId: String,
        updates: Map<String, Any>
    ): Resource<Unit> {
        return try {
            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .await()
            Log.d(TAG, "User profile updated: $updates")
            Resource.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Update profile error: ${e.message}")
            Resource.Error(e.message ?: "Failed to update profile.")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SIGN OUT
    // ══════════════════════════════════════════════════════════════

    /**
     * Signs the current user out of Firebase Auth.
     * Not a suspend function — synchronous, no network call needed.
     * Just clears the local auth token.
     */
    fun signOut() {
        Log.d(TAG, "User signed out: ${firebaseAuth.currentUser?.email}")
        firebaseAuth.signOut()
    }

    // ══════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Writes a User object to Firestore.
     * Document ID = Firebase Auth UID — makes lookups trivial.
     *
     * SetOptions.merge() means:
     *   - If the document already exists → only update provided fields
     *   - If the document does not exist → create it
     *   This is safe to call multiple times without overwriting data.
     */
    private suspend fun saveUserToFirestore(user: User) {
        firestore
            .collection(Constants.COLLECTION_USERS)
            .document(user.userId)
            .set(user, SetOptions.merge())
            .await()
        Log.d(TAG, "Firestore user document saved: ${user.userId}")
    }

    /**
     * Checks if a Firestore profile exists for this user.
     * If not, creates one. Called after email login to handle
     * edge cases where the profile save failed during registration.
     */
    private suspend fun ensureUserProfileExists(firebaseUser: FirebaseUser) {
        val document = firestore
            .collection(Constants.COLLECTION_USERS)
            .document(firebaseUser.uid)
            .get()
            .await()

        if (!document.exists()) {
            Log.d(TAG, "Profile missing — creating for existing user")
            saveUserToFirestore(
                User(
                    userId    = firebaseUser.uid,
                    name      = firebaseUser.displayName ?: "Student",
                    email     = firebaseUser.email ?: "",
                    photoUrl  = firebaseUser.photoUrl?.toString() ?: "",
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
}