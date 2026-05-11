package com.example.taskmate.ui.focus

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.taskmate.data.model.StudySession
import com.example.taskmate.data.model.Subject
import com.example.taskmate.data.model.Task
import com.example.taskmate.data.repository.StudyRepository
import com.example.taskmate.utils.DateUtils
import com.example.taskmate.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val userId = firebaseAuth.currentUser?.uid ?: ""

    private val _tasks = MutableLiveData<Resource<List<Task>>>()
    val tasks: LiveData<Resource<List<Task>>> = _tasks

    private val _currentSubject = MutableLiveData<Subject?>()
    val currentSubject: LiveData<Subject?> = _currentSubject

    init {
        if (userId.isNotBlank()) {
            loadTodayTasks()
        }
    }

    private fun loadTodayTasks() {
        // Use getTasksForUser instead — shows all tasks not just today's
        studyRepository.getTasksForUser(userId)
            .onEach { resource ->
                if (resource is Resource.Success) {
                    // Filter to incomplete tasks only for focus mode
                    val incompleteTasks = resource.data.filter { !it.completed }
                    _tasks.value = Resource.Success(incompleteTasks)
                } else {
                    _tasks.value = resource
                }
            }
            .launchIn(viewModelScope)
    }

    fun setCurrentSubject(subject: Subject?) {
        _currentSubject.value = subject
    }

    fun toggleTask(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            studyRepository.toggleTaskComplete(userId, taskId, completed)
        }
    }

    /**
     * Records a completed Pomodoro session.
     * Called from FocusFragment when the timer reaches zero
     * or when the user taps Skip.
     */
    fun recordSessionComplete(durationMinutes: Int = 25) {
        if (userId.isBlank()) return

        val now = System.currentTimeMillis()
        val session = StudySession(
            userId          = userId,
            subjectId       = _currentSubject.value?.id ?: "",
            subjectName     = _currentSubject.value?.name ?: "",
            durationMinutes = durationMinutes,
            startedAt       = now - (durationMinutes * 60 * 1000L),
            completedAt     = now,
            wasCompleted    = true,
            dateString      = StudySession.formatDate(now)
        )

        viewModelScope.launch {
            studyRepository.recordSession(userId, session)
            // UPDATE STREAK after recording session
            studyRepository.updateStreak(userId)
        }
    }
}