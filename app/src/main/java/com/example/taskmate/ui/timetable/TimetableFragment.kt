package com.example.taskmate.ui.timetable

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskmate.R
import com.example.taskmate.databinding.FragmentTimetableBinding
import com.example.taskmate.ui.adapters.ScheduleAdapter
import com.example.taskmate.utils.DateUtils
import com.example.taskmate.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TimetableFragment : Fragment() {

    private var _binding: FragmentTimetableBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimetableViewModel by viewModels()
    private lateinit var scheduleAdapter: ScheduleAdapter

    // Currently selected day index (0=Mon … 6=Sun)
    private var selectedDayIndex = DateUtils.currentDayIndex()

    private val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimetableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWeekLabel()
        setupDayChips()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        // Load slots for today when fragment first opens
        viewModel.loadSlotsForDay(selectedDayIndex)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ══════════════════════════════════════════════════════════════
    //  SETUP
    // ══════════════════════════════════════════════════════════════

    private fun setupWeekLabel() {
        binding.tvTimetableWeek.text = DateUtils.currentWeekLabel()
    }

    private fun setupDayChips() {
        binding.llDayChips.removeAllViews()

        dayNames.forEachIndexed { index, name ->
            val chip = TextView(requireContext()).apply {
                text      = name
                textSize  = 11f
                gravity   = android.view.Gravity.CENTER
                setPadding(20, 10, 20, 10)
                isClickable = true
                isFocusable = true
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8 }

                // Highlight the current day by default
                if (index == selectedDayIndex) setActiveChip(this)
                else setInactiveChip(this)

                setOnClickListener {
                    // Reset all chips first
                    for (i in 0 until binding.llDayChips.childCount) {
                        val child = binding.llDayChips.getChildAt(i)
                                as? TextView ?: continue
                        setInactiveChip(child)
                    }
                    // Activate tapped chip
                    setActiveChip(this)
                    selectedDayIndex = index
                    // Load schedule for selected day
                    viewModel.loadSlotsForDay(index)
                }
            }
            binding.llDayChips.addView(chip)
        }
    }

    private fun setupRecyclerView() {
        scheduleAdapter = ScheduleAdapter(
            onSlotClick = { slot ->
                Toast.makeText(
                    requireContext(),
                    "${slot.subjectName} — ${slot.startTimeLabel}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onSlotLongClick = { slot ->
                showDeleteConfirmation(slot)
            }
        )
        binding.rvSchedule.apply {
            adapter       = scheduleAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.fabAddSlot.setOnClickListener {
            AddSlotBottomSheet().show(childFragmentManager, "AddSlot")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  OBSERVE
    // ══════════════════════════════════════════════════════════════

    private fun observeViewModel() {
        viewModel.scheduleSlots.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // optional show loading
                }
                is Resource.Success -> {
                    scheduleAdapter.submitList(resource.data)

                    // Show empty state if no slots for this day
                    if (resource.data.isEmpty()) {
                        binding.tvNoSlots?.visibility = View.VISIBLE
                    } else {
                        binding.tvNoSlots?.visibility = View.GONE
                    }
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

        // Observe add slot result — reload list after adding
        viewModel.addSlotState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    // Reload slots for currently selected day
                    // so the new slot appears immediately
                    viewModel.loadSlotsForDay(selectedDayIndex)
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        resource.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {}
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════

    private fun setActiveChip(chip: TextView) {
        chip.setBackgroundResource(R.drawable.bg_day_chip_active)
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    private fun setInactiveChip(chip: TextView) {
        chip.setBackgroundResource(R.drawable.bg_day_chip_inactive)
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_400))
    }

    private fun showDeleteConfirmation(slot: com.example.taskmate.data.model.ScheduleSlot) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete slot")
            .setMessage("Remove ${slot.subjectName} from ${slot.startTimeLabel}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSlot(slot.id)
                // Reload after delete
                binding.root.postDelayed({
                    viewModel.loadSlotsForDay(selectedDayIndex)
                }, 500)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}