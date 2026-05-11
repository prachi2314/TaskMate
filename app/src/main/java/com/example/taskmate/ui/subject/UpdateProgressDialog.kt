package com.example.taskmate.ui.subject

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.taskmate.data.model.Subject
import com.example.taskmate.databinding.DialogUpdateProgressBinding
import com.example.taskmate.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * UpdateProgressDialog.kt
 * Location: ui/subject/UpdateProgressDialog.kt
 *
 * A simple AlertDialog for updating chapter completion count.
 * Shows a number picker (SeekBar) pre-filled with current value.
 *
 * Usage:
 *   UpdateProgressDialog.newInstance(subject)
 *       .show(childFragmentManager, "UpdateProgress")
 */
@AndroidEntryPoint
class UpdateProgressDialog : DialogFragment() {

    companion object {
        private const val ARG_SUBJECT_ID    = "subjectId"
        private const val ARG_SUBJECT_NAME  = "subjectName"
        private const val ARG_TOTAL_CHAPTERS = "totalChapters"
        private const val ARG_DONE_CHAPTERS  = "completedChapters"

        const val TAG = "UpdateProgressDialog"

        /**
         * Factory method — creates the dialog with the subject data
         * passed as arguments (survives rotation).
         */
        fun newInstance(subject: Subject): UpdateProgressDialog {
            return UpdateProgressDialog().apply {
                arguments = bundleOf(
                    ARG_SUBJECT_ID     to subject.id,
                    ARG_SUBJECT_NAME   to subject.name,
                    ARG_TOTAL_CHAPTERS to subject.totalChapters,
                    ARG_DONE_CHAPTERS  to subject.completedChapters
                )
            }
        }
    }

    private var _binding: DialogUpdateProgressBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubjectViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogUpdateProgressBinding.inflate(layoutInflater)

        // ── Unpack arguments ───────────────────────────────────────
        val subjectId      = arguments?.getString(ARG_SUBJECT_ID) ?: ""
        val subjectName    = arguments?.getString(ARG_SUBJECT_NAME) ?: ""
        val totalChapters  = arguments?.getInt(ARG_TOTAL_CHAPTERS) ?: 0
        val doneChapters   = arguments?.getInt(ARG_DONE_CHAPTERS) ?: 0

        // ── Setup UI ───────────────────────────────────────────────
        binding.tvDialogSubjectName.text = subjectName
        binding.tvChapterCount.text = "$doneChapters / $totalChapters"

        // Configure SeekBar
        binding.seekbarChapters.max      = totalChapters
        binding.seekbarChapters.progress = doneChapters

        binding.seekbarChapters.setOnSeekBarChangeListener(
            object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: android.widget.SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    binding.tvChapterCount.text = "$progress / $totalChapters"
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            }
        )

        // ── Observe save result ────────────────────────────────────
        viewModel.addSubjectState.observe(this) { resource ->
            if (resource is Resource.Success) {
                Toast.makeText(requireContext(), "Progress updated!", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }

        // ── Build dialog ───────────────────────────────────────────
        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                val newProgress = binding.seekbarChapters.progress
                viewModel.updateProgress(subjectId, newProgress)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}