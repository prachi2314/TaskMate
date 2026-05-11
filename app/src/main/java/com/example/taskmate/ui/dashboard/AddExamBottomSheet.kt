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
import com.example.taskmate.data.model.Exam
import com.example.taskmate.data.model.Subject
import com.example.taskmate.databinding.BottomSheetAddExamBinding
import com.example.taskmate.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class AddExamBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "AddExamBottomSheet"
    }

    private var _binding: BottomSheetAddExamBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    private var selectedSubject: Subject? = null
    private var selectedExamDateMs: Long  = 0L
    private var subjectList: List<Subject> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddExamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeSubjects()
    }

    private fun setupClickListeners() {
        binding.btnCloseExamSheet.setOnClickListener { dismiss() }
        binding.btnPickExamDate.setOnClickListener   { showDatePicker() }
        binding.btnSaveExam.setOnClickListener       { saveExam() }
    }

    private fun observeSubjects() {
        viewModel.subjects.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                subjectList = resource.data
                val names   = resource.data.map { it.name }
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    names
                )
                binding.acvExamSubject.setAdapter(adapter)
                binding.acvExamSubject.setOnItemClickListener { _, _, position, _ ->
                    selectedSubject = resource.data.getOrNull(position)
                }
            }
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                cal.set(year, month, day, 23, 59, 59)
                selectedExamDateMs = cal.timeInMillis
                binding.btnPickExamDate.text =
                    "$day ${getMonthName(month)} $year"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).also { dialog ->
            dialog.datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    private fun saveExam() {
        val title = binding.etExamTitle.text.toString().trim()

        if (title.isBlank()) {
            binding.tilExamTitle.error = "Exam title is required"
            return
        }
        if (selectedExamDateMs == 0L) {
            Toast.makeText(requireContext(), "Please select exam date", Toast.LENGTH_SHORT).show()
            return
        }

        val exam = Exam(
            title           = title,
            subjectId       = selectedSubject?.id ?: "",
            subjectName     = selectedSubject?.name ?: "",
            subjectColorHex = selectedSubject?.colorHex ?: "#7059D0",
            examDate        = selectedExamDateMs,
            location        = binding.etExamLocation.text.toString().trim(),
            notes           = binding.etExamNotes.text.toString().trim()
        )

        binding.btnSaveExam.isEnabled = false
        binding.btnSaveExam.text      = "Saving…"

        viewModel.addExam(requireContext(), exam)

        binding.root.postDelayed({
            if (isAdded) {
                Toast.makeText(requireContext(), "Exam added!", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }, 600)
    }

    private fun getMonthName(month: Int) = listOf(
        "Jan","Feb","Mar","Apr","May","Jun",
        "Jul","Aug","Sep","Oct","Nov","Dec"
    ).getOrElse(month) { "" }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}