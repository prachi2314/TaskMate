package com.example.taskmate.ui.dashboard

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.taskmate.R
import com.example.taskmate.data.model.Subject
import com.example.taskmate.data.model.Task
import com.example.taskmate.databinding.BottomSheetAddTaskBinding
import com.example.taskmate.utils.DateUtils
import com.example.taskmate.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

/**
 * AddTaskBottomSheet.kt
 * Location: ui/dashboard/AddTaskBottomSheet.kt
 *
 * Bottom sheet for adding a new task.
 * Opened from DashboardFragment FAB.
 */
@AndroidEntryPoint
class AddTaskBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "AddTaskBottomSheet"
    }

    private var _binding: BottomSheetAddTaskBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    // ── State ──────────────────────────────────────────────────────
    private var selectedSubject: Subject? = null
    private var selectedDueDateMs: Long   = 0L
    private var selectedPriority: Int     = Task.PRIORITY_LOW
    private var subjectList: List<Subject> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPriorityChips()
        setupClickListeners()
        observeSubjects()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ══════════════════════════════════════════════════════════════
    //  SETUP
    // ══════════════════════════════════════════════════════════════

    private fun setupPriorityChips() {
        binding.chipPriorityLow.setOnClickListener {
            selectedPriority = Task.PRIORITY_LOW
        }
        binding.chipPriorityMedium.setOnClickListener {
            selectedPriority = Task.PRIORITY_MEDIUM
        }
        binding.chipPriorityHigh.setOnClickListener {
            selectedPriority = Task.PRIORITY_HIGH
        }
    }

    private fun setupClickListeners() {
        binding.btnCloseTaskSheet.setOnClickListener { dismiss() }
        binding.btnPickDueDate.setOnClickListener    { showDatePicker() }
        binding.btnSaveTask.setOnClickListener       { saveTask() }
    }

    private fun observeSubjects() {
        viewModel.subjects.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                subjectList      = resource.data
                val subjectNames = resource.data.map { it.name }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    subjectNames
                )
                binding.acvTaskSubject.setAdapter(adapter)
                binding.acvTaskSubject.setOnItemClickListener { _, _, position, _ ->
                    selectedSubject = resource.data.getOrNull(position)
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DATE PICKER
    // ══════════════════════════════════════════════════════════════

    private fun showDatePicker() {
        val cal = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                cal.set(year, month, day, 23, 59, 59)
                selectedDueDateMs = cal.timeInMillis
                binding.btnPickDueDate.text =
                    "$day ${getMonthName(month)} $year"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).also { dialog ->
            // Cannot pick past dates
            dialog.datePicker.minDate =
                System.currentTimeMillis() - 1000
        }.show()
    }

    private fun getMonthName(month: Int): String {
        return listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        ).getOrElse(month) { "" }
    }

    // ══════════════════════════════════════════════════════════════
    //  SAVE TASK
    // ══════════════════════════════════════════════════════════════

    private fun saveTask() {
        val title       = binding.etTaskTitle.text.toString().trim()
        val description = binding.etTaskDescription.text.toString().trim()

        // Validate title
        if (title.isBlank()) {
            binding.tilTaskTitle.error = "Task title is required"
            binding.etTaskTitle.requestFocus()
            return
        }
        binding.tilTaskTitle.error = null

        // If no due date selected default to today end of day
        val dueDateFinal = if (selectedDueDateMs == 0L) {
            DateUtils.endOfToday()
        } else {
            selectedDueDateMs
        }

        // Build Task object
        // All fields match Task.kt data class exactly
        val task = Task(
            id              = "",                           // Firestore generates this
            userId          = "",                           // Repository fills this
            title           = title,
            description     = description,
            subjectId       = selectedSubject?.id ?: "",
            subjectName     = selectedSubject?.name ?: "",
            subjectColorHex = selectedSubject?.colorHex ?: "#7059D0",
            dueDate         = dueDateFinal,
            completed       = false,                        // new tasks start incomplete
            completedAt     = 0L,
            priority        = selectedPriority,
            createdAt       = System.currentTimeMillis()
        )

        // Disable button to prevent double tap
        binding.btnSaveTask.isEnabled = false
        binding.btnSaveTask.text      = "Adding…"

        // Pass context for scheduling notification
        viewModel.addTask(requireContext(), task)

        // Dismiss after short delay — Firestore write is fast
        binding.root.postDelayed({
            if (isAdded) {
                Toast.makeText(
                    requireContext(),
                    "Task added!",
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            }
        }, 700)
    }
}