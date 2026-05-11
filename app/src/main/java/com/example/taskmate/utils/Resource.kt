package com.example.taskmate.utils

/**
 * Resource.kt
 * Location: utils/Resource.kt
 *
 * A generic sealed class that wraps every network or database
 * result so the UI always knows what state to show.
 *
 * Usage in ViewModel:
 *   _tasks.value = Resource.Loading
 *   _tasks.value = Resource.Success(taskList)
 *   _tasks.value = Resource.Error("Failed to load tasks")
 *
 * Usage in Fragment:
 *   when (resource) {
 *       is Resource.Loading -> showSpinner()
 *       is Resource.Success -> showData(resource.data)
 *       is Resource.Error   -> showError(resource.message)
 *   }
 *
 * The 'out T' variance annotation means a Resource<Dog>
 * can be used where a Resource<Animal> is expected.
 */
sealed class Resource<out T> {

    /**
     * Loading state — show a progress indicator.
     * No data yet.
     */
    object Loading : Resource<Nothing>()

    /**
     * Success state — the operation completed.
     * data holds the result.
     */
    data class Success<T>(val data: T) : Resource<T>()

    /**
     * Error state — something went wrong.
     * message describes what happened.
     */
    data class Error(val message: String) : Resource<Nothing>()

    // ── Helper properties ──────────────────────────────────────────

    /** True only when this is a Success */
    val isSuccess get() = this is Success

    /** True only when this is a Loading */
    val isLoading get() = this is Loading

    /** True only when this is an Error */
    val isError get() = this is Error

    /** Returns data if Success, null otherwise */
    fun getDataOrNull(): T? = if (this is Success) data else null

    /** Returns message if Error, null otherwise */
    fun getErrorOrNull(): String? = if (this is Error) message else null
}