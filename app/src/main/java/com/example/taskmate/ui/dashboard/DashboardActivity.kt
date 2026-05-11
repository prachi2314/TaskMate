package com.example.taskmate.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.taskmate.R
import com.example.taskmate.databinding.ActivityDashboardBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * DashboardActivity.kt
 * Location: ui/dashboard/DashboardActivity.kt
 *
 * Main host activity after login.
 * Contains NavHostFragment + BottomNavigationView.
 * Handles:
 *  1. Navigation setup
 *  2. Back button behaviour (minimize instead of close)
 *  3. Status bar color
 *  4. Notification permission request
 */
@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Fix status bar — content does not go behind it
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_600)

        setupNavigation()
        requestNotificationPermission()
    }

    // ══════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════════

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment)
                as NavHostFragment
        navController = navHostFragment.navController

        // Wire bottom nav to nav controller
        // One line handles all tab switching automatically
        binding.bottomNavigation.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    /**
     * Back button behaviour:
     * If NOT on dashboard tab → go to dashboard tab
     * If ON dashboard tab → minimize app (do not close)
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (navController.currentDestination?.id != R.id.dashboardFragment) {
            navController.navigate(R.id.dashboardFragment)
        } else {
            moveTaskToBack(true)
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  NOTIFICATION PERMISSION
    // ══════════════════════════════════════════════════════════════

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SIGN OUT HELPER
    // ══════════════════════════════════════════════════════════════

    /**
     * Called from DashboardFragment profile menu.
     * Navigates to LoginActivity and clears back stack.
     */
    fun navigateToLogin() {
        startActivity(
            Intent(
                this,
                com.example.taskmate.ui.auth.LoginActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}