package com.example.taskmate.ui.timetable

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.taskmate.R
import com.example.taskmate.data.model.ScheduleSlot
import com.example.taskmate.data.model.SlotType
import com.example.taskmate.data.model.Subject
import com.example.taskmate.databinding.BottomSheetAddSlotBinding
import com.example.taskmate.utils.Resource
import com.example.taskmate.utils.TaskNotificationScheduler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddSlotBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "AddSlotBottomSheet"
    }

    private var _binding: BottomSheetAddSlotBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimetableViewModel by viewModels()

    // ── State ──────────────────────────────────────────────────────
    private var selectedDayIndex  = 0
    private var selectedSubject: Subject? = null
    private var selectedSlotType  = SlotType.CLASS
    private var startHour         = 8
    private var startMinute       = 0
    private var isAm              = true
    private var subjectList: List<Subject> = emptyList()

    // Store the slot being saved so we can schedule notification after save
    private var pendingSlot: ScheduleSlot? = null

    private val dayNames = listOf(
        "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddSlotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDayChips()
        setupTimePicker()
        setupSlotTypeChips()
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

    private fun setupDayChips() {
        binding.llDayChips.removeAllViews()

        dayNames.forEachIndexed { index, name ->
            val chip = android.widget.TextView(requireContext()).apply {
                text      = name
                textSize  = 12f
                gravity   = android.view.Gravity.CENTER
                setPadding(24, 10, 24, 10)
                isClickable = true
                isFocusable = true
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8 }

                if (index == 0) applyActiveStyle(this)
                else applyInactiveStyle(this)

                setOnClickListener {
                    for (i in 0 until binding.llDayChips.childCount) {
                        val child = binding.llDayChips.getChildAt(i)
                                as? android.widget.TextView ?: continue
                        applyInactiveStyle(child)
                    }
                    applyActiveStyle(this)
                    selectedDayIndex = index
                }
            }
            binding.llDayChips.addView(chip)
        }
    }

    private fun setupTimePicker() {
        // Hour picker — 1 to 12
        binding.npHour.apply {
            minValue = 1
            maxValue = 12
            value    = if (startHour == 0) 12
            else if (startHour > 12) startHour - 12
            else startHour
            setOnValueChangedListener { _, _, newVal ->
                updateStartHourFrom12Hr(newVal, isAm)
                updateTimePreview()
            }
        }

        // Minute picker — 00, 15, 30, 45
        binding.npMinute.apply {
            minValue        = 0
            maxValue        = 3
            displayedValues = arrayOf("00", "15", "30", "45")
            value           = 0
            setOnValueChangedListener { _, _, newVal ->
                startMinute = newVal * 15
                updateTimePreview()
            }
        }

        // AM / PM picker
        binding.npAmPm.apply {
            minValue        = 0
            maxValue        = 1
            displayedValues = arrayOf("AM", "PM")
            value           = 0 // default AM
            setOnValueChangedListener { _, _, newVal ->
                isAm = newVal == 0
                updateStartHourFrom12Hr(binding.npHour.value, isAm)
                updateTimePreview()
            }
        }

        updateTimePreview()
    }

    private fun setupSlotTypeChips() {
        binding.chipGroupSlotType.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedSlotType = when {
                checkedIds.contains(R.id.chip_type_self_study) -> SlotType.SELF_STUDY
                checkedIds.contains(R.id.chip_type_break)      -> SlotType.BREAK
                checkedIds.contains(R.id.chip_type_exam)       -> SlotType.EXAM
                else                                           -> SlotType.CLASS
            }
            // Show or hide subject field based on slot type
            val showSubject = selectedSlotType == SlotType.CLASS ||
                    selectedSlotType == SlotType.EXAM
            binding.tilSlotSubject.visibility =
                if (showSubject) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnCloseSlotSheet.setOnClickListener { dismiss() }
        binding.btnSaveSlot.setOnClickListener       { saveSlot() }
    }

    // ══════════════════════════════════════════════════════════════
    //  OBSERVE
    // ══════════════════════════════════════════════════════════════

    private fun observeViewModel() {

        // Load subjects for dropdown
        viewModel.subjectsForDropdown.observe(viewLifecycleOwner) { subjects ->
            subjectList      = subjects
            val names        = subjects.map { it.name }
            val adapter      = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                names
            )
            binding.acvSlotSubject.setAdapter(adapter)
            binding.acvSlotSubject.setOnItemClickListener { _, _, position, _ ->
                selectedSubject = subjects.getOrNull(position)
            }
        }

        // Observe add slot result
        // addSlotState is Resource<Unit> — no slot data inside
        viewModel.addSlotState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnSaveSlot.isEnabled = false
                    binding.btnSaveSlot.text      = "Saving…"
                }
                is Resource.Success -> {
                    binding.btnSaveSlot.isEnabled = true
                    binding.btnSaveSlot.text      = "Add slot"

                    // Schedule notification using pendingSlot
                    // pendingSlot was saved before calling viewModel.addSlot()
                    pendingSlot?.let { slot ->
                        TaskNotificationScheduler
                            .scheduleSlotReminder(requireContext(), slot)
                    }
                    pendingSlot = null

                    Toast.makeText(
                        requireContext(), "Slot added!", Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                }
                is Resource.Error -> {
                    binding.btnSaveSlot.isEnabled = true
                    binding.btnSaveSlot.text      = "Add slot"
                    Toast.makeText(
                        requireContext(),
                        resource.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SAVE SLOT
    // ══════════════════════════════════════════════════════════════

    private fun saveSlot() {
        val durationStr = binding.etSlotDuration.text.toString().trim()
        val location    = binding.etSlotLocation.text.toString().trim()

        // Validate subject for CLASS and EXAM types
        if ((selectedSlotType == SlotType.CLASS ||
                    selectedSlotType == SlotType.EXAM) &&
            selectedSubject == null &&
            binding.acvSlotSubject.text.isNullOrBlank()) {
            binding.tilSlotSubject.error = "Please select a subject"
            return
        }
        binding.tilSlotSubject.error = null

        // Validate duration
        if (durationStr.isBlank()) {
            binding.tilSlotDuration.error = "Duration is required"
            return
        }
        val duration = durationStr.toIntOrNull()
        if (duration == null || duration <= 0) {
            binding.tilSlotDuration.error = "Enter a valid duration in minutes"
            return
        }
        if (duration > 480) {
            binding.tilSlotDuration.error = "Max duration is 480 minutes"
            return
        }
        binding.tilSlotDuration.error = null

        val startTimeMinutes = (startHour * 60) + startMinute

        val subjectNameToUse = when (selectedSlotType) {
            SlotType.CLASS, SlotType.EXAM ->
                selectedSubject?.name
                    ?: binding.acvSlotSubject.text.toString().trim()
            SlotType.SELF_STUDY -> "Free period"
            SlotType.BREAK      -> "Break"
        }

        // Build the slot
        val slot = ScheduleSlot(
            dayOfWeek        = selectedDayIndex,
            startTimeMinutes = startTimeMinutes,
            durationMinutes  = duration,
            subjectId        = selectedSubject?.id ?: "",
            subjectName      = subjectNameToUse,
            subjectColorHex  = selectedSubject?.colorHex ?: "#C4B8E8",
            location         = location,
            type             = selectedSlotType,
            createdAt        = System.currentTimeMillis()
        )

        // Save slot reference BEFORE calling viewModel
        // so it is available when addSlotState emits Success
        pendingSlot = slot

        viewModel.addSlot(slot)
    }

    // ══════════════════════════════════════════════════════════════
    //  TIME HELPERS
    // ══════════════════════════════════════════════════════════════

    private fun updateStartHourFrom12Hr(hour12: Int, am: Boolean) {
        startHour = when {
            am && hour12 == 12  -> 0
            am                  -> hour12
            !am && hour12 == 12 -> 12
            else                -> hour12 + 12
        }
    }

    private fun updateTimePreview() {
        val amPmStr   = if (isAm) "AM" else "PM"
        val display12 = when {
            startHour == 0  -> 12
            startHour <= 12 -> startHour
            else            -> startHour - 12
        }
        val minStr = startMinute.toString().padStart(2, '0')
        binding.tvTimePreview.text = "$display12:$minStr $amPmStr"
    }

    // ══════════════════════════════════════════════════════════════
    //  CHIP STYLE HELPERS
    // ══════════════════════════════════════════════════════════════

    private fun applyActiveStyle(chip: android.widget.TextView) {
        chip.setBackgroundResource(R.drawable.bg_day_chip_active)
        chip.setTextColor(
            androidx.core.content.ContextCompat.getColor(
                requireContext(), R.color.white
            )
        )
    }

    private fun applyInactiveStyle(chip: android.widget.TextView) {
        chip.setBackgroundResource(R.drawable.bg_day_chip_inactive)
        chip.setTextColor(
            androidx.core.content.ContextCompat.getColor(
                requireContext(), R.color.blue_400
            )
        )
    }
}