package com.example.taskmate.ui.adapters

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmate.R
import com.example.taskmate.data.model.Task
import com.example.taskmate.databinding.ItemTaskBinding
import com.example.taskmate.utils.toDisplayDate

/**
 * TaskAdapter.kt
 * Location: ui/adapters/TaskAdapter.kt
 *
 * Displays the task list in DashboardFragment → rv_tasks.
 *
 * Uses ListAdapter with DiffUtil for efficient updates —
 * only changed items are redrawn, not the whole list.
 *
 * Features:
 *  - Checkbox toggles task completed state
 *  - Strikethrough text when task is done
 *  - Subject tag pill with subject color
 *  - Priority indicator dot
 *  - Due date label with overdue highlight
 *  - Swipe to delete (handled via ItemTouchHelper in Fragment)
 *
 * @param onCheckClick  called when checkbox is tapped
 * @param onDeleteClick called when delete action is triggered
 */
class TaskAdapter(
    private val onCheckClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        // Inflate item_task.xml and wrap it in a ViewHolder
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ── ViewHolder ─────────────────────────────────────────────────
    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            // ── Title ──────────────────────────────────────────────
            binding.tvTaskTitle.text = task.title

            // Strikethrough when done
            if (task.completed) {
                binding.tvTaskTitle.paintFlags =
                    binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvTaskTitle.alpha = 0.5f
            } else {
                binding.tvTaskTitle.paintFlags =
                    binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvTaskTitle.alpha = 1f
            }

            // ── Checkbox ───────────────────────────────────────────
            // Set without triggering listener to avoid loop
            binding.cbTaskDone.setOnCheckedChangeListener(null)
            binding.cbTaskDone.isChecked = task.completed
            binding.cbTaskDone.setOnCheckedChangeListener { _, _ ->
                onCheckClick(task)
            }

            // ── Subject tag ────────────────────────────────────────
            if (task.subjectName.isNotBlank()) {
                binding.tvTaskSubjectTag.text = task.subjectName.take(4)
                binding.tvTaskSubjectTag.visibility = android.view.View.VISIBLE

                // Apply subject color to the tag background
                try {
                    val color = Color.parseColor(task.subjectColorHex)
                    // Make the background color very light (10% opacity)
                    val lightColor = Color.argb(26, Color.red(color), Color.green(color), Color.blue(color))
                    binding.tvTaskSubjectTag.background.setTint(lightColor)
                    binding.tvTaskSubjectTag.setTextColor(color)
                } catch (e: IllegalArgumentException) {
                    // Fallback to purple if color parsing fails
                    binding.tvTaskSubjectTag.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.purple_600)
                    )
                }
            } else {
                binding.tvTaskSubjectTag.visibility = android.view.View.GONE
            }

            // ── Due date ───────────────────────────────────────────
            if (task.hasDueDate) {
                binding.tvTaskDue.visibility = android.view.View.VISIBLE
                binding.tvTaskDue.text = when {
                    task.isDueToday -> "Due today"
                    task.isOverdue  -> "Overdue · ${task.dueDate.toDisplayDate()}"
                    else            -> "Due ${task.dueDate.toDisplayDate()}"
                }
                // Red text for overdue tasks
                binding.tvTaskDue.setTextColor(
                    if (task.isOverdue && !task.completed)
                        ContextCompat.getColor(binding.root.context, R.color.red_400)
                    else
                        ContextCompat.getColor(binding.root.context, R.color.purple_400)
                )
            } else {
                binding.tvTaskDue.visibility = android.view.View.GONE
            }

            // ── Click listeners ────────────────────────────────────
            // Long press to delete
            binding.root.setOnLongClickListener {
                onDeleteClick(task)
                true
            }
        }
    }

    // ── DiffUtil callback ──────────────────────────────────────────
    // DiffUtil compares old and new lists to find the minimum set
    // of changes — only changed items are redrawn in the RecyclerView
    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {

        // Called to check if two items represent the same task
        // (same document ID = same task, even if content changed)
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        // Called to check if the content of two items is identical
        // (if true, no rebind needed — the UI stays as is)
        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}