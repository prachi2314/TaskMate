package com.example.taskmate

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.taskmate.databinding.ActivityMainBinding
import com.example.taskmate.ui.auth.LoginActivity
import com.example.taskmate.ui.dashboard.DashboardActivity

/**
 * MainActivity.kt
 * Location: java/com/yourname/taskmate/MainActivity.kt
 *
 * TaskMate Splash Screen.
 *
 * What it does:
 *  1. Shows the TaskMate logo with a smooth bounce-in animation
 *  2. Fades in the app name and tagline sequentially
 *  3. Animates the three loading dots at the bottom
 *  4. After 2.8 seconds checks Firebase Auth state
 *  5. Navigates to:
 *       DashboardActivity → if user is already logged in
 *       LoginActivity     → if no user is signed in
 *  6. Removes itself from back stack so Back button
 *     doesn't return to splash
 */
class MainActivity : AppCompatActivity() {

    // ── ViewBinding ────────────────────────────────────────────────
    private lateinit var binding: ActivityMainBinding

    // ── Firebase Auth ──────────────────────────────────────────────
    private lateinit var auth: FirebaseAuth

    // ── Constants ──────────────────────────────────────────────────
    private val SPLASH_DURATION = 2800L
    private val handler = Handler(Looper.getMainLooper())

    // ══════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ══════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Hide system UI for a full-screen splash feel
        window.statusBarColor = getColor(R.color.purple_800)

        // Start animations immediately
        startLogoAnimation()
        startDotsAnimation()

        // Navigate after splash duration
        handler.postDelayed({ navigateToNextScreen() }, SPLASH_DURATION)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel pending navigation to avoid crash on destroyed Activity
        handler.removeCallbacksAndMessages(null)
    }

    // ══════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════════

    /**
     * Checks Firebase Auth for a signed-in user and navigates
     * to the correct screen.
     */
    private fun navigateToNextScreen() {
        val destination = if (auth.currentUser != null) {
            // User already signed in → go straight to app
            Intent(this, DashboardActivity::class.java)
        } else {
            // No user → show login
            Intent(this, LoginActivity::class.java)
        }

        startActivity(destination)

        // Smooth fade transition between splash and next screen
        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )

        // Remove this Activity from back stack
        finish()
    }

    // ══════════════════════════════════════════════════════════════
    //  LOGO ANIMATION
    //  Logo bounces in → name fades up → tagline fades in → dots appear
    // ══════════════════════════════════════════════════════════════

    private fun startLogoAnimation() {

        // ── Logo: scale bounce + fade in ───────────────────────────
        val logoScaleX = ObjectAnimator.ofFloat(
            binding.ivSplashLogo, View.SCALE_X, 0.6f, 1.05f, 1f
        ).apply { duration = 700; interpolator = OvershootInterpolator(1.5f) }

        val logoScaleY = ObjectAnimator.ofFloat(
            binding.ivSplashLogo, View.SCALE_Y, 0.6f, 1.05f, 1f
        ).apply { duration = 700; interpolator = OvershootInterpolator(1.5f) }

        val logoFade = ObjectAnimator.ofFloat(
            binding.ivSplashLogo, View.ALPHA, 0f, 1f
        ).apply { duration = 500 }

        val logoAnim = AnimatorSet().apply {
            playTogether(logoScaleX, logoScaleY, logoFade)
        }

        // ── App name: slide up + fade in (starts 300ms after logo) ─
        binding.tvSplashAppName.translationY = 30f
        val nameFade = ObjectAnimator.ofFloat(
            binding.tvSplashAppName, View.ALPHA, 0f, 1f
        ).apply { duration = 500; startDelay = 300 }

        val nameSlide = ObjectAnimator.ofFloat(
            binding.tvSplashAppName, View.TRANSLATION_Y, 30f, 0f
        ).apply {
            duration = 500
            startDelay = 300
            interpolator = AccelerateDecelerateInterpolator()
        }

        // ── Tagline: fade in (starts 500ms after logo) ─────────────
        val tagFade = ObjectAnimator.ofFloat(
            binding.tvSplashTagline, View.ALPHA, 0f, 1f
        ).apply { duration = 500; startDelay = 500 }

        // ── Version label: fade in (starts 600ms) ──────────────────
        val versionFade = ObjectAnimator.ofFloat(
            binding.tvVersion, View.ALPHA, 0f, 0.6f
        ).apply { duration = 400; startDelay = 600 }

        // ── Dots container: fade in (starts 700ms) ─────────────────
        val dotsFade = ObjectAnimator.ofFloat(
            binding.loadingDots, View.ALPHA, 0f, 1f
        ).apply { duration = 400; startDelay = 700 }

        // Play all animations together (each has its own startDelay)
        AnimatorSet().apply {
            playTogether(
                logoAnim,
                nameFade, nameSlide,
                tagFade,
                versionFade,
                dotsFade
            )
            start()
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DOTS ANIMATION
    //  Three dots pulse in sequence: dot1 → dot2 → dot3 → repeat
    // ══════════════════════════════════════════════════════════════

    private fun startDotsAnimation() {
        animateDot(binding.dot1, 0L)
        animateDot(binding.dot2, 200L)
        animateDot(binding.dot3, 400L)
    }

    private fun animateDot(dot: View, delayMs: Long) {
        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(dot, View.SCALE_X, 1f, 1.6f).apply { duration = 350 },
                ObjectAnimator.ofFloat(dot, View.SCALE_Y, 1f, 1.6f).apply { duration = 350 },
                ObjectAnimator.ofFloat(dot, View.ALPHA, 0.4f, 1f).apply { duration = 350 }
            )
        }

        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(dot, View.SCALE_X, 1.6f, 1f).apply { duration = 350 },
                ObjectAnimator.ofFloat(dot, View.SCALE_Y, 1.6f, 1f).apply { duration = 350 },
                ObjectAnimator.ofFloat(dot, View.ALPHA, 1f, 0.4f).apply { duration = 350 }
            )
        }

        AnimatorSet().apply {
            playSequentially(scaleUp, scaleDown)
            startDelay = delayMs
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    // Loop the animation while activity is alive
                    if (!isDestroyed && !isFinishing) {
                        animateDot(dot, 0L)
                    }
                }
            })
            start()
        }
    }
}