package com.example.service

import android.content.Context
import android.util.Log
import com.example.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class AuthService(private val context: Context) {

    private val tag = "AuthService"
    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    // Persistent storage for the local demo fallback mode
    private val prefs = context.getSharedPreferences("smart_hisab_prefs", Context.MODE_PRIVATE)

    var isFirebaseAvailable: Boolean = false
        private set

    init {
        try {
            // Check if Firebase is initialized, try to initialize if empty.
            if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (initEx: Exception) {
                    Log.e(tag, "Manual FirebaseApp.initializeApp failed: ${initEx.message}")
                }
            }
            // Check if Firebase is initialized.
            // If the google-services.json is missing or invalid, this will catch gracefully.
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firebaseAuth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()
                isFirebaseAvailable = true
                Log.d(tag, "Firebase is successfully initialized and available.")
            } else {
                Log.w(tag, "FirebaseApp list is empty. Operating in high-fidelity demo mode.")
                isFirebaseAvailable = false
            }
        } catch (e: Exception) {
            Log.e(tag, "Firebase initialization failed: ${e.message}. Operating in high-fidelity demo mode.")
            isFirebaseAvailable = false
        }
    }

    // Get currently logged-in user ID
    fun getCurrentUid(): String? {
        return if (isFirebaseAvailable) {
            firebaseAuth?.currentUser?.uid
        } else {
            prefs.getString("demo_current_uid", null)
        }
    }

    // Check if user is logged in
    fun isUserLoggedIn(): Boolean {
        return getCurrentUid() != null
    }

    // Cache profile locally in SharedPreferences for offline fallback
    private fun cacheUserProfileLocally(profile: UserProfile) {
        try {
            val json = JSONObject().apply {
                put("uid", profile.uid)
                put("shopName", profile.shopName)
                put("ownerName", profile.ownerName)
                put("email", profile.email)
                put("phone", profile.phone ?: "")
                put("createdAt", profile.createdAt)
                put("currency", profile.currency)
                put("language", profile.language)
            }.toString()
            prefs.edit().putString("user_${profile.uid}", json).apply()
        } catch (e: Exception) {
            Log.e(tag, "Failed to cache user profile locally", e)
        }
    }

    // Retrieve locally cached profile
    private fun getCachedUserProfile(uid: String): UserProfile? {
        val userJsonStr = prefs.getString("user_$uid", null) ?: return null
        return try {
            val json = JSONObject(userJsonStr)
            UserProfile(
                uid = json.getString("uid"),
                shopName = json.getString("shopName"),
                ownerName = json.getString("ownerName"),
                email = json.getString("email"),
                phone = json.getString("phone").takeIf { it.isNotEmpty() },
                createdAt = json.getLong("createdAt"),
                currency = json.optString("currency", "Rs"),
                language = json.optString("language", "en")
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse cached user profile", e)
            null
        }
    }

    // Sign up a new user with email, password and profile data
    suspend fun signUp(
        email: String,
        password: String,
        shopName: String,
        ownerName: String,
        phone: String?
    ): UserResult {
        return if (isFirebaseAvailable) {
            try {
                val auth = firebaseAuth ?: return UserResult.Error("Firebase Auth is not available.")
                val db = firestore ?: return UserResult.Error("Firestore is not available.")

                // 1. Create authentication user
                val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val uid = authResult.user?.uid ?: return UserResult.Error("Failed to retrieve user ID.")

                // 2. Create profile object
                val profile = UserProfile(
                    uid = uid,
                    shopName = shopName,
                    ownerName = ownerName,
                    email = email.trim(),
                    phone = phone?.takeIf { it.isNotBlank() },
                    createdAt = System.currentTimeMillis()
                )

                // 3. Save profile in Firestore collection: users/{uid}
                db.collection("users")
                    .document(uid)
                    .set(profile.toMap())
                    .await()

                cacheUserProfileLocally(profile)
                UserResult.Success(profile)
            } catch (e: Exception) {
                Log.e(tag, "Firebase Sign Up Error", e)
                UserResult.Error(e.localizedMessage ?: "An unexpected error occurred during cloud sign up.")
            }
        } else {
            // Demo Fallback Mode (saves to SharedPreferences so it persists after app restart!)
            try {
                val mockUid = "demo_uid_" + email.hashCode().toString()
                
                // Check if user already exists
                if (prefs.contains("user_$mockUid")) {
                    return UserResult.Error("An account with this email already exists in local demo database.")
                }

                val profile = UserProfile(
                    uid = mockUid,
                    shopName = shopName,
                    ownerName = ownerName,
                    email = email.trim(),
                    phone = phone?.takeIf { it.isNotBlank() },
                    createdAt = System.currentTimeMillis()
                )

                cacheUserProfileLocally(profile)
                prefs.edit().putString("demo_current_uid", mockUid).apply()

                UserResult.Success(profile)
            } catch (e: Exception) {
                UserResult.Error("Local simulation signup error: ${e.message}")
            }
        }
    }

    // Log in an existing user
    suspend fun logIn(email: String, password: String): UserResult {
        return if (isFirebaseAvailable) {
            try {
                val auth = firebaseAuth ?: return UserResult.Error("Firebase Auth is not available.")
                val db = firestore ?: return UserResult.Error("Firestore is not available.")

                // 1. Authenticate with Firebase Auth
                val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
                val uid = authResult.user?.uid ?: return UserResult.Error("Failed to retrieve user ID.")

                // 2. Fetch profile from Firestore users/{uid} with graceful fallback
                try {
                    val doc = db.collection("users").document(uid).get().await()
                    if (doc.exists()) {
                        val profile = UserProfile.fromMap(doc.data ?: emptyMap())
                        cacheUserProfileLocally(profile)
                        UserResult.Success(profile)
                    } else {
                        // Create minimal fallback profile if it doesn't exist in firestore yet
                        val profile = UserProfile(
                            uid = uid,
                            email = email,
                            shopName = "My Ledger Shop",
                            ownerName = "Shop Owner"
                        )
                        db.collection("users").document(uid).set(profile.toMap()).await()
                        cacheUserProfileLocally(profile)
                        UserResult.Success(profile)
                    }
                } catch (firestoreEx: Exception) {
                    Log.w(tag, "Firestore fetch failed during login, checking local cache", firestoreEx)
                    val cachedProfile = getCachedUserProfile(uid)
                    if (cachedProfile != null) {
                        UserResult.Success(cachedProfile)
                    } else {
                        // Create a fallback profile so the user can log in and use the app offline!
                        val fallbackProfile = UserProfile(
                            uid = uid,
                            email = email,
                            shopName = "My Hisab Shop (Offline)",
                            ownerName = "Shop Owner"
                        )
                        cacheUserProfileLocally(fallbackProfile)
                        UserResult.Success(fallbackProfile)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Firebase Log In Error", e)
                UserResult.Error(e.localizedMessage ?: "Invalid email or password. Please try again.")
            }
        } else {
            // Demo Fallback Mode
            val mockUid = "demo_uid_" + email.hashCode().toString()
            val cachedProfile = getCachedUserProfile(mockUid)
            if (cachedProfile != null) {
                prefs.edit().putString("demo_current_uid", mockUid).apply()
                UserResult.Success(cachedProfile)
            } else {
                // If demo user is logging in with any user, let's auto-create it for a smooth flow
                if (email.trim().lowercase() == "demo@hisab.com" && password == "password") {
                    val demoProfile = UserProfile(
                        uid = "demo_host_uid",
                        shopName = "Hisab General Store",
                        ownerName = "Ahmad Hisab",
                        email = "demo@hisab.com"
                    )
                    cacheUserProfileLocally(demoProfile)
                    prefs.edit().putString("demo_current_uid", "demo_host_uid").apply()
                    UserResult.Success(demoProfile)
                } else {
                    UserResult.Error("No account found for this email in local database. Please Sign Up or use 'demo@hisab.com' with password 'password'.")
                }
            }
        }
    }

    // Get user profile data
    suspend fun getUserProfile(uid: String): UserResult {
        return if (isFirebaseAvailable) {
            try {
                val db = firestore ?: return UserResult.Error("Firestore is not available.")
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val profile = UserProfile.fromMap(doc.data ?: emptyMap())
                    cacheUserProfileLocally(profile)
                    UserResult.Success(profile)
                } else {
                    val cachedProfile = getCachedUserProfile(uid)
                    if (cachedProfile != null) {
                        UserResult.Success(cachedProfile)
                    } else {
                        // Create on-the-fly fallback if document doesn't exist
                        val firebaseUser = firebaseAuth?.currentUser
                        val fallback = UserProfile(
                            uid = uid,
                            email = firebaseUser?.email ?: "",
                            shopName = "My Ledger Shop",
                            ownerName = "Shop Owner"
                        )
                        cacheUserProfileLocally(fallback)
                        UserResult.Success(fallback)
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Firestore getUserProfile failed, checking local cache", e)
                val cachedProfile = getCachedUserProfile(uid)
                if (cachedProfile != null) {
                    UserResult.Success(cachedProfile)
                } else {
                    // Try to get email from FirebaseAuth to construct a fallback
                    val firebaseUser = firebaseAuth?.currentUser
                    if (firebaseUser != null && firebaseUser.uid == uid) {
                        val fallback = UserProfile(
                            uid = uid,
                            email = firebaseUser.email ?: "",
                            shopName = "My Ledger Shop",
                            ownerName = "Shop Owner"
                        )
                        cacheUserProfileLocally(fallback)
                        UserResult.Success(fallback)
                    } else {
                        UserResult.Error(e.localizedMessage ?: "Failed to load cloud profile data.")
                    }
                }
            }
        } else {
            val cachedProfile = getCachedUserProfile(uid)
            if (cachedProfile != null) {
                UserResult.Success(cachedProfile)
            } else {
                UserResult.Error("Local profile not found.")
            }
        }
    }

    // Log out user
    fun logOut() {
        if (isFirebaseAvailable) {
            firebaseAuth?.signOut()
        } else {
            prefs.edit().remove("demo_current_uid").apply()
        }
    }

    // Update user profile language preference
    suspend fun updateLanguage(uid: String, lang: String): UserResult {
        // Save to global app preferences first so it's loaded instantly on next launch
        prefs.edit().putString("app_language", lang).apply()

        return if (isFirebaseAvailable) {
            try {
                val db = firestore ?: return UserResult.Error("Firestore is not available.")
                db.collection("users").document(uid).update("language", lang).await()
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val profile = UserProfile.fromMap(doc.data ?: emptyMap())
                    cacheUserProfileLocally(profile)
                    UserResult.Success(profile)
                } else {
                    val cachedProfile = getCachedUserProfile(uid)
                    if (cachedProfile != null) {
                        val updated = cachedProfile.copy(language = lang)
                        cacheUserProfileLocally(updated)
                        UserResult.Success(updated)
                    } else {
                        UserResult.Error("Profile not found.")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Firebase Update Language Error", e)
                val cachedProfile = getCachedUserProfile(uid)
                if (cachedProfile != null) {
                    val updated = cachedProfile.copy(language = lang)
                    cacheUserProfileLocally(updated)
                    UserResult.Success(updated)
                } else {
                    UserResult.Error(e.localizedMessage ?: "Failed to update language on cloud.")
                }
            }
        } else {
            val cachedProfile = getCachedUserProfile(uid)
            if (cachedProfile != null) {
                val updated = cachedProfile.copy(language = lang)
                cacheUserProfileLocally(updated)
                UserResult.Success(updated)
            } else {
                UserResult.Error("Failed to update local language.")
            }
        }
    }
}

// Result wrapper class for handling auth states cleanly
sealed class UserResult {
    data class Success(val profile: UserProfile) : UserResult()
    data class Error(val message: String) : UserResult()
}
