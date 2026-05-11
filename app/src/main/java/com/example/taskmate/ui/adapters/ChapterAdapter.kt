package com.example.taskmate.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmate.databinding.ItemChapterBinding

/**
 * ChapterAdapter.kt
 * Shows individual chapters for a subject.
 * Each chapter can be marked complete or revised.
 */
class ChapterAdapter(
    private val onChapterChecked: (index: Int, isChecked: Boolean) -> Unit,
    private val onMarkRevised: (index: Int) -> Unit
) : ListAdapter<ChapterItem, ChapterAdapter.ChapterViewHolder>(ChapterDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val binding = ItemChapterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ChapterViewHolder(
        private val binding: ItemChapterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chapter: ChapterItem, index: Int) {
            // Chapter number
            binding.tvChapterNumber.text = (index + 1).toString()

            // Chapter name
            binding.tvChapterName.text = chapter.name.ifBlank {
                "Chapter ${index + 1}"
            }

            // Strikethrough if completed
            if (chapter.isCompleted) {
                binding.tvChapterName.paintFlags =
                    binding.tvChapterName.paintFlags or
                            android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvChapterName.alpha = 0.5f
                binding.tvChapterNumber.setBackgroundResource(
                    com.example.taskmate.R.drawable.bg_session_dot_active
                )
            } else {
                binding.tvChapterName.paintFlags =
                    binding.tvChapterName.paintFlags and
                            android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvChapterName.alpha = 1f
                binding.tvChapterNumber.setBackgroundResource(
                    com.example.taskmate.R.drawable.bg_session_dot_inactive
                )
            }

            // Revised badge
            binding.tvRevisedBadge.visibility =
                if (chapter.isRevised) View.VISIBLE else View.GONE

            // Checkbox
            binding.cbChapterDone.setOnCheckedChangeListener(null)
            binding.cbChapterDone.isChecked = chapter.isCompleted
            binding.cbChapterDone.setOnCheckedChangeListener { _, isChecked ->
                onChapterChecked(index, isChecked)
            }

            // Long press to mark as revised
            binding.root.setOnLongClickListener {
                onMarkRevised(index)
                true
            }
        }
    }

    class ChapterDiffCallback : DiffUtil.ItemCallback<ChapterItem>() {
        override fun areItemsTheSame(a: ChapterItem, b: ChapterItem) =
            a.index == b.index
        override fun areContentsTheSame(a: ChapterItem, b: ChapterItem) =
            a == b
    }
}

/**
 * Data class representing one chapter row.
 */
data class ChapterItem(
    val index: Int,
    val name: String,
    val isCompleted: Boolean,
    val isRevised: Boolean
)