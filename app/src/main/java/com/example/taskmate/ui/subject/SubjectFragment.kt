package com.example.taskmate.ui.subject

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskmate.R
import com.example.taskmate.data.model.Subject
import com.example.taskmate.databinding.FragmentSubjectBinding
import com.example.taskmate.ui.adapters.SubjectAdapter
import com.example.taskmate.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * SubjectFragment.kt
 * Location: ui/subject/SubjectFragment.kt
 *
 * Shows list of all subjects.
 * Tapping a subject card navigates to SubjectDetailFragment.
 * + and − buttons update chapter completion directly.
 * Mark as Revised button logs a revision session.
 */
@AndroidEntryPoint
class SubjectFragment : Fragment() {

    private var _binding: FragmentSubjectBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubjectViewModel by viewModels()
    private lateinit var subjectAdapter: SubjectAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ══════════════════════════════════════════════════════════════
    //  SETUP
    // ══════════════════════════════════════════════════════════════

    private fun setupRecyclerView() {
        subjectAdapter = SubjectAdapter(

            // + button — increment chapter by 1
            onChapterIncrement = { subject ->
                handleIncrement(subject)
            },

            // − button — decrement chapter by 1
            onChapterDecrement = { subject ->
                handleDecrement(subject)
            },

            // Mark as revised button
            onMarkRevised = { subject ->
                showRevisionDialog(subject)
            },

            // Card tap — navigate to chapter detail screen
            onCardClick = { subject ->
                navigateToDetail(subject)
            },

            // Long press — delete with confirmation
            onDeleteClick = { subject ->
                showDeleteConfirmation(subject)
            }
        )

        binding.rvSubjectsList.apply {
            adapter       = subjectAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.fabAddSubject.setOnClickListener {
            AddSubjectBottomSheet()
                .show(childFragmentManager, "AddSubject")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════════

    /**
     * Navigates to SubjectDetailFragment passing the subject ID.
     *
     * findNavController() works correctly here because:
     *  1. This Fragment is hosted inside NavHostFragment
     *  2. onViewCreated has been called (view exists)
     *  3. We call findNavController() which is imported from
     *     androidx.navigation.fragment.findNavController
     *
     * The subject ID is passed as a Bundle argument.
     * SubjectDetailFragment reads it via arguments?.getString("subjectId")
     */
    private fun navigateToDetail(subject: Subject) {
        try {
            val bundle = Bundle().apply {
                putString("subjectId", subject.id)
            }
            findNavController().navigate(
                R.id.action_subject_to_detail,
                bundle
            )
        } catch (e: Exception) {
            android.util.Log.e(
                "SubjectFragment",
                "Navigation failed: ${e.message}"
            )
            Toast.makeText(
                requireContext(),
                "Could not open subject details",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SUBJECT ACTIONS
    // ══════════════════════════════════════════════════════════════

    private fun handleIncrement(subject: Subject) {
        if (subject.totalChapters == 0 ||
            subject.completedChapters < subject.totalChapters) {
            viewModel.updateProgress(
                subject.id,
                subject.completedChapters + 1
            )
        } else {
            Toast.makeText(
                requireContext(),
                "All chapters already completed!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleDecrement(subject: Subject) {
        if (subject.completedChapters > 0) {
            viewModel.updateProgress(
                subject.id,
                subject.completedChapters - 1
            )
        }
    }

    private fun showDeleteConfirmation(subject: Subject) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Subject")
            .setMessage(
                "Delete \"${subject.name}\"?\n\n" +
                        "All chapter progress and revision data will be lost."
            )
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSubject(subject.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ══════════════════════════════════════════════════════════════
    //  REVISION DIALOG
    // ══════════════════════════════════════════════════════════════

    private fun showRevisionDialog(subject: Subject) {
        val input = EditText(requireContext()).apply {
            hint      = "Chapter name or note (optional)"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(48, 24, 48, 8)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Log Revision")
            .setMessage("Recording revision for: ${subject.name}")
            .setView(input)
            .setPositiveButton("Log Revision") { _, _ ->
                val note = input.text.toString().trim()
                viewModel.logRevision(
                    subjectId   = subject.id,
                    subjectName = subject.name,
                    chapterNote = note
                )
                Toast.makeText(
                    requireContext(),
                    "Revision logged for ${subject.name}!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ══════════════════════════════════════════════════════════════
    //  OBSERVE
    // ══════════════════════════════════════════════════════════════

    private fun observeViewModel() {
        viewModel.subjects.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> { }

                is Resource.Success -> {
                    subjectAdapter.submitList(resource.data)
                    val count = resource.data.size
                    binding.tvSubjectCount.text =
                        "$count subject${if (count != 1) "s" else ""}"
                }

                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        resource.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}