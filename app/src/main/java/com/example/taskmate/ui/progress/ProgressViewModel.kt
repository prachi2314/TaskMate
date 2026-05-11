package com.example.taskmate.ui.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.taskmate.data.model.Subject
import com.example.taskmate.data.model.Task
import com.example.taskmate.data.repository.StudyRepository
import com.example.taskmate.utils.DateUtils
import com.example.taskmate.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val userId = firebaseAuth.currentUser?.uid ?: ""

    // ── Subjects ───────────────────────────────────────────────────
    private val _subjects = MutableLiveData<Resource<List<Subject>>>()
    val subjects: LiveData<Resource<List<Subject>>> = _subjects

    // ── Tasks (for tasks done ring) ────────────────────────────────
    private val _tasks = MutableLiveData<Resource<List<Task>>>()
    val tasks: LiveData<Resource<List<Task>>> = _tasks

    // ── Weekly goals — calculated from real data ───────────────────
    private val _weeklyGoals = MutableLiveData<WeeklyGoals>()
    val weeklyGoals: LiveData<WeeklyGoals> = _weeklyGoals

    // ── Calendar heatmap data ──────────────────────────────────────
    private val _calendarData = MutableLiveData<Map<String, Int>>()
    val calendarData: LiveData<Map<String, Int>> = _calendarData

    // ── Days until next exam ───────────────────────────────────────
    private val _daysUntilExam = MutableLiveData<Int>()
    val daysUntilExam: LiveData<Int> = _daysUntilExam

    init {
        if (userId.isNotBlank()) {
            loadSubjects()
            loadTasks()
            loadWeeklyGoals()
            loadCalendarData()
        }
    }

    // ── Load functions ─────────────────────────────────────────────

    private fun loadSubjects() {
        studyRepository.getSubjectsForUser(userId)
            .onEach { resource ->
                _subjects.value = resource

                if (resource is Resource.Success) {
                    // Calculate nearest exam
                    val nearestExam = resource.data
                        .mapNotNull { it.daysUntilExam }
                        .filter { it >= 0 }
                        .minOrNull()
                    _daysUntilExam.value = nearestExam ?: 30
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadTasks() {
        studyRepository.getTasksForUser(userId)
            .onEach { resource ->
                _tasks.value = resource
                // Recalculate goals when tasks update
                recalculateWeeklyGoals()
            }
            .launchIn(viewModelScope)
    }

    private fun loadWeeklyGoals() {
        viewModelScope.launch {
            recalculateWeeklyGoals()
        }
    }

    /**
     * Calculates the three ring percentages from REAL data:
     *
     * Study hours ring:
     *   Actual study minutes this week / target (20 hours) × 100
     *
     * Tasks ring:
     *   Completed tasks today / total tasks today × 100
     *
     * Revision ring:
     *   Average subject progress across all subjects
     */
    private fun recalculateWeeklyGoals() {
        viewModelScope.launch {
            // ── Study hours ring ───────────────────────────────────
            val sessionResult = studyRepository.getSessionsThisWeek(
                userId      = userId,
                weekStartMs = DateUtils.startOfCurrentWeek()
            )

            val studyMinutesThisWeek = if (sessionResult is Resource.Success) {
                sessionResult.data.sumOf { it.durationMinutes }
            } else 0

            // Target: 20 hours per week = 1200 minutes
            val targetMinutes = 1200
            val studyPercent = ((studyMinutesThisWeek.toFloat() / targetMinutes) * 100f)
                .coerceIn(0f, 100f)

            // ── Tasks ring (today's tasks) ─────────────────────────
            val taskResource = _tasks.value
            val taskPercent = if (taskResource is Resource.Success &&
                taskResource.data.isNotEmpty()) {
                val total     = taskResource.data.size
                val completed = taskResource.data.count { it.completed }
                ((completed.toFloat() / total.toFloat()) * 100f)
                    .coerceIn(0f, 100f)
            } else 0f

            // ── Revision ring (average subject progress) ───────────
            val subjectResource = _subjects.value
            val revisionPercent = if (subjectResource is Resource.Success &&
                subjectResource.data.isNotEmpty()) {
                subjectResource.data
                    .map { it.progressPercent.toFloat() }
                    .average()
                    .toFloat()
                    .coerceIn(0f, 100f)
            } else 0f

            _weeklyGoals.value = WeeklyGoals(
                studyHoursPercent = studyPercent,
                tasksPercent      = taskPercent,
                revisionPercent   = revisionPercent
            )
        }
    }

    private fun loadCalendarData() {
        viewModelScope.launch {
            val currentMonth = SimpleDateFormat(
                "yyyy-MM",
                Locale.getDefault()
            ).format(Date())

            val result = studyRepository.getSessionsByDate(userId, currentMonth)
            if (result is Resource.Success) {
                _calendarData.value = result.data
            }
        }
    }
}

/**
 * Holds the three weekly goal ring percentages.
 * All values are 0–100 floats.
 */
data class WeeklyGoals(
    val studyHoursPercent: Float,   // based on actual session minutes
    val tasksPercent: Float,        // based on today's completed tasks
    val revisionPercent: Float      // based on average subject progress
)