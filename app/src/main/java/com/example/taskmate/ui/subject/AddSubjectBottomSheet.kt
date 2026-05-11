package com.example.taskmate.ui.subject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.taskmate.data.model.Subject
import com.example.taskmate.databinding.BottomSheetAddSubjectBinding
import com.example.taskmate.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddSubjectBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "AddSubjectBottomSheet"
    }

    private var _binding: BottomSheetAddSubjectBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubjectViewModel by viewModels()

    private var selectedColorHex = "#7059D0"
    private var selectedEmoji    = "📚"

    private val presetColors = listOf(
        "#378ADD", "#7059D0", "#1D9E75",
        "#D4537E", "#BA7517", "#D85A30",
        "#E24B4A", "#085041"
    )

    private val presetEmojis = listOf(
        "📚", "⚛️", "📐", "🧪", "🏛️", "📖",
        "🖥️", "🌍", "🔬", "📊", "🎭", "🎨"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddSubjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEmojiPicker()
        setupColorPicker()
        setupClickListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupEmojiPicker() {
        binding.llEmojiPicker.removeAllViews()
        presetEmojis.forEach { emoji ->
            val btn = android.widget.TextView(requireContext()).apply {
                text     = emoji
                textSize = 24f
                setPadding(12, 8, 12, 8)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    selectedEmoji = emoji
                    binding.tvSelectedEmoji.text = emoji
                    updateEmojiSelection(this)
                }
            }
            binding.llEmojiPicker.addView(btn)
        }
    }

    private fun setupColorPicker() {
        binding.llColorPicker.removeAllViews()
        presetColors.forEach { colorHex ->
            val size = (36 * resources.displayMetrics.density).toInt()
            val circle = android.view.View(requireContext()).apply {
                layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                    marginEnd = (8 * resources.displayMetrics.density).toInt()
                }
                val drawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(android.graphics.Color.parseColor(colorHex))
                }
                background  = drawable
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    selectedColorHex = colorHex
                    binding.viewColorPreview.setBackgroundColor(
                        android.graphics.Color.parseColor(colorHex)
                    )
                    updateColorSelection(this)
                }
            }
            binding.llColorPicker.addView(circle)
        }
        binding.viewColorPreview.setBackgroundColor(
            android.graphics.Color.parseColor(selectedColorHex)
        )
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnSaveSubject.setOnClickListener { saveSubject() }
        binding.btnPickExamDate.setOnClickListener { showDatePicker() }
    }

    private var selectedExamDateMs = 0L

    private fun showDatePicker() {
        val picker = com.google.android.material.datepicker.MaterialDatePicker
            .Builder.datePicker()
            .setTitleText("Select exam date")
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            selectedExamDateMs       = selection
            binding.btnPickExamDate.text = picker.headerText
        }
        picker.show(parentFragmentManager, "ExamDatePicker")
    }

    private fun saveSubject() {
        val name = binding.etSubjectName.text.toString().trim()
        if (name.isBlank()) {
            binding.tilSubjectName.error = "Subject name is required"
            return
        }
        binding.tilSubjectName.error = null

        val totalChaptersStr = binding.etTotalChapters.text.toString().trim()
        val totalChapters    = totalChaptersStr.toIntOrNull() ?: 0

        // Parse chapter names from the input field
        // User enters one chapter per line
        val chapterNamesRaw = binding.etChapterNames.text.toString().trim()
        val chapterNames = if (chapterNamesRaw.isNotBlank()) {
            chapterNamesRaw
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
        } else emptyList()

        // If user entered chapter names but no total — use count of names
        val finalTotal = when {
            totalChapters > 0 -> totalChapters
            chapterNames.isNotEmpty() -> chapterNames.size
            else -> 0
        }

        val subject = Subject(
            name          = name,
            shortName     = name.take(4),
            colorHex      = selectedColorHex,
            emoji         = selectedEmoji,
            totalChapters = finalTotal,
            chapterNames  = chapterNames,
            examDate      = selectedExamDateMs,
            notes         = binding.etSubjectNotes.text.toString().trim()
        )

        viewModel.addSubject(subject)
    }

    private fun observeViewModel() {
        viewModel.addSubjectState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnSaveSubject.isEnabled = false
                    binding.btnSaveSubject.text      = "Saving…"
                }
                is Resource.Success -> {
                    binding.btnSaveSubject.isEnabled = true
                    binding.btnSaveSubject.text      = "Save subject"
                    Toast.makeText(requireContext(), "Subject added!", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                is Resource.Error -> {
                    binding.btnSaveSubject.isEnabled = true
                    binding.btnSaveSubject.text      = "Save subject"
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateEmojiSelection(selected: android.widget.TextView) {
        for (i in 0 until binding.llEmojiPicker.childCount) {
            (binding.llEmojiPicker.getChildAt(i) as? android.widget.TextView)
                ?.alpha = 0.4f
        }
        selected.alpha = 1f
    }

    private fun updateColorSelection(selected: android.view.View) {
        for (i in 0 until binding.llColorPicker.childCount) {
            (binding.llColorPicker.getChildAt(i).background
                    as? android.graphics.drawable.GradientDrawable)
                ?.setStroke(0, android.graphics.Color.TRANSPARENT)
        }
        (selected.background as? android.graphics.drawable.GradientDrawable)
            ?.setStroke(
                (3 * resources.displayMetrics.density).toInt(),
                android.graphics.Color.WHITE
            )
    }
}