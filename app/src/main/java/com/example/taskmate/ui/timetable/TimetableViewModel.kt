package com.example.taskmate.ui.timetable

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.taskmate.data.model.ScheduleSlot
import com.example.taskmate.data.model.Subject
import com.example.taskmate.data.repository.StudyRepository
import com.example.taskmate.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val userId = firebaseAuth.currentUser?.uid ?: ""

    // ── Schedule slots ─────────────────────────────────────────────
    private val _scheduleSlots = MutableLiveData<Resource<List<ScheduleSlot>>>()
    val scheduleSlots: LiveData<Resource<List<ScheduleSlot>>> = _scheduleSlots

    // ── Add slot state — Resource<Unit> (no data needed) ──────────
    // This is Resource<Unit> NOT Resource<ScheduleSlot>
    // The slot data is kept in AddSlotBottomSheet.pendingSlot
    private val _addSlotState = MutableLiveData<Resource<Unit>>()
    val addSlotState: LiveData<Resource<Unit>> = _addSlotState

    // ── Subjects for dropdown in AddSlotBottomSheet ────────────────
    private val _subjectsForDropdown = MutableLiveData<List<Subject>>()
    val subjectsForDropdown: LiveData<List<Subject>> = _subjectsForDropdown

    // Track current listening job
    private var slotsJob: Job? = null

    init {
        if (userId.isNotBlank()) {
            loadSubjectsForDropdown()
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  LOAD SLOTS
    // ══════════════════════════════════════════════════════════════

    fun loadSlotsForDay(dayIndex: Int) {
        slotsJob?.cancel()
        _scheduleSlots.value = Resource.Loading

        slotsJob = studyRepository.getSlotsForDay(userId, dayIndex)
            .onEach { resource ->
                _scheduleSlots.value = resource
            }
            .launchIn(viewModelScope)
    }

    // ══════════════════════════════════════════════════════════════
    //  ADD SLOT
    // ══════════════════════════════════════════════════════════════

    /**
     * Adds a slot and emits Resource<Unit> on addSlotState.
     * The slot with generated ID is handled separately
     * in AddSlotBottomSheet using pendingSlot pattern.
     */
    fun addSlot(slot: ScheduleSlot) {
        viewModelScope.launch {
            _addSlotState.value = Resource.Loading

            // addScheduleSlot returns Resource<ScheduleSlot>
            // but we only care whether it succeeded here
            val result = studyRepository.addScheduleSlot(userId, slot)

            // Convert to Resource<Unit> for the ViewModel
            _addSlotState.value = when (result) {
                is Resource.Success -> Resource.Success(Unit)
                is Resource.Error   -> Resource.Error(result.message)
                is Resource.Loading -> Resource.Loading
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DELETE SLOT
    // ══════════════════════════════════════════════════════════════

    fun deleteSlot(slotId: String) {
        viewModelScope.launch {
            studyRepository.deleteScheduleSlot(userId, slotId)
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SUBJECTS FOR DROPDOWN
    // ══════════════════════════════════════════════════════════════

    private fun loadSubjectsForDropdown() {
        studyRepository.getSubjectsForUser(userId)
            .onEach { resource ->
                if (resource is Resource.Success) {
                    _subjectsForDropdown.value = resource.data
                }
            }
            .launchIn(viewModelScope)
    }
}