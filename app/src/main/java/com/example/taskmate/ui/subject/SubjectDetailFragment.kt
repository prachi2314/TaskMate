package com.example.taskmate.ui.subject

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskmate.data.model.Subject
import com.example.taskmate.databinding.FragmentSubjectDetailBinding
import com.example.taskmate.ui.adapters.ChapterAdapter
import com.example.taskmate.ui.adapters.ChapterItem
import com.example.taskmate.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * SubjectDetailFragment.kt
 * Location: ui/subject/SubjectDetailFragment.kt
 *
 * Shows all chapters of a subject.
 * Opened by tapping a subject card in SubjectFragment.
 *
 * Navigation:
 *   SubjectFragment → (action_subject_to_detail) → SubjectDetailFragment
 *
 * Receives:
 *   arguments.getString("subjectId") — the Firestore document ID
 */
@AndroidEntryPoint
class SubjectDetailFragment : Fragment() {

    private var _binding: FragmentSubjectDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubjectViewModel by viewModels()
    private lateinit var chapterAdapter: ChapterAdapter

    // Subject ID passed via Navigation arguments
    private val subjectId: String by lazy {
        arguments?.getString("subjectId") ?: ""
    }

    // Keep reference to current subject for chapter operations
    private var currentSubject: Subject? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubjectDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (subjectId.isBlank()) {
            Toast.makeText(requireContext(), "Subject not found", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

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
        chapterAdapter = ChapterAdapter(

            // Checkbox tapped — mark chapter complete or incomplete
            onChapterChecked = { index, isChecked ->
                val subject = currentSubject ?: return@ChapterAdapter

                val newCompleted = if (isChecked) {
                    (subject.completedChapters + 1)
                        .coerceAtMost(subject.totalChapters)
                } else {
                    (subject.completedChapters - 1)
                        .coerceAtLeast(0)
                }

                viewModel.updateProgress(subject.id, newCompleted)
            },

            // Long press — mark chapter as revised
            onMarkRevised = { index ->
                val subject     = currentSubject ?: return@ChapterAdapter
                val chapterName = subject.chapterNames.getOrNull(index)
                    ?: "Chapter ${index + 1}"

                AlertDialog.Builder(requireContext())
                    .setTitle("Mark as Revised")
                    .setMessage(
                        "Mark \"$chapterName\" as revised?\n\n" +
                                "This will update your revision progress."
                    )
                    .setPositiveButton("Mark Revised") { _, _ ->
                        viewModel.markChapterRevised(subject.id, index)
                        Toast.makeText(
                            requireContext(),
                            "\"$chapterName\" marked as revised!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.rvChapters.apply {
            adapter       = chapterAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        // Back button — use NavController to go back correctly
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  OBSERVE
    // ══════════════════════════════════════════════════════════════

    private fun observeViewModel() {
        viewModel.subjects.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Optional: show loading skeleton
                }

                is Resource.Success -> {
                    // Find this specific subject by ID
                    val subject = resource.data.find { it.id == subjectId }

                    if (subject == null) {
                        Toast.makeText(
                            requireContext(),
                            "Subject not found",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().popBackStack()
                        return@observe
                    }

                    currentSubject = subject
                    bindSubjectHeader(subject)
                    buildChapterList(subject)
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

    // ══════════════════════════════════════════════════════════════
    //  BIND DATA
    // ══════════════════════════════════════════════════════════════

    private fun bindSubjectHeader(subject: Subject) {
        binding.tvDetailEmoji.text       = subject.emoji
        binding.tvDetailSubjectName.text = subject.name
        binding.tvDetailProgress.text    =
            "${subject.completedChapters} / ${subject.totalChapters} chapters done"
        binding.progressDetailBar.progress = subject.progressPercent
        binding.tvDetailPercent.text       = subject.progressLabel
        binding.tvRevisionPercent.text     =
            "Revised: ${subject.revisionPercent}%"

        // Apply subject color to progress bar
        try {
            val color = android.graphics.Color.parseColor(subject.colorHex)
            binding.progressDetailBar.setIndicatorColor(color)
        } catch (e: IllegalArgumentException) { }
    }

    /**
     * Builds the chapter list from subject data.
     *
     * Each chapter shows:
     *  - Chapter number (1, 2, 3...)
     *  - Chapter name (from chapterNames list or "Chapter N" fallback)
     *  - Completed state (checkbox)
     *  - Revised badge (if in revisedChapters list)
     */
    private fun buildChapterList(subject: Subject) {
        // Use the larger of totalChapters or chapterNames.size
        val count = maxOf(subject.totalChapters, subject.chapterNames.size)

        if (count == 0) {
            binding.tvNoChapters.visibility = View.VISIBLE
            binding.rvChapters.visibility   = View.GONE
            return
        }

        binding.tvNoChapters.visibility = View.GONE
        binding.rvChapters.visibility   = View.VISIBLE

        val chapters = (0 until count).map { index ->
            ChapterItem(
                index       = index,
                name        = subject.chapterNames.getOrNull(index)
                    ?: "Chapter ${index + 1}",
                isCompleted = index < subject.completedChapters,
                isRevised   = subject.revisedChapters.contains(index)
            )
        }

        chapterAdapter.submitList(chapters)
    }
}