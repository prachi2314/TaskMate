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
 * SubjectProgressAdapter.kt
 * Location: ui/adapters/SubjectProgressAdapter.kt
 *
 * Displays the compact subject progress list on the Dashboard screen.
 * Uses item_subject.xml — same layout as SubjectAdapter but
 * no click actions (read-only on Dashboard).
 *
 * Separate from SubjectAdapter so Dashboard and Subject screen
 * can evolve independently without coupling.
 */
class SubjectProgressAdapter :
    ListAdapter<Subject, SubjectProgressAdapter.SubjectProgressViewHolder>(
        SubjectProgressDiffCallback()
    ) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SubjectProgressViewHolder {
        val binding = ItemSubjectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SubjectProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubjectProgressViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SubjectProgressViewHolder(
        private val binding: ItemSubjectBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(subject: Subject) {
            binding.tvSubjectEmoji.text   = subject.emoji
            binding.tvSubjectName.text    = subject.name
            binding.tvSubjectChapters.text = subject.chapterProgressLabel
            binding.tvSubjectPercent.text  = subject.progressLabel

            // Apply subject color to progress bar
            try {
                val color = Color.parseColor(subject.colorHex)
                binding.progressSubject.setIndicatorColor(color)
                binding.tvSubjectPercent.setTextColor(color)

                val lightBg = Color.argb(
                    38,
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color)
                )
                binding.cardSubjectIcon.setCardBackgroundColor(lightBg)
                binding.progressSubject.trackColor = lightBg
            } catch (e: IllegalArgumentException) { /* ignore */ }

            binding.progressSubject.progress = subject.progressPercent

            // Read-only on Dashboard — no click listeners
        }
    }

    class SubjectProgressDiffCallback : DiffUtil.ItemCallback<Subject>() {
        override fun areItemsTheSame(oldItem: Subject, newItem: Subject) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Subject, newItem: Subject) =
            oldItem == newItem
    }
}