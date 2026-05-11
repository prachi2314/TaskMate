package com.example.taskmate.data.model

/**
 * Subject.kt — Updated with chapter names list
 */
data class Subject(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val shortName: String = "",
    val colorHex: String = "#7059D0",
    val emoji: String = "📚",
    val totalChapters: Int = 0,
    val completedChapters: Int = 0,

    // List of chapter names — e.g. ["Introduction", "Arrays", "Linked List"]
    // Empty if user did not enter chapter names
    val chapterNames: List<String> = emptyList(),

    // Which chapter indices have been marked as revised
    // e.g. [0, 2, 3] means chapters 1, 3, 4 are revised
    val revisedChapters: List<Int> = emptyList(),

    val examDate: Long = 0L,
    val notes: String = "",
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progressPercent: Int
        get() = if (totalChapters == 0) 0
        else ((completedChapters.toFloat() / totalChapters.toFloat()) * 100).toInt()

    val progressFloat: Float
        get() = progressPercent.toFloat()

    val chapterProgressLabel: String
        get() = "$completedChapters / $totalChapters chapters"

    val progressLabel: String
        get() = "$progressPercent%"

    val isCompleted: Boolean
        get() = totalChapters > 0 && completedChapters >= totalChapters

    val daysUntilExam: Int?
        get() {
            if (examDate == 0L) return null
            val diff = examDate - System.currentTimeMillis()
            return if (diff < 0) 0 else (diff / (1000 * 60 * 60 * 24)).toInt()
        }

    val tagLabel: String
        get() = shortName.ifBlank { name.take(4) }

    val revisionPercent: Int
        get() = if (totalChapters == 0) 0
        else ((revisedChapters.size.toFloat() / totalChapters.toFloat()) * 100).toInt()

    companion object {
        val PRESET_COLORS = listOf(
            "#378ADD", "#7059D0", "#1D9E75",
            "#D4537E", "#BA7517", "#D85A30",
            "#E24B4A", "#085041"
        )
        val PRESET_EMOJIS = listOf(
            "📚", "⚛️", "📐", "🧪", "🏛️", "📖",
            "🖥️", "🌍", "🔬", "📊", "🎭", "🎨"
        )
    }
}