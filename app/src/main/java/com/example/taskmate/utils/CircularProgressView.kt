package com.example.taskmate.utils

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.taskmate.R

/**
 * CircularProgressView.kt
 * A self-contained circular progress ring — no third party library.
 * Replaces com.mikhaellopez:circularprogressbar everywhere.
 *
 * XML usage:
 *   <com.example.taskmate.utils.CircularProgressView
 *       android:layout_width="72dp"
 *       android:layout_height="72dp"
 *       app:cpv_progress="75"
 *       app:cpv_progressColor="#1D9E75"
 *       app:cpv_trackColor="#E1F5EE"
 *       app:cpv_strokeWidth="8dp" />
 */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Progress value 0–100 ───────────────────────────────────────
    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 100f)
            invalidate()  // triggers redraw
        }

    // ── Colors ─────────────────────────────────────────────────────
    var progressColor: Int = Color.parseColor("#7059D0")
        set(value) { field = value; progressPaint.color = value; invalidate() }

    var trackColor: Int = Color.parseColor("#F3F0FF")
        set(value) { field = value; trackPaint.color = value; invalidate() }

    // ── Stroke width in pixels ─────────────────────────────────────
    var strokeWidth: Float = dpToPx(8f)
        set(value) {
            field = value
            progressPaint.strokeWidth = value
            trackPaint.strokeWidth = value
            invalidate()
        }

    // ── Paints ─────────────────────────────────────────────────────
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeCap   = Paint.Cap.ROUND
        color       = trackColor
        strokeWidth = this@CircularProgressView.strokeWidth
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeCap   = Paint.Cap.ROUND
        color       = progressColor
        strokeWidth = this@CircularProgressView.strokeWidth
    }

    private val oval = RectF()

    // ── Read XML attributes ────────────────────────────────────────
    init {
        if (attrs != null) {
            val a: TypedArray = context.obtainStyledAttributes(
                attrs, R.styleable.CircularProgressView
            )
            progress      = a.getFloat(R.styleable.CircularProgressView_cpv_progress, 0f)
            progressColor = a.getColor(R.styleable.CircularProgressView_cpv_progressColor,
                Color.parseColor("#7059D0"))
            trackColor    = a.getColor(R.styleable.CircularProgressView_cpv_trackColor,
                Color.parseColor("#F3F0FF"))
            strokeWidth   = a.getDimension(R.styleable.CircularProgressView_cpv_strokeWidth,
                dpToPx(8f))
            a.recycle()

            // Apply to paints after reading
            progressPaint.color       = progressColor
            progressPaint.strokeWidth = strokeWidth
            trackPaint.color          = trackColor
            trackPaint.strokeWidth    = strokeWidth
        }
    }

    // ── Draw ───────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val half    = strokeWidth / 2f
        val padding = half + paddingLeft

        oval.set(
            padding,
            padding,
            width  - padding,
            height - padding
        )

        // Draw background track (full circle)
        canvas.drawArc(oval, 0f, 360f, false, trackPaint)

        // Draw progress arc (starts at top, -90 degrees)
        val sweepAngle = 360f * (progress / 100f)
        canvas.drawArc(oval, -90f, sweepAngle, false, progressPaint)
    }

    // ── Helper ─────────────────────────────────────────────────────
    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    /**
     * Animates progress from current value to target value.
     * Call this instead of setting progress directly for smooth animation.
     */
    fun setProgressWithAnimation(targetProgress: Float, durationMs: Long = 600) {
        val start = this.progress
        val animator = android.animation.ValueAnimator.ofFloat(start, targetProgress).apply {
            duration = durationMs
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { anim ->
                progress = anim.animatedValue as Float
            }
        }
        animator.start()
    }
}