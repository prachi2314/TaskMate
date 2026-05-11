package com.example.taskmate.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmate.R
import com.example.taskmate.data.model.ScheduleSlot
import com.example.taskmate.data.model.SlotType
import com.example.taskmate.databinding.ItemScheduleSlotBinding

/**
 * ScheduleAdapter.kt
 * Location: ui/adapters/ScheduleAdapter.kt
 *
 * Displays the timetable schedule in TimetableFragment → rv_schedule.
 *
 * Each row shows:
 *  - Time label (left side)
 *  - Color-accented subject card (right side)
 *
 * The left accent bar color and card background are set
 * dynamically based on the subject's colorHex field.
 *
 * Slot types are styled differently:
 *   CLASS      → subject color background
 *   SELF_STUDY → gray tones
 *   BREAK      → very light gray
 *   EXAM       → red tint
 */
class ScheduleAdapter(
    private val onSlotClick: (ScheduleSlot) -> Unit = {},
    private val onSlotLongClick: (ScheduleSlot) -> Unit = {}
) : ListAdapter<ScheduleSlot, ScheduleAdapter.SlotViewHolder>(SlotDiffCallback()) {

    // ══════════════════════════════════════════════════════════════
    //  ADAPTER OVERRIDES
    // ══════════════════════════════════════════════════════════════

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val binding = ItemScheduleSlotBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SlotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ══════════════════════════════════════════════════════════════
    //  VIEW HOLDER
    // ══════════════════════════════════════════════════════════════

    inner class SlotViewHolder(
        private val binding: ItemScheduleSlotBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(slot: ScheduleSlot) {
            val context = binding.root.context

            // ── Time label ─────────────────────────────────────────
            binding.tvSlotTime.text = slot.startTimeLabel

            // ── Subject name ───────────────────────────────────────
            binding.tvSlotSubject.text = when {
                slot.subjectName.isNotBlank() -> slot.subjectName
                slot.type == SlotType.SELF_STUDY -> "Free period"
                slot.type == SlotType.BREAK      -> "Break"
                slot.type == SlotType.EXAM       -> "Exam"
                else                             -> "Class"
            }

            // ── Detail line (room + duration) ──────────────────────
            binding.tvSlotDetail.text = slot.detailLabel

            // ── Apply colors based on slot type ────────────────────
            when (slot.type) {
                SlotType.CLASS      -> applySubjectColor(slot.subjectColorHex)
                SlotType.SELF_STUDY -> applyGrayStyle()
                SlotType.BREAK      -> applyLightGrayStyle()
                SlotType.EXAM       -> applyExamStyle()
            }

            // ── Text colors ────────────────────────────────────────
            when (slot.type) {
                SlotType.CLASS -> {
                    try {
                        val color     = Color.parseColor(slot.subjectColorHex)
                        val darkColor = darkenColor(color, 0.55f)
                        binding.tvSlotSubject.setTextColor(darkColor)
                        binding.tvSlotDetail.setTextColor(color)
                    } catch (e: IllegalArgumentException) {
                        binding.tvSlotSubject.setTextColor(
                            ContextCompat.getColor(context, R.color.purple_900)
                        )
                        binding.tvSlotDetail.setTextColor(
                            ContextCompat.getColor(context, R.color.purple_400)
                        )
                    }
                }
                SlotType.SELF_STUDY, SlotType.BREAK -> {
                    binding.tvSlotSubject.setTextColor(
                        ContextCompat.getColor(context, R.color.gray_600)
                    )
                    binding.tvSlotDetail.setTextColor(
                        ContextCompat.getColor(context, R.color.gray_400)
                    )
                }
                SlotType.EXAM -> {
                    binding.tvSlotSubject.setTextColor(
                        ContextCompat.getColor(context, R.color.red_800)
                    )
                    binding.tvSlotDetail.setTextColor(
                        ContextCompat.getColor(context, R.color.red_400)
                    )
                }
            }

            // ── Click listeners ────────────────────────────────────
            binding.root.setOnClickListener {
                onSlotClick(slot)
            }
            binding.cardSlot.setOnLongClickListener {
                onSlotLongClick(slot)
                true
            }
        }

        // ── Color helper functions ─────────────────────────────────

        /**
         * Applies the subject color to the card background and
         * accent bar. Makes the card background very light (15% opacity)
         * and the accent bar the full subject color.
         */
        private fun applySubjectColor(colorHex: String) {
            try {
                val color = Color.parseColor(colorHex)
                val lightColor = Color.argb(
                    38,  // 15% opacity
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color)
                )
                binding.cardSlot.setCardBackgroundColor(lightColor)
                binding.viewSlotAccent.setBackgroundColor(color)
            } catch (e: IllegalArgumentException) {
                applyGrayStyle()
            }
        }

        private fun applyGrayStyle() {
            binding.cardSlot.setCardBackgroundColor(Color.parseColor("#F3F0F8"))
            binding.viewSlotAccent.setBackgroundColor(Color.parseColor("#C4B8E8"))
        }

        private fun applyLightGrayStyle() {
            binding.cardSlot.setCardBackgroundColor(Color.parseColor("#F8F7F4"))
            binding.viewSlotAccent.setBackgroundColor(Color.parseColor("#D3D1C7"))
        }

        private fun applyExamStyle() {
            binding.cardSlot.setCardBackgroundColor(Color.parseColor("#FCEBEB"))
            binding.viewSlotAccent.setBackgroundColor(Color.parseColor("#E24B4A"))
        }

        /**
         * Creates a darker version of a color.
         * factor: 0.0 = black, 1.0 = original color
         * Used to make subject name text darker than the subject color.
         */
        private fun darkenColor(color: Int, factor: Float): Int {
            val r = (Color.red(color)   * factor).toInt().coerceIn(0, 255)
            val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
            val b = (Color.blue(color)  * factor).toInt().coerceIn(0, 255)
            return Color.rgb(r, g, b)
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DIFF CALLBACK
    // ══════════════════════════════════════════════════════════════

    class SlotDiffCallback : DiffUtil.ItemCallback<ScheduleSlot>() {

        // Two slots are the same item if they have the same Firestore ID
        override fun areItemsTheSame(
            oldItem: ScheduleSlot,
            newItem: ScheduleSlot
        ): Boolean = oldItem.id == newItem.id

        // Two slots have the same content if all fields match
        // data class == compares every property automatically
        override fun areContentsTheSame(
            oldItem: ScheduleSlot,
            newItem: ScheduleSlot
        ): Boolean = oldItem == newItem
    }
}