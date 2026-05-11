package com.example.taskmate.ui.focus

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskmate.R
import com.example.taskmate.databinding.FragmentFocusBinding
import com.example.taskmate.ui.adapters.FocusTaskAdapter
import com.example.taskmate.utils.FocusModeManager
import com.example.taskmate.utils.NotificationHelper
import com.example.taskmate.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FocusFragment : Fragment() {

    private var _binding: FragmentFocusBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FocusViewModel by viewModels()
    private lateinit var focusTaskAdapter: FocusTaskAdapter

    // ── Timer ──────────────────────────────────────────────────────
    private var countDownTimer: CountDownTimer? = null
    private var isRunning  = false
    private var isPaused   = false

    private val FOCUS_DURATION_MS = 25 * 60 * 1000L
    private val SHORT_BREAK_MS    =  5 * 60 * 1000L
    private val LONG_BREAK_MS     = 15 * 60 * 1000L

    private var totalDurationMs = FOCUS_DURATION_MS
    private var remainingMs     = FOCUS_DURATION_MS
    private var isFocusMode     = true

    private var completedSessions = 0
    private val MAX_SESSIONS      = 4

    // ── WakeLock — keeps screen on during focus ────────────────────
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFocusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        setupChipGroup()
        observeViewModel()

        // Show initial state — do NOT start timer automatically
        updateTimerDisplay(remainingMs)
        updateRing(remainingMs)
        updatePlayPauseIcon(false)
        binding.tvTimerLabel.text = "Ready to focus"
        updateSessionDots()
    }

    override fun onPause() {
        super.onPause()
        if (isRunning) pauseTimer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        releaseWakeLock()
        _binding = null
    }

    // ══════════════════════════════════════════════════════════════
    //  SETUP
    // ══════════════════════════════════════════════════════════════

    private fun setupRecyclerView() {
        focusTaskAdapter = FocusTaskAdapter(
            onCheckClick = { task ->
                viewModel.toggleTask(task.id, !task.completed)
            }
        )
        binding.rvFocusTasks.apply {
            adapter = focusTaskAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.btnPlayPause.setOnClickListener {
            when {
                isRunning -> pauseTimer()
                isPaused  -> resumeTimer()
                else      -> startTimer()
            }
        }
        binding.btnReset.setOnClickListener { resetTimer() }
        binding.btnSkip.setOnClickListener  { skipSession() }
    }

    /**
     * Each chip has its own click listener.
     * This is more reliable than setOnCheckedStateChangeListener
     * which sometimes fires with empty checkedIds.
     */
    private fun setupChipGroup() {
        binding.chipFocusMode.setOnClickListener {
            switchMode(FOCUS_DURATION_MS, true, "Ready to focus")
        }
        binding.chipShortBreak.setOnClickListener {
            switchMode(SHORT_BREAK_MS, false, "Short break")
        }
        binding.chipLongBreak.setOnClickListener {
            switchMode(LONG_BREAK_MS, false, "Long break")
        }
    }

    private fun switchMode(
        durationMs: Long,
        isFocus: Boolean,
        label: String
    ) {
        countDownTimer?.cancel()
        isRunning       = false
        isPaused        = false
        totalDurationMs = durationMs
        isFocusMode     = isFocus
        remainingMs     = durationMs
        releaseWakeLock()

        updateTimerDisplay(remainingMs)
        updateRing(remainingMs)
        updatePlayPauseIcon(false)
        binding.tvTimerLabel.text = label
    }

    // ══════════════════════════════════════════════════════════════
    //  TIMER LOGIC
    // ══════════════════════════════════════════════════════════════

    private fun startTimer() {
        isRunning = true
        isPaused  = false
        updatePlayPauseIcon(true)
        acquireWakeLock()

        // Enable DND during focus
        if (isFocusMode) {
            if (FocusModeManager.hasDndPermission(requireContext())) {
                FocusModeManager.enableDnd(requireContext())
                binding.tvTimerLabel.text = "Focus session — DND on"
            } else {
                binding.tvTimerLabel.text = "Focus session"
            }
        }

        countDownTimer = object : CountDownTimer(remainingMs, 1000) {
            override fun onTick(ms: Long) {
                remainingMs = ms
                updateTimerDisplay(ms)
                updateRing(ms)
            }
            override fun onFinish() {
                isRunning   = false
                isPaused    = false
                remainingMs = 0
                updateTimerDisplay(0)
                updateRing(0)
                updatePlayPauseIcon(false)
                releaseWakeLock()
                // Disable DND when session ends
                FocusModeManager.disableDnd(requireContext())
                onSessionFinished()
            }
        }.start()
    }


    private fun pauseTimer() {
        countDownTimer?.cancel()
        isRunning = false
        isPaused  = true
        updatePlayPauseIcon(false)
        releaseWakeLock()
        // Restore notifications when paused
        FocusModeManager.disableDnd(requireContext())
        binding.tvTimerLabel.text = "Paused"
    }


    private fun resumeTimer() {
        isPaused = false
        startTimer()
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        isRunning   = false
        isPaused    = false
        remainingMs = totalDurationMs
        releaseWakeLock()
        FocusModeManager.disableDnd(requireContext())
        updateTimerDisplay(remainingMs)
        updateRing(remainingMs)
        updatePlayPauseIcon(false)
        binding.tvTimerLabel.text = when {
            isFocusMode -> "Ready to focus"
            totalDurationMs == SHORT_BREAK_MS -> "Short break"
            else -> "Long break"
        }
    }

    private fun setupDndButton() {
        if (!FocusModeManager.hasDndPermission(requireContext())) {
            binding.tvDndStatus.text = "Tap to enable Focus Mode (DND)"
            binding.tvDndStatus.setOnClickListener {
                FocusModeManager.requestDndPermission(requireContext())
            }
        } else {
            binding.tvDndStatus.text = "Focus Mode ready ✓"
        }
    }

    private fun skipSession() {
        countDownTimer?.cancel()
        isRunning = false
        isPaused  = false
        releaseWakeLock()

        if (isFocusMode) {
            completedSessions++
            updateSessionDots()
            viewModel.recordSessionComplete(25)
            if (completedSessions % MAX_SESSIONS == 0) switchToLongBreak()
            else switchToShortBreak()
        } else {
            switchToFocus()
        }
    }

    private fun onSessionFinished() {
        if (isFocusMode) {
            completedSessions++
            updateSessionDots()
            viewModel.recordSessionComplete(25)
            binding.tvTimerLabel.text = "Session complete!"

            // Show notification
            NotificationHelper.showFocusCompleteNotification(
                requireContext(),
                "Session $completedSessions complete! Take a break."
            )

            binding.root.postDelayed({
                if (_binding != null) {
                    if (completedSessions % MAX_SESSIONS == 0) switchToLongBreak()
                    else switchToShortBreak()
                }
            }, 1500)
        } else {
            binding.tvTimerLabel.text = "Break over! Ready?"
            NotificationHelper.showBreakCompleteNotification(requireContext())
            binding.root.postDelayed({
                if (_binding != null) switchToFocus()
            }, 1500)
        }
    }

    // ── Mode switchers ─────────────────────────────────────────────

    private fun switchToFocus() {
        binding.chipFocusMode.isChecked = true
        switchMode(FOCUS_DURATION_MS, true, "Ready to focus")
    }

    private fun switchToShortBreak() {
        binding.chipShortBreak.isChecked = true
        switchMode(SHORT_BREAK_MS, false, "Short break — well done!")
    }

    private fun switchToLongBreak() {
        binding.chipLongBreak.isChecked = true
        switchMode(LONG_BREAK_MS, false, "Long break — great work!")
    }

    // ══════════════════════════════════════════════════════════════
    //  WAKELOCK
    // ══════════════════════════════════════════════════════════════

    private fun acquireWakeLock() {
        try {
            val powerManager = requireContext()
                .getSystemService(android.content.Context.POWER_SERVICE)
                    as android.os.PowerManager
            wakeLock = powerManager.newWakeLock(
                android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "TaskMate:FocusWakeLock"
            )
            wakeLock?.acquire(60 * 60 * 1000L) // 1 hour max
        } catch (e: Exception) {
            android.util.Log.e("FocusFragment", "WakeLock failed: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            android.util.Log.e("FocusFragment", "WakeLock release failed: ${e.message}")
        } finally {
            wakeLock = null
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  UI UPDATES
    // ══════════════════════════════════════════════════════════════

    private fun updateTimerDisplay(ms: Long) {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        binding.tvTimerDisplay.text =
            String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateRing(remainingMs: Long) {
        val progress = if (totalDurationMs > 0) {
            (remainingMs.toFloat() / totalDurationMs.toFloat()) * 100f
        } else 0f
        binding.cpbPomodoro.setProgressWithAnimation(progress, 500)
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        binding.btnPlayPause.setIconResource(
            if (isPlaying) R.drawable.ic_pause_24
            else           R.drawable.ic_play_arrow_24
        )
    }

    private fun updateSessionDots() {
        val dots = listOf(
            binding.dotSession1,
            binding.dotSession2,
            binding.dotSession3,
            binding.dotSession4
        )
        val completedInCycle = completedSessions % MAX_SESSIONS
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index < completedInCycle) R.drawable.bg_session_dot_active
                else R.drawable.bg_session_dot_inactive
            )
        }
        val sessionNum = completedInCycle + 1
        binding.tvSessionCounter.text = "Session $sessionNum of $MAX_SESSIONS"
    }

    // ══════════════════════════════════════════════════════════════
    //  OBSERVE
    // ══════════════════════════════════════════════════════════════

    private fun observeViewModel() {
        viewModel.tasks.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> { }
                is Resource.Success -> {
                    focusTaskAdapter.submitList(resource.data)
                    binding.tvFocusTaskTitle.text =
                        if (resource.data.isEmpty()) "No tasks for today"
                        else "Current tasks"
                }
                is Resource.Error -> { }
            }
        }

        viewModel.currentSubject.observe(viewLifecycleOwner) { subject ->
            binding.tvCurrentSubjectChip.text =
                subject?.name ?: "No subject selected"
        }
    }
}