package com.example.taskmate.ui.subject

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskmate.data.model.Subject
import com.example.taskmate.databinding.FragmentSubjectDetailBinding
import com.example.taskmate.ui.adapters.ChapterAdapter
import com.example.taskmate.ui.adapters.ChapterItem
import com.example.taskmate.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SubjectDetailFragment : Fragment() {

    companion object {
        const val ARG_SUBJECT_ID = "subjectId"

        fun newInstance(subjectId: String): SubjectDetailFragment {
            return SubjectDetailFragment().apply {
                arguments = bundleOf(ARG_SUBJECT_ID to subjectId)
            }
        }
    }

    private var _binding: FragmentSubjectDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubjectViewModel by viewModels()
    private lateinit var chapterAdapter: ChapterAdapter

    private val subjectId by lazy {
        arguments?.getString(ARG_SUBJECT_ID) ?: ""
    }

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
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        chapterAdapter = ChapterAdapter(

            // Checkbox tapped — mark chapter complete or incomplete
            onChapterChecked = { index, isChecked ->
                val subject = currentSubject ?: return@ChapterAdapter
                val newCompleted = if (isChecked) {
                    (subject.completedChapters + 1).coerceAtMost(subject.totalChapters)
                } else {
                    (subject.completedChapters - 1).coerceAtLeast(0)
                }
                viewModel.updateProgress(subject.id, newCompleted)
            },

            // Long press — mark chapter as revised
            onMarkRevised = { index ->
                val subject = currentSubject ?: return@ChapterAdapter
                val chapterName = subject.chapterNames.getOrNull(index)
                    ?: "Chapter ${index + 1}"

                AlertDialog.Builder(requireContext())
                    .setTitle("Mark as Revised")
                    .setMessage("Mark \"$chapterName\" as revised?")
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
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun observeViewModel() {
        viewModel.subjects.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                val subject = resource.data.find { it.id == subjectId }
                    ?: return@observe

                currentSubject = subject

                // Header
                binding.tvDetailEmoji.text        = subject.emoji
                binding.tvDetailSubjectName.text  = subject.name
                binding.tvDetailProgress.text     =
                    "${subject.completedChapters} / ${subject.totalChapters} chapters done"
                binding.progressDetailBar.progress = subject.progressPercent
                binding.tvDetailPercent.text       = subject.progressLabel
                binding.tvRevisionPercent.text     =
                    "Revised: ${subject.revisionPercent}%"

                // Build chapter list
                val chapters = (0 until maxOf(
                    subject.totalChapters,
                    subject.chapterNames.size
                )).map { index ->
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
    }
}