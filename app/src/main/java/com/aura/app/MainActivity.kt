package com.aura.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.aura.app.ui.common.AURANavigation
import com.aura.app.ui.onboarding.OnboardingScreen
import com.aura.app.ui.permissions.PermissionChecker
import com.aura.app.ui.student.ClassModeOverlay
import com.aura.app.utils.Constants
import com.aura.app.utils.PermissionManager
import kotlinx.coroutines.launch

/**
 * Main Activity for AURA Android Classroom Management System
 *
 * This is the entry point that handles:
 * - Permission requests for all required AURA features
 * - Navigation between student and teacher interfaces
 * - Onboarding flow for new users
 * - Deep linking and emergency access
 * - Class Mode activation and management
 */
class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager
    private var isClassModeActive by mutableStateOf(false)
    private var hasCompletedOnboarding by mutableStateOf(false)
    private var currentUserRole by mutableStateOf(Constants.UserRole.STUDENT)

    // Permission request launchers
    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            handleOverlayPermissionResult(granted)
        }

    private val mediaProjectionPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleMediaProjectionResult(result.resultCode)
        }

    private val accessibilityPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleAccessibilityPermissionResult(result.resultCode)
        }

    private val usageStatsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleUsageStatsPermissionResult(result.resultCode)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        initializeApp()
        setupUI()
    }

    /**
     * Initialize core AURA components and check initial state
     */
    private fun initializeApp() {
        permissionManager = PermissionManager(this)

        // Check if user is currently in Class Mode
        checkClassModeStatus()

        // Check if user has completed onboarding
        checkOnboardingStatus()

        // Determine user role (student/teacher)
        determineUserRole()

        // Handle deep links and emergency activation
        handleIntent(intent)
    }

    /**
     * Setup main UI with Jetpack Compose
     */
    private fun setupUI() {
        setContent {
            AURATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }

    @Composable
    private fun MainContent() {
        val context = LocalContext.current

        // Check and request permissions first
        if (!permissionManager.hasAllPermissions()) {
            PermissionRequestScreen(
                onRequestPermissions = { requestRequiredPermissions() }
            )
            return
        }

        // Show onboarding if not completed
        if (!hasCompletedOnboarding) {
            OnboardingScreen(
                onComplete = { hasCompletedOnboarding = true }
            )
            return
        }

        // Show Class Mode overlay if active
        if (isClassModeActive) {
            ClassModeOverlay(
                onExitClassMode = { exitClassMode() }
            )
            return
        }

        // Main navigation based on user role
        AURANavigation(
            userRole = currentUserRole,
            onClassModeToggle = { activate ->
                if (activate) {
                    activateClassMode()
                } else {
                    exitClassMode()
                }
            },
            onEmergencyActivated = { handleEmergencyRequest() }
        )
    }

    @Composable
    private fun PermissionRequestScreen(
        onRequestPermissions: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "AURA - Classroom Management",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "AURA requires several permissions to function properly in classroom environments:",
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            PermissionChecker(
                permissionManager = permissionManager,
                onPermissionsRequest = onRequestPermissions
            )
        }
    }

    /**
     * Request all required permissions for AURA functionality
     */
    private fun requestRequiredPermissions() {
        lifecycleScope.launch {
            val missingPermissions = permissionManager.getMissingPermissions()

            missingPermissions.forEach { permission ->
                when (permission.type) {
                    Constants.PermissionType.OVERLAY -> {
                        overlayPermissionLauncher.launch(permission.permission)
                    }
                    Constants.PermissionType.MEDIA_PROJECTION -> {
                        requestMediaProjectionPermission()
                    }
                    Constants.PermissionType.ACCESSIBILITY -> {
                        requestAccessibilityPermission()
                    }
                    Constants.PermissionType.USAGE_STATS -> {
                        requestUsageStatsPermission()
                    }
                    Constants.PermissionType.LOCATION -> {
                        overlayPermissionLauncher.launch(permission.permission)
                    }
                    else -> {
                        overlayPermissionLauncher.launch(permission.permission)
                    }
                }
            }
        }
    }

    /**
     * Request MediaProjection permission for screenshot capture
     */
    private fun requestMediaProjectionPermission() {
        try {
            val mediaProjectionManager =
                getSystemService(android.media.projection.MediaProjectionManager::class.java)
            val intent = mediaProjectionManager.createScreenCaptureIntent()
            mediaProjectionPermissionLauncher.launch(intent)
        } catch (e: Exception) {
            showError("Failed to request screen capture permission")
        }
    }

    /**
     * Request Accessibility permission for escape detection
     */
    private fun requestAccessibilityPermission() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
            accessibilityPermissionLauncher.launch(intent)
        } catch (e: Exception) {
            showError("Failed to request accessibility permission")
        }
    }

    /**
     * Request Usage Stats permission for app monitoring
     */
    private fun requestUsageStatsPermission() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
            usageStatsPermissionLauncher.launch(intent)
        } catch (e: Exception) {
            showError("Failed to request usage access permission")
        }
    }

    /**
     * Handle permission results
     */
    private fun handleOverlayPermissionResult(granted: Boolean) {
        if (granted) {
            showToast("Overlay permission granted")
        } else {
            showError("Overlay permission is required for Class Mode functionality")
        }
        checkAndUpdatePermissions()
    }

    private fun handleMediaProjectionResult(resultCode: Int) {
        if (resultCode == RESULT_OK) {
            showToast("Screen capture permission granted")
        } else {
            showError("Screen capture permission is required for AI monitoring")
        }
        checkAndUpdatePermissions()
    }

    private fun handleAccessibilityPermissionResult(resultCode: Int) {
        // Accessibility permissions require manual checking
        checkAndUpdatePermissions()
    }

    private fun handleUsageStatsPermissionResult(resultCode: Int) {
        // Usage stats permissions require manual checking
        checkAndUpdatePermissions()
    }

    /**
     * Check if all permissions are granted
     */
    private fun checkAndUpdatePermissions() {
        if (permissionManager.hasAllPermissions()) {
            showToast("All permissions granted - AURA is ready!")
        }
    }

    /**
     * Check Class Mode status from shared preferences
     */
    private fun checkClassModeStatus() {
        val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        isClassModeActive = prefs.getBoolean(Constants.PREF_CLASS_MODE_ACTIVE, false)
    }

    /**
     * Check onboarding completion status
     */
    private fun checkOnboardingStatus() {
        val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        hasCompletedOnboarding = prefs.getBoolean(Constants.PREF_ONBOARDING_COMPLETE, false)
    }

    /**
     * Determine user role (student/teacher)
     */
    private fun determineUserRole() {
        val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        currentUserRole = when (prefs.getString(Constants.PREF_USER_ROLE, Constants.UserRole.STUDENT)) {
            Constants.UserRole.TEACHER -> Constants.UserRole.TEACHER
            else -> Constants.UserRole.STUDENT
        }
    }

    /**
     * Activate Class Mode for current session
     */
    private fun activateClassMode() {
        try {
            // Save to preferences
            getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(Constants.PREF_CLASS_MODE_ACTIVE, true)
                .apply()

            isClassModeActive = true

            // Start Class Mode service
            val intent = Intent(this, com.aura.app.service.ClassModeService::class.java)
            startForegroundService(intent)

            showToast("Class Mode activated")
        } catch (e: Exception) {
            showError("Failed to activate Class Mode")
        }
    }

    /**
     * Exit Class Mode and restore normal functionality
     */
    private fun exitClassMode() {
        try {
            // Update preferences
            getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(Constants.PREF_CLASS_MODE_ACTIVE, false)
                .apply()

            isClassModeActive = false

            // Stop Class Mode service
            val intent = Intent(this, com.aura.app.service.ClassModeService::class.java)
            stopService(intent)

            showToast("Class Mode deactivated")
        } catch (e: Exception) {
            showError("Failed to exit Class Mode")
        }
    }

    /**
     * Handle emergency requests from students
     */
    private fun handleEmergencyRequest() {
        try {
            // Start emergency service
            val intent = Intent(this, com.aura.app.service.EmergencyService::class.java)
            startForegroundService(intent)

            // Notify teacher
            showToast("Emergency request sent to teacher")

        } catch (e: Exception) {
            showError("Failed to send emergency request")
        }
    }

    /**
     * Handle incoming intents (deep links, emergency activation)
     */
    private fun handleIntent(intent: Intent?) {
        intent?.let {
            when (it.action) {
                Constants.ACTION_EMERGENCY_ACTIVATE -> {
                    handleEmergencyRequest()
                }
                Constants.ACTION_CLASS_MODE_ENABLE -> {
                    activateClassMode()
                }
                Constants.ACTION_CLASS_MODE_DISABLE -> {
                    exitClassMode()
                }
                // Handle deep links for authentication
                Intent.ACTION_VIEW -> {
                    handleDeepLink(it)
                }
            }
        }
    }

    /**
     * Handle authentication deep links
     */
    private fun handleDeepLink(intent: Intent) {
        // Implementation for handling Supabase authentication
        // This would process the authentication tokens
        // and update the user's login state
    }

    /**
     * Utility functions for showing messages
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
    }

    /**
     * Handle new intent when activity is already running
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * Save state before activity destruction
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_CLASS_MODE_ACTIVE, isClassModeActive)
        outState.putBoolean(KEY_ONBOARDING_COMPLETE, hasCompletedOnboarding)
        outState.putString(KEY_USER_ROLE, currentUserRole)
    }

    /**
     * Restore state after activity recreation
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isClassModeActive = savedInstanceState.getBoolean(KEY_CLASS_MODE_ACTIVE, false)
        hasCompletedOnboarding = savedInstanceState.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        currentUserRole = savedInstanceState.getString(KEY_USER_ROLE, Constants.UserRole.STUDENT)
    }

    companion object {
        private const val KEY_CLASS_MODE_ACTIVE = "class_mode_active"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_USER_ROLE = "user_role"
    }
}