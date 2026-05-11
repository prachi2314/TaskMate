package com.example.taskmate.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskmate.R
import com.example.taskmate.databinding.FragmentDashboardBinding
import com.example.taskmate.ui.adapters.ExamAdapter
import com.example.taskmate.ui.adapters.SubjectProgressAdapter
import com.example.taskmate.ui.adapters.TaskAdapter
import com.example.taskmate.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

/**
 * DashboardFragment
 * Location: ui/dashboard/DashboardFragment.kt
 *
 * Responsibilities (UI only — no Firebase here):
 *  1. Observe tasks LiveData → update RecyclerView
 *  2. Observe subjects LiveData → update progress bars
 *  3. Show greeting based on time of day
 *  4. Show/hide loading and empty states
 *  5. Open AddTaskBottomSheet on FAB click
 */
@AndroidEntryPoint
class DashboardFragment : Fragment() {

    // ── ViewBinding ────────────────────────────────────────────────
    // _binding is nullable — set in onCreateView, cleared in onDestroyView
    // binding (non-null) is only used between those two lifecycle events
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // ── ViewModel ──────────────────────────────────────────────────
    // Hilt injects DashboardViewModel automatically
    private val viewModel: DashboardViewModel by viewModels()

    // ── Adapters ───────────────────────────────────────────────────
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var subjectAdapter: SubjectProgressAdapter
    private lateinit var examAdapter: ExamAdapter

    // ══════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ══════════════════════════════════════════════════════════════

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGreeting()
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
        loadUserAvatar()
    }

    // Prevent memory leaks — Fragment view can be destroyed while
    // the Fragment itself stays alive (e.g. back stack)
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ══════════════════════════════════════════════════════════════
    //  SETUP
    // ══════════════════════════════════════════════════════════════

    /**
     * Shows "Good morning", "Good afternoon", or "Good evening"
     * based on the current hour.
     */
    private fun setupGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hour < 12 -> "Good morning,"
            hour < 17 -> "Good afternoon,"
            else      -> "Good evening,"
        }

        // Load student name from ViewModel
        viewModel.currentUserName.observe(viewLifecycleOwner) { name ->
            binding.tvStudentName.text = name
        }
    }

    private fun setupRecyclerViews() {

        // ── Task RecyclerView ──────────────────────────────────────
        taskAdapter = TaskAdapter(
            onCheckClick = { task ->
                // Toggle done/undone — ViewModel calls Firestore
                viewModel.toggleTask(task.id, !task.completed)
            },
            onDeleteClick = { task ->
                viewModel.deleteTask(task.id)
            }
        )
        binding.rvTasks.apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // Disable nested scrolling so NestedScrollView controls scroll
            isNestedScrollingEnabled = false
        }

        // ── Subject RecyclerView ───────────────────────────────────
        subjectAdapter = SubjectProgressAdapter()
        binding.rvSubjects.apply {
            adapter = subjectAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }

        // ── Exams RecyclerView ─────────────────────────────────────
        examAdapter = ExamAdapter()
        binding.rvExams.apply {
            adapter = examAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {

        // FAB — add task
        binding.fabAddTask.setOnClickListener {
            AddTaskBottomSheet().show(childFragmentManager, AddTaskBottomSheet.TAG)
        }

        // Add exam button inside exams card
        binding.btnAddExam.setOnClickListener {
            AddExamBottomSheet().show(childFragmentManager, AddExamBottomSheet.TAG)
        }

        // Profile avatar
        binding.ivAvatar.setOnClickListener {
            showProfileMenu()
        }
    }

    private fun showProfileMenu() {
        val options = arrayOf("Sign Out", "Cancel")

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Profile")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> signOut()
                    1 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun signOut() {
        viewModel.signOut()
        startActivity(
            android.content.Intent(
                requireContext(),
                com.example.taskmate.ui.auth.LoginActivity::class.java
            ).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }


    // ══════════════════════════════════════════════════════════════
    //  OBSERVE VIEWMODEL
    // ══════════════════════════════════════════════════════════════

    private fun observeViewModel() {
        observeTasks()
        observeSubjects()
        observeExams()
        observeStats()
    }

    private fun observeTasks() {
        viewModel.tasks.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressTasksLoading.visibility = View.VISIBLE
                    binding.tvNoTasks.visibility = View.GONE
                }

                is Resource.Success -> {
                    binding.progressTasksLoading.visibility = View.GONE
                    taskAdapter.submitList(resource.data)

                    // Show empty state when list is empty
                    binding.tvNoTasks.visibility =
                        if (resource.data.isEmpty()) View.VISIBLE else View.GONE

                    // Update task count label e.g. "2/4 done"
                    val done = resource.data.count { it.completed }
                    binding.tvTasksCount.text = "$done/${resource.data.size} done"
                }

                is Resource.Error -> {
                    binding.progressTasksLoading.visibility = View.GONE
                    showError(resource.message)
                }
            }
        }
    }

    private fun observeSubjects() {
        viewModel.subjects.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> { /* optional skeleton loader */ }

                is Resource.Success -> {
                    subjectAdapter.submitList(resource.data)
                }

                is Resource.Error -> showError(resource.message)
            }
        }
    }

    private fun observeExams() {
        viewModel.upcomingExams.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> { }
                is Resource.Success -> {
                    examAdapter.submitList(resource.data)
                    binding.tvNoExams.visibility =
                        if (resource.data.isEmpty()) View.VISIBLE else View.GONE
                }
                is Resource.Error -> { }
            }
        }
    }

    private fun observeStats() {
        viewModel.studyStats.observe(viewLifecycleOwner) { stats ->
            binding.tvTodayHours.text = stats.todayHours
            binding.tvStreak.text = stats.streakDays.toString()
            binding.tvOverallPercent.text = "${stats.overallPercent}%"
        }
    }
         private fun loadUserAvatar() {
            val user     = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val photoUrl = user?.photoUrl

            if (photoUrl != null) {
                // User has a Google profile photo — load it into the avatar
                com.bumptech.glide.Glide.with(this)
                    .load(photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person_24)
                    .error(R.drawable.ic_person_24)
                    .into(binding.ivAvatar)
            } else {
                // No photo — show initials as text drawn on a colored circle
                val name     = user?.displayName ?: user?.email?.take(1) ?: "U"
                val initials = name.split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .uppercase()

                // Draw initials onto a bitmap and set as avatar image
                val bitmap = createInitialsBitmap(initials)
                binding.ivAvatar.setImageBitmap(bitmap)
            }
        }

        /**
         * Creates a circular bitmap with initials text.
         * No extra TextView needed — drawn directly onto canvas.
         */
         private fun createInitialsBitmap(initials: String): android.graphics.Bitmap {
            val size   = resources.getDimensionPixelSize(R.dimen.avatar_size)
            val bitmap = android.graphics.Bitmap.createBitmap(
                size, size,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)

            // Background circle
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = androidx.core.content.ContextCompat.getColor(
                    requireContext(), R.color.purple_600
                )
            }
            val radius = size / 2f
            canvas.drawCircle(radius, radius, radius, paint)

            // Initials text
            val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color     = android.graphics.Color.WHITE
                textSize  = size * 0.38f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface  = android.graphics.Typeface.DEFAULT_BOLD
            }
            val textY = radius - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(initials, radius, textY, textPaint)

            return bitmap
         }


    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}