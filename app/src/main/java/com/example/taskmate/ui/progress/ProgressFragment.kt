package com.example.taskmate.ui.progress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskmate.databinding.FragmentProgressBinding
import com.example.taskmate.ui.adapters.MasteryAdapter
import com.example.taskmate.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProgressViewModel by viewModels()
    private lateinit var masteryAdapter: MasteryAdapter

    private val quotes = listOf(
        Pair(
            "Success is the sum of small efforts repeated day in and day out.",
            "Robert Collier"
        ),
        Pair(
            "The secret of getting ahead is getting started.",
            "Mark Twain"
        ),
        Pair(
            "Don't watch the clock. Do what it does — keep going.",
            "Sam Levenson"
        ),
        Pair(
            "Believe you can and you're halfway there.",
            "Theodore Roosevelt"
        ),
        Pair(
            "Study hard, for the well is deep and our brains are shallow.",
            "Richard Baxter"
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupQuote()
        setupRecyclerView()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ══════════════════════════════════════════════════════════════
    //  SETUP
    // ══════════════════════════════════════════════════════════════

    private fun setupQuote() {
        val dayOfYear = java.util.Calendar.getInstance()
            .get(java.util.Calendar.DAY_OF_YEAR)
        val quote = quotes[dayOfYear % quotes.size]
        binding.tvQuote.text = "\"${quote.first}\""
        binding.tvQuoteAuthor.text = "— ${quote.second}"
    }

    private fun setupRecyclerView() {
        masteryAdapter = MasteryAdapter()
        binding.rvMastery.apply {
            adapter = masteryAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  OBSERVE
    // ══════════════════════════════════════════════════════════════

    private fun observeViewModel() {
        observeSubjects()
        observeWeeklyGoals()
        observeDaysUntilExam()
    }

    private fun observeSubjects() {
        viewModel.subjects.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> { /* optional skeleton */ }
                is Resource.Success -> {
                    masteryAdapter.submitList(resource.data)
                }
                is Resource.Error -> { /* show error */ }
            }
        }
    }

    /**
     * Observes the weekly goals LiveData and updates all three
     * circular progress rings with REAL calculated values.
     */
    private fun observeWeeklyGoals() {
        viewModel.weeklyGoals.observe(viewLifecycleOwner) { goals ->

            // ── Study hours ring ───────────────────────────────────
            binding.cpbStudyHours.setProgressWithAnimation(
                goals.studyHoursPercent, 800
            )
            binding.tvStudyHoursValue.text =
                "${goals.studyHoursPercent.toInt()}%"

            // ── Tasks done ring ────────────────────────────────────
            binding.cpbTasksDone.setProgressWithAnimation(
                goals.tasksPercent, 800
            )
            binding.tvTasksValue.text =
                "${goals.tasksPercent.toInt()}%"

            // ── Revision ring ──────────────────────────────────────
            binding.cpbRevision.setProgressWithAnimation(
                goals.revisionPercent, 800
            )
            binding.tvRevisionValue.text =
                "${goals.revisionPercent.toInt()}%"
        }
    }

    private fun observeDaysUntilExam() {
        viewModel.daysUntilExam.observe(viewLifecycleOwner) { days ->
            val month = SimpleDateFormat(
                "MMMM yyyy",
                Locale.getDefault()
            ).format(Date())
            binding.tvProgressSubtitle.text =
                "$month · Exam in $days days"
        }
    }
}