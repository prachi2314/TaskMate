package com.example.taskmate.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.example.taskmate.data.repository.AuthRepository
import com.example.taskmate.data.repository.StudyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AppModule.kt
 * Location: di/AppModule.kt
 *
 * Hilt dependency injection module.
 *
 * What is Hilt?
 * Hilt is a DI (Dependency Injection) framework.
 * Instead of manually creating and passing objects like:
 *   val auth = FirebaseAuth.getInstance()
 *   val repo = AuthRepository(auth, firestore)
 *   val vm   = AuthViewModel(repo)
 *
 * Hilt does this automatically by reading the @Inject
 * and @Provides annotations and wiring everything together.
 *
 * @InstallIn(SingletonComponent::class) means these
 * dependencies live as long as the app lives — they are
 * created once and reused everywhere (Singleton pattern).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the FirebaseAuth instance.
     *
     * @Singleton means only ONE instance is ever created.
     * Every ViewModel and Repository that needs FirebaseAuth
     * gets the same instance injected automatically.
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    /**
     * Provides the FirebaseFirestore instance.
     *
     * Configures Firestore with memory cache settings so the app
     * can read recently loaded data without a network call.
     *
     * firestoreSettings configures:
     *   - Memory cache: stores recent data in RAM
     *   - This replaces the deprecated isPersistenceEnabled
     */
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()

        val settings = firestoreSettings {
            // Memory cache — data stays in RAM while app is open
            // For offline persistence use: persistentCacheSettings { }
            setLocalCacheSettings(memoryCacheSettings { })
        }
        firestore.firestoreSettings = settings

        return firestore
    }

    /**
     * Provides AuthRepository.
     *
     * Hilt automatically calls provideFirebaseAuth() and
     * provideFirestore() to get the parameters needed here.
     * You never manually create AuthRepository anywhere else.
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository {
        return AuthRepository(firebaseAuth, firestore)
    }

    /**
     * Provides StudyRepository.
     *
     * StudyRepository only needs Firestore — not Auth —
     * because it always receives the userId as a parameter
     * from the ViewModel (which gets it from FirebaseAuth).
     */
    @Provides
    @Singleton
    fun provideStudyRepository(
        firestore: FirebaseFirestore
    ): StudyRepository {
        return StudyRepository(firestore)
    }
}