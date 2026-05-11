package com.example.taskmate.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmate.data.model.Subject
import com.example.taskmate.databinding.ItemProgressSubjectBinding

/**
 * MasteryAdapter.kt
 * Location: ui/adapters/MasteryAdapter.kt
 *
 * Displays the subject mastery bars in ProgressFragment → rv_mastery.
 * Each row shows: subject name + progress bar + percentage.
 *
 * Uses item_progress_subject.xml.
 * Simpler than SubjectAdapter — no emoji, no chapter count,
 * just the name and progress bar for a clean progress view.
 */
class MasteryAdapter :
    ListAdapter<Subject, MasteryAdapter.MasteryViewHolder>(MasteryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MasteryViewHolder {
        val binding = ItemProgressSubjectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MasteryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MasteryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MasteryViewHolder(
        private val binding: ItemProgressSubjectBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(subject: Subject) {

            // ── Subject name ───────────────────────────────────────
            // Show short name if available, otherwise first 7 chars
            binding.tvProgressSubjectName.text =
                subject.shortName.ifBlank { subject.name.take(7) }

            // ── Progress bar ───────────────────────────────────────
            binding.progressMasteryBar.progress = subject.progressPercent

            // Apply subject color to the bar
            try {
                val color = Color.parseColor(subject.colorHex)
                val lightColor = Color.argb(
                    38,
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color)
                )
                binding.progressMasteryBar.setIndicatorColor(color)
                binding.progressMasteryBar.trackColor = lightColor
                binding.tvProgressPercent.setTextColor(color)
            } catch (e: IllegalArgumentException) { /* ignore */ }

            // ── Percentage ─────────────────────────────────────────
            binding.tvProgressPercent.text = subject.progressLabel
        }
    }

    class MasteryDiffCallback : DiffUtil.ItemCallback<Subject>() {
        override fun areItemsTheSame(oldItem: Subject, newItem: Subject) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Subject, newItem: Subject) =
            oldItem == newItem
    }
}