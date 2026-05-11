package com.example.taskmate.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.example.taskmate.R
import com.example.taskmate.databinding.ActivityLoginBinding
import com.example.taskmate.ui.dashboard.DashboardActivity
import com.example.taskmate.utils.Constants
import com.example.taskmate.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * LoginActivity.kt
 * Location: ui/auth/LoginActivity.kt
 *
 * TaskMate Login and Register screen.
 *
 * Handles:
 *  1. Toggle between Login / Register modes (single screen)
 *  2. Email + Password login and registration
 *  3. Google Sign-In
 *  4. Forgot password email
 *  5. Real-time input validation
 *  6. Loading state management
 *  7. Auto-skip if user already logged in
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    // ── ViewBinding ────────────────────────────────────────────────
    private lateinit var binding: ActivityLoginBinding

    // ── ViewModel (injected by Hilt) ───────────────────────────────
    private val viewModel: AuthViewModel by viewModels()

    // ── UI state ───────────────────────────────────────────────────
    private var isLoginMode = true

    // ── Google Sign-In ─────────────────────────────────────────────
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { token ->
                viewModel.loginWithGoogle(token)
            } ?: showToast("Google sign-in failed. Try again.")
        } catch (e: ApiException) {
            showToast("Google sign-in cancelled.")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ══════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // If already signed in skip login screen
        if (viewModel.isUserLoggedIn) {
            navigateToDashboard()
            return
        }

        setupGoogleSignIn()
        setupClickListeners()
        setupInputValidation()
        setupSwitchModeText()
        observeViewModel()
    }

    // ══════════════════════════════════════════════════════════════
    //  SETUP
    // ══════════════════════════════════════════════════════════════

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(Constants.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {

        // Primary button — Sign in OR Create account
        binding.btnPrimaryAction.setOnClickListener {
            hideKeyboard()
            if (isLoginMode) performLogin() else performRegister()
        }

        // Google Sign-In
        binding.btnGoogleSignIn.setOnClickListener {
            hideKeyboard()
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }

        // Forgot password
        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    /**
     * Makes only the action word (Sign up / Sign in) clickable and purple.
     * The rest of the text is non-clickable gray.
     */
    private fun setupSwitchModeText() {
        updateSwitchModeText()
    }

    private fun updateSwitchModeText() {
        val fullText = if (isLoginMode)
            "Don't have an account? Sign up"
        else
            "Already have an account? Sign in"

        val actionWord = if (isLoginMode) "Sign up" else "Sign in"
        val spannable  = SpannableString(fullText)
        val start      = fullText.lastIndexOf(actionWord)
        val end        = start + actionWord.length

        if (start >= 0) {
            spannable.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(this, R.color.purple_600)
                ),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) { toggleMode() }
                    override fun updateDrawState(ds: android.text.TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                    }
                },
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.tvSwitchMode.text = spannable
        binding.tvSwitchMode.movementMethod = LinkMovementMethod.getInstance()
        binding.tvSwitchMode.highlightColor  = android.graphics.Color.TRANSPARENT
    }

    private fun setupInputValidation() {
        // Clear error messages as user starts typing
        binding.etName.doOnTextChanged     { _, _, _, _ -> binding.tilName.error = null }
        binding.etEmail.doOnTextChanged    { _, _, _, _ -> binding.tilEmail.error = null }
        binding.etPassword.doOnTextChanged { _, _, _, _ -> binding.tilPassword.error = null }
    }

    // ══════════════════════════════════════════════════════════════
    //  AUTH ACTIONS
    // ══════════════════════════════════════════════════════════════

    private fun performLogin() {
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        if (!validateEmailPassword(email, password)) return
        viewModel.loginWithEmail(email, password)
    }

    private fun performRegister() {
        val name     = binding.etName.text.toString().trim()
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        if (!validateName(name)) return
        if (!validateEmailPassword(email, password)) return
        viewModel.registerWithEmail(name, email, password)
    }

    private fun showForgotPasswordDialog() {
        val currentEmail = binding.etEmail.text.toString().trim()

        val builder = AlertDialog.Builder(this)
            .setTitle("Reset password")

        if (currentEmail.isNotBlank()) {
            builder.setMessage("Send a reset link to:\n$currentEmail?")
            builder.setPositiveButton("Send") { _, _ ->
                viewModel.sendPasswordResetEmail(currentEmail)
            }
        } else {
            val input = android.widget.EditText(this).apply {
                hint = "Enter your email"
                inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                setPadding(48, 24, 48, 8)
            }
            builder.setView(input)
            builder.setPositiveButton("Send") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotBlank()) viewModel.sendPasswordResetEmail(email)
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    // ══════════════════════════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════════════════════════

    private fun validateName(name: String): Boolean {
        if (name.isBlank()) {
            binding.tilName.error = "Full name is required"
            binding.tilName.requestFocus()
            return false
        }
        return true
    }

    private fun validateEmailPassword(email: String, password: String): Boolean {
        if (email.isBlank()) {
            binding.tilEmail.error = "Email is required"
            binding.tilEmail.requestFocus()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email address"
            binding.tilEmail.requestFocus()
            return false
        }
        if (password.isBlank()) {
            binding.tilPassword.error = "Password is required"
            binding.tilPassword.requestFocus()
            return false
        }
        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            binding.tilPassword.requestFocus()
            return false
        }
        return true
    }

    // ══════════════════════════════════════════════════════════════
    //  OBSERVE VIEWMODEL
    // ══════════════════════════════════════════════════════════════

    private fun observeViewModel() {

        // Auth state
        viewModel.authState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> setLoadingState(true)
                is Resource.Success -> {
                    setLoadingState(false)
                    navigateToDashboard()
                }
                is Resource.Error -> {
                    setLoadingState(false)
                    handleAuthError(resource.message)
                }
            }
        }

        // Password reset state
        viewModel.resetEmailState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> { /* optional spinner */ }
                is Resource.Success -> showToast("Reset link sent! Check your inbox.")
                is Resource.Error   -> showToast(resource.message)
            }
        }
    }

    /**
     * Maps Firebase error messages to friendly inline field errors.
     */
    private fun handleAuthError(message: String) {
        when {
            message.contains("password", ignoreCase = true) &&
                    message.contains("incorrect", ignoreCase = true) ->
                binding.tilPassword.error = "Incorrect password"

            message.contains("no user record", ignoreCase = true) ||
                    message.contains("user not found", ignoreCase = true) ->
                binding.tilEmail.error = "No account found with this email"

            message.contains("already in use", ignoreCase = true) ->
                binding.tilEmail.error = "This email is already registered"

            message.contains("network", ignoreCase = true) ->
                showToast("No internet. Check your connection.")

            else -> showToast(message)
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Switches between Login and Register modes.
     * Animates the name field in and out.
     */
    private fun toggleMode() {
        isLoginMode = !isLoginMode

        val fadeAnim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        binding.cardForm.startAnimation(fadeAnim)

        if (isLoginMode) {
            // Switch to Login
            binding.tvFormBadge.text    = "Welcome back"
            binding.tvFormTitle.text    = "Sign in"
            binding.tvFormSubtitle.text = "Welcome back! Enter your details."
            binding.btnPrimaryAction.text = "Sign in"
            binding.tvForgotPassword.visibility = View.VISIBLE

            // Animate name field out
            binding.tilName.animate()
                .alpha(0f)
                .translationY(-16f)
                .setDuration(200)
                .withEndAction { binding.tilName.visibility = View.GONE }
                .start()

        } else {
            // Switch to Register
            binding.tvFormBadge.text    = "New here"
            binding.tvFormTitle.text    = "Create account"
            binding.tvFormSubtitle.text = "Join TaskMate and start studying smarter!"
            binding.btnPrimaryAction.text = "Create account"
            binding.tvForgotPassword.visibility = View.GONE

            // Animate name field in
            binding.tilName.alpha        = 0f
            binding.tilName.translationY = -16f
            binding.tilName.visibility   = View.VISIBLE
            binding.tilName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .start()
        }

        updateSwitchModeText()
        clearForm()
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

        val actionLabel = when {
            isLoading && isLoginMode  -> "Signing in…"
            isLoading && !isLoginMode -> "Creating account…"
            isLoginMode               -> "Sign in"
            else                      -> "Create account"
        }
        binding.btnPrimaryAction.text      = actionLabel
        binding.btnPrimaryAction.isEnabled = !isLoading
        binding.btnGoogleSignIn.isEnabled  = !isLoading
        binding.etEmail.isEnabled          = !isLoading
        binding.etPassword.isEnabled       = !isLoading
        binding.etName.isEnabled           = !isLoading
    }

    private fun clearForm() {
        binding.etName.text?.clear()
        binding.etEmail.text?.clear()
        binding.etPassword.text?.clear()
        binding.tilName.error     = null
        binding.tilEmail.error    = null
        binding.tilPassword.error = null
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // ══════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════════

    private fun navigateToDashboard() {
        startActivity(
            Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }
}