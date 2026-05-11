package com.example.taskmate.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.taskmate.data.model.Exam
import com.example.taskmate.data.model.Subject
import com.example.taskmate.data.model.Task
import com.example.taskmate.data.repository.AuthRepository
import com.example.taskmate.data.repository.StudyRepository
import com.example.taskmate.utils.Constants
import com.example.taskmate.utils.DateUtils
import com.example.taskmate.utils.Resource
import com.example.taskmate.utils.TaskNotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * DashboardViewModel.kt
 * Location: ui/dashboard/DashboardViewModel.kt
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val userId = firebaseAuth.currentUser?.uid ?: ""

    // ── LiveData ───────────────────────────────────────────────────
    private val _tasks = MutableLiveData<Resource<List<Task>>>()
    val tasks: LiveData<Resource<List<Task>>> = _tasks

    private val _subjects = MutableLiveData<Resource<List<Subject>>>()
    val subjects: LiveData<Resource<List<Subject>>> = _subjects

    private val _upcomingExams = MutableLiveData<Resource<List<Exam>>>()
    val upcomingExams: LiveData<Resource<List<Exam>>> = _upcomingExams

    private val _currentUserName = MutableLiveData<String>()
    val currentUserName: LiveData<String> = _currentUserName

    private val _studyStats = MutableLiveData<StudyStats>()
    val studyStats: LiveData<StudyStats> = _studyStats

    init {
        loadUserName()
        loadTodayTasks()
        loadSubjects()
        loadExams()
        loadStudyStats()
    }

    // ── Load functions ─────────────────────────────────────────────

    private fun loadUserName() {
        _currentUserName.value =
            firebaseAuth.currentUser?.displayName ?: "Student"
    }

    private fun loadTodayTasks() {
        // Load ALL tasks — not just today's date range
        // This fixes the issue where tasks without due dates don't show
        studyRepository.getTasksForUser(userId)
            .onEach { resource ->
                if (resource is Resource.Success) {
                    val today = com.example.taskmate.utils.DateUtils.startOfToday()
                    val tomorrow = today + 86400000L

                    // Show tasks that are:
                    // 1. Have due date today
                    // 2. Are overdue (past due date, not completed)
                    // 3. Have NO due date set (dueDate = 0)
                    val todayTasks = resource.data.filter { task ->
                        when {
                            task.dueDate == 0L -> true  // no due date — always show
                            task.dueDate in today until tomorrow -> true  // due today
                            task.dueDate < today && !task.completed -> true  // overdue
                            else -> false
                        }
                    }.sortedBy { it.createdAt }

                    _tasks.value = Resource.Success(todayTasks)
                } else {
                    _tasks.value = resource
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadSubjects() {
        studyRepository.getSubjectsForUser(userId)
            .onEach { _subjects.value = it }
            .launchIn(viewModelScope)
    }

    private fun loadExams() {
        studyRepository.getExamsForUser(userId)
            .onEach { resource ->
                // Filter to only upcoming exams (not past)
                if (resource is Resource.Success) {
                    val upcoming = resource.data
                        .filter { it.isUpcoming }
                        .sortedBy { it.examDate }
                        .take(3) // show max 3 exams on dashboard
                    _upcomingExams.value = Resource.Success(upcoming)
                } else {
                    _upcomingExams.value = resource
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadStudyStats() {
        viewModelScope.launch {
            val result = studyRepository.getSessionsThisWeek(
                userId       = userId,
                weekStartMs  = DateUtils.startOfCurrentWeek()
            )
            if (result is Resource.Success) {
                val todaySessions = result.data.filter { it.isToday }
                val todayMinutes  = todaySessions.sumOf { it.durationMinutes }

                // Calculate overall subject progress
                val subjectsData = (_subjects.value as? Resource.Success)?.data ?: emptyList()
                val overallPercent = if (subjectsData.isEmpty()) 0
                else subjectsData.map { it.progressPercent }.average().toInt()

                _studyStats.value = StudyStats(
                    todayHours     = DateUtils.minutesToHoursLabel(todayMinutes),
                    streakDays     = 0, // loaded from user profile
                    overallPercent = overallPercent
                )
            }
        }
    }

    // ── Task actions ───────────────────────────────────────────────

    fun toggleTask(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            studyRepository.toggleTaskComplete(userId, taskId, completed)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            studyRepository.deleteTask(userId, taskId)
        }
    }

    fun addTask(context: android.content.Context, task: Task) {
        viewModelScope.launch {
            val result = studyRepository.addTask(userId, task)
            if (result is Resource.Success) {
                com.example.taskmate.utils.TaskNotificationScheduler
                    .scheduleTaskReminder(context, task)
            }
        }
    }

    fun addExam(context: android.content.Context, exam: Exam) {
        viewModelScope.launch {
            val result = studyRepository.addExam(userId, exam)
            if (result is Resource.Success) {
                // Now exam.id is NOT empty — scheduling works correctly
                TaskNotificationScheduler.scheduleExamReminders(
                    context,
                    result.data  // use the returned exam with ID
                )
            }
        }
    }

    fun signOut() = authRepository.signOut()
}

/**
 * Data class holding the three Dashboard stat card values.
 */
data class StudyStats(
    val todayHours: String,
    val streakDays: Int,
    val overallPercent: Int
)