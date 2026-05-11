package com.example.taskmate.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmate.data.model.Subject
import com.example.taskmate.databinding.ItemSubjectBinding

/**
 * SubjectAdapter.kt
 * Location: ui/adapters/SubjectAdapter.kt
 */
class SubjectAdapter(
    private val onChapterIncrement: (Subject) -> Unit,
    private val onChapterDecrement: (Subject) -> Unit,
    private val onMarkRevised: (Subject) -> Unit,
    private val onCardClick: (Subject) -> Unit,
    private val onDeleteClick: (Subject) -> Unit
) : ListAdapter<Subject, SubjectAdapter.SubjectViewHolder>(
    SubjectDiffCallback()
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SubjectViewHolder {
        val binding = ItemSubjectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SubjectViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SubjectViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    // ══════════════════════════════════════════════════════════════
    //  VIEW HOLDER
    // ══════════════════════════════════════════════════════════════

    inner class SubjectViewHolder(
        private val binding: ItemSubjectBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(subject: Subject) {

            // Emoji icon
            binding.tvSubjectEmoji.text = subject.emoji

            // Icon background color (light tint)
            applySubjectColor(subject.colorHex)

            // Name
            binding.tvSubjectName.text = subject.name

            // Chapter labels
            binding.tvSubjectChapters.text = subject.chapterProgressLabel
            binding.tvChapterCounter.text  =
                "${subject.completedChapters} / ${subject.totalChapters} done"

            // Percentage
            binding.tvSubjectPercent.text = subject.progressLabel
            try {
                binding.tvSubjectPercent.setTextColor(
                    Color.parseColor(subject.colorHex)
                )
            } catch (e: IllegalArgumentException) { }

            // Progress bar
            binding.progressSubject.progress = subject.progressPercent
            try {
                val color = Color.parseColor(subject.colorHex)
                val light = Color.argb(38,
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color)
                )
                binding.progressSubject.setIndicatorColor(color)
                binding.progressSubject.trackColor = light
            } catch (e: IllegalArgumentException) { }

            // Plus button
            binding.btnChapterPlus.isEnabled =
                subject.totalChapters == 0 ||
                        subject.completedChapters < subject.totalChapters

            binding.btnChapterPlus.setOnClickListener {
                onChapterIncrement(subject)
            }

            // Minus button
            binding.btnChapterMinus.isEnabled =
                subject.completedChapters > 0

            binding.btnChapterMinus.setOnClickListener {
                onChapterDecrement(subject)
            }

            // Mark as revised button
            binding.btnMarkRevised.setOnClickListener {
                onMarkRevised(subject)
            }

            // Card click
            binding.root.setOnClickListener {
                onCardClick(subject)
            }

            // Long press delete
            binding.root.setOnLongClickListener {
                onDeleteClick(subject)
                true
            }
        }

        private fun applySubjectColor(colorHex: String) {
            try {
                val color = Color.parseColor(colorHex)
                val light = Color.argb(38,
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color)
                )
                binding.cardSubjectIcon.setCardBackgroundColor(light)
            } catch (e: IllegalArgumentException) {
                binding.cardSubjectIcon.setCardBackgroundColor(
                    Color.parseColor("#F3F0FF")
                )
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DIFF CALLBACK
    // ══════════════════════════════════════════════════════════════

    class SubjectDiffCallback : DiffUtil.ItemCallback<Subject>() {
        override fun areItemsTheSame(
            oldItem: Subject,
            newItem: Subject
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: Subject,
            newItem: Subject
        ): Boolean = oldItem == newItem
    }
}