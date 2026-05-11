package com.example.taskmate.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmate.R
import com.example.taskmate.data.model.Exam
import com.example.taskmate.data.model.UrgencyLevel
import com.example.taskmate.databinding.ItemExamBinding

/**
 * ExamAdapter.kt
 * Location: ui/adapters/ExamAdapter.kt
 *
 * Displays upcoming exams in DashboardFragment → rv_exams.
 * All binding IDs match item_exam.xml exactly.
 */
class ExamAdapter(
    private val onExamClick: (Exam) -> Unit = {},
    private val onDeleteClick: (Exam) -> Unit = {}
) : ListAdapter<Exam, ExamAdapter.ExamViewHolder>(ExamDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExamViewHolder {
        val binding = ItemExamBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExamViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ══════════════════════════════════════════════════════════════
    //  VIEW HOLDER
    // ══════════════════════════════════════════════════════════════

    inner class ExamViewHolder(
        private val binding: ItemExamBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exam: Exam) {
            val context = binding.root.context

            // ── Date badge ─────────────────────────────────────────
            // binding.cardExamDate  → card_exam_date  in XML
            // binding.tvExamDay     → tv_exam_day     in XML
            // binding.tvExamMonth   → tv_exam_month   in XML
            binding.tvExamDay.text   = exam.dayNumber
            binding.tvExamMonth.text = exam.monthAbbreviation

            // Apply subject color to the date badge background
            try {
                val baseColor = Color.parseColor(exam.subjectColorHex)
                val lightColor = Color.argb(
                    38,
                    Color.red(baseColor),
                    Color.green(baseColor),
                    Color.blue(baseColor)
                )
                binding.cardExamDate.setCardBackgroundColor(lightColor)
                binding.tvExamDay.setTextColor(darkenColor(baseColor, 0.6f))
                binding.tvExamMonth.setTextColor(baseColor)
            } catch (e: IllegalArgumentException) {
                binding.cardExamDate.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.blue_50)
                )
                binding.tvExamDay.setTextColor(
                    ContextCompat.getColor(context, R.color.blue_800)
                )
                binding.tvExamMonth.setTextColor(
                    ContextCompat.getColor(context, R.color.blue_400)
                )
            }

            // ── Exam name ──────────────────────────────────────────
            binding.tvExamName.text = exam.title

            // ── Days away ──────────────────────────────────────────
            // binding.tvExamDaysAway → tv_exam_days_away in XML
            binding.tvExamDaysAway.text = exam.daysAwayLabel

            // ── Urgency badge ──────────────────────────────────────
            // binding.tvExamUrgency → tv_exam_urgency in XML
            binding.tvExamUrgency.text = exam.urgencyLabel
            applyUrgencyStyle(exam.urgency, context)

            // ── Click listeners ────────────────────────────────────
            binding.root.setOnClickListener { onExamClick(exam) }
            binding.root.setOnLongClickListener {
                onDeleteClick(exam)
                true
            }
        }

        private fun applyUrgencyStyle(
            urgency: UrgencyLevel,
            context: android.content.Context
        ) {
            when (urgency) {
                UrgencyLevel.URGENT -> {
                    binding.tvExamUrgency.setTextColor(
                        ContextCompat.getColor(context, R.color.red_800)
                    )
                    binding.tvExamUrgency.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.red_50)
                        )
                    binding.tvExamDaysAway.setTextColor(
                        ContextCompat.getColor(context, R.color.red_400)
                    )
                }
                UrgencyLevel.SOON -> {
                    binding.tvExamUrgency.setTextColor(
                        ContextCompat.getColor(context, R.color.amber_800)
                    )
                    binding.tvExamUrgency.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.amber_50)
                        )
                    binding.tvExamDaysAway.setTextColor(
                        ContextCompat.getColor(context, R.color.purple_400)
                    )
                }
                UrgencyLevel.OK -> {
                    binding.tvExamUrgency.setTextColor(
                        ContextCompat.getColor(context, R.color.teal_800)
                    )
                    binding.tvExamUrgency.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.teal_50)
                        )
                    binding.tvExamDaysAway.setTextColor(
                        ContextCompat.getColor(context, R.color.purple_400)
                    )
                }
                UrgencyLevel.PAST -> {
                    binding.tvExamUrgency.setTextColor(
                        ContextCompat.getColor(context, R.color.gray_600)
                    )
                    binding.tvExamUrgency.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.gray_100)
                        )
                    binding.tvExamDaysAway.setTextColor(
                        ContextCompat.getColor(context, R.color.gray_400)
                    )
                }
                UrgencyLevel.DONE -> {
                    binding.tvExamUrgency.setTextColor(
                        ContextCompat.getColor(context, R.color.teal_800)
                    )
                    binding.tvExamUrgency.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.teal_50)
                        )
                    binding.tvExamDaysAway.setTextColor(
                        ContextCompat.getColor(context, R.color.teal_500)
                    )
                }
            }
        }

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

    class ExamDiffCallback : DiffUtil.ItemCallback<Exam>() {
        override fun areItemsTheSame(oldItem: Exam, newItem: Exam) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Exam, newItem: Exam) =
            oldItem == newItem
    }
}