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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskmate.R
import com.example.taskmate.data.model.Subject
import com.example.taskmate.databinding.FragmentSubjectBinding
import com.example.taskmate.ui.adapters.SubjectAdapter
import com.example.taskmate.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

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
            onChapterIncrement = { subject ->
                handleIncrement(subject)
            },
            onChapterDecrement = { subject ->
                handleDecrement(subject)
            },
            onMarkRevised = { subject ->
                showRevisionDialog(subject)
            },
            onCardClick = { subject ->
                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.nav_host_fragment,
                        SubjectDetailFragment.newInstance(subject.id)
                    )
                    .addToBackStack(null)
                    .commit()
            },
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
                "All chapters completed!",
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

    private fun openUpdateDialog(subject: Subject) {
        UpdateProgressDialog
            .newInstance(subject)
            .show(childFragmentManager, UpdateProgressDialog.TAG)
    }

    private fun showDeleteConfirmation(subject: Subject) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Subject")
            .setMessage(
                "Delete \"${subject.name}\"?\n" +
                        "All progress data will be lost."
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
            .setTitle("Log Revision — ${subject.name}")
            .setMessage("This records that you revised ${subject.name} today.")
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