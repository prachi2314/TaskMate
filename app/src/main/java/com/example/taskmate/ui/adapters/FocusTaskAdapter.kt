package com.example.taskmate.ui.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmate.R
import com.example.taskmate.data.model.Task
import com.example.taskmate.databinding.ItemFocusTaskBinding

/**
 * FocusTaskAdapter.kt
 * Location: ui/adapters/FocusTaskAdapter.kt
 *
 * Displays the current task mini-list in FocusFragment → rv_focus_tasks.
 * Dark themed — all colors reference dark_* color tokens.
 *
 * Shows max 5 tasks so the card doesn't overflow the screen.
 * Completed tasks show with strikethrough and reduced opacity.
 *
 * @param onCheckClick called when the checkbox is tapped
 */
class FocusTaskAdapter(
    private val onCheckClick: (Task) -> Unit = {}
) : ListAdapter<Task, FocusTaskAdapter.FocusTaskViewHolder>(FocusTaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FocusTaskViewHolder {
        val binding = ItemFocusTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FocusTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FocusTaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FocusTaskViewHolder(
        private val binding: ItemFocusTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            val context = binding.root.context

            // ── Task title ─────────────────────────────────────────
            binding.tvFocusTaskTitle.text = task.title

            // ── Completed state ────────────────────────────────────
            if (task.completed) {
                // Strikethrough + dim text for completed tasks
                binding.tvFocusTaskTitle.paintFlags =
                    binding.tvFocusTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvFocusTaskTitle.alpha = 0.4f
                binding.tvFocusTaskTitle.setTextColor(
                    ContextCompat.getColor(context, R.color.dark_text_secondary)
                )
            } else {
                // Normal active task
                binding.tvFocusTaskTitle.paintFlags =
                    binding.tvFocusTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvFocusTaskTitle.alpha = 1f
                binding.tvFocusTaskTitle.setTextColor(
                    ContextCompat.getColor(context, R.color.dark_text_primary)
                )
            }

            // ── Checkbox ───────────────────────────────────────────
            binding.cbFocusTask.setOnCheckedChangeListener(null)
            binding.cbFocusTask.isChecked = task.completed
            binding.cbFocusTask.setOnCheckedChangeListener { _, _ ->
                onCheckClick(task)
            }
        }
    }

    class FocusTaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task) =
            oldItem == newItem
    }
}