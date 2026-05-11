package com.example.taskmate.ui.subject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.taskmate.data.model.RevisionEntry
import com.example.taskmate.data.model.StudySession
import com.example.taskmate.data.model.Subject
import com.example.taskmate.data.repository.StudyRepository
import com.example.taskmate.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubjectViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val userId = firebaseAuth.currentUser?.uid ?: ""

    private val _subjects = MutableLiveData<Resource<List<Subject>>>()
    val subjects: LiveData<Resource<List<Subject>>> = _subjects

    private val _addSubjectState = MutableLiveData<Resource<Unit>>()
    val addSubjectState: LiveData<Resource<Unit>> = _addSubjectState

    init {
        if (userId.isNotBlank()) {
            loadSubjects()
        }
    }

    private fun loadSubjects() {
        studyRepository.getSubjectsForUser(userId)
            .onEach { _subjects.value = it }
            .launchIn(viewModelScope)
    }

    fun addSubject(subject: Subject) {
        viewModelScope.launch {
            _addSubjectState.value = Resource.Loading
            _addSubjectState.value = studyRepository.addSubject(userId, subject)
        }
    }

    fun updateProgress(subjectId: String, completedChapters: Int) {
        viewModelScope.launch {
            studyRepository.updateSubjectProgress(
                userId,
                subjectId,
                completedChapters
            )
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            studyRepository.deleteSubject(userId, subjectId)
        }
    }

    /**
     * Logs a revision session for a subject.
     * Called from SubjectFragment showRevisionDialog().
     */
    fun logRevision(
        subjectId: String,
        subjectName: String,
        chapterNote: String
    ) {
        viewModelScope.launch {
            val now   = System.currentTimeMillis()
            val entry = RevisionEntry(
                subjectId   = subjectId,
                subjectName = subjectName,
                chapterName = chapterNote,
                revisedAt   = now,
                dateString  = StudySession.formatDate(now)
            )
            studyRepository.logRevision(userId, subjectId, entry)
        }
    }

    fun markChapterRevised(subjectId: String, chapterIndex: Int) {
        viewModelScope.launch {
            studyRepository.markChapterRevised(userId, subjectId, chapterIndex)
        }
    }
}