package com.aura.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager
import com.aura.app.data.database.AppDatabase
import com.aura.app.data.repository.AttendanceRepository
import com.aura.app.data.repository.SessionRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.network.SupabaseClient
import com.aura.app.service.ClassModeService
import com.aura.app.service.SyncService
import com.aura.app.utils.CrashReporting
import com.aura.app.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * AURA Application class
 *
 * Initializes all core components of the AURA system including:
 * - Database connections with encryption
 * - Network clients for Supabase
 * - Background services for monitoring
 * - Crash reporting and logging
 * - Repository patterns for data access
 */
class AURAApplication : Application(), Configuration.Provider {

    // Application-wide coroutine scope
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Database instance (encrypted with SQLCipher)
    lateinit var database: AppDatabase
        private set

    // Repository instances
    lateinit var userRepository: UserRepository
        private set
    lateinit var sessionRepository: SessionRepository
        private set
    lateinit var attendanceRepository: AttendanceRepository
        private set

    // Network client
    lateinit var supabaseClient: SupabaseClient
        private set

    // Logger instance
    lateinit var logger: Logger
        private set

    override fun onCreate() {
        super.onCreate()

        try {
            initializeComponents()
            setupCrashReporting()
            startEssentialServices()
            logger.info("AURA Application initialized successfully")
        } catch (e: Exception) {
            // Fallback initialization
            handleInitializationError(e)
        }
    }

    /**
     * Initialize all core components of the AURA system
     */
    private fun initializeComponents() {
        // Initialize logging first
        logger = Logger(this)

        // Initialize encrypted database
        initializeDatabase()

        // Initialize network client
        initializeNetworkClient()

        // Initialize repositories
        initializeRepositories()

        logger.debug("All core components initialized")
    }

    /**
     * Initialize the encrypted SQLite database with Room
     */
    private fun initializeDatabase() {
        applicationScope.launch {
            try {
                database = AppDatabase.getDatabase(this@AURAApplication)
                logger.info("Encrypted database initialized successfully")
            } catch (e: Exception) {
                logger.error("Failed to initialize database", e)
                throw e
            }
        }
    }

    /**
     * Initialize Supabase client for backend connectivity
     */
    private fun initializeNetworkClient() {
        applicationScope.launch {
            try {
                supabaseClient = SupabaseClient.getInstance(this@AURAApplication)
                logger.info("Supabase client initialized")
            } catch (e: Exception) {
                logger.error("Failed to initialize network client", e)
                // Network initialization should not crash the app
                logger.warn("Continuing without network connectivity")
            }
        }
    }

    /**
     * Initialize repository instances for data access
     */
    private fun initializeRepositories() {
        applicationScope.launch {
            try {
                userRepository = UserRepository(database.userDao(), supabaseClient)
                sessionRepository = SessionRepository(database.sessionDao(), supabaseClient)
                attendanceRepository = AttendanceRepository(database.attendanceDao(), supabaseClient)
                logger.debug("All repositories initialized")
            } catch (e: Exception) {
                logger.error("Failed to initialize repositories", e)
                throw e
            }
        }
    }

    /**
     * Setup crash reporting and error tracking
     */
    private fun setupCrashReporting() {
        try {
            CrashReporting.initialize(this)
            logger.info("Crash reporting initialized")
        } catch (e: Exception) {
            // Crash reporting is optional
            logger.warn("Could not initialize crash reporting: ${e.message}")
        }
    }

    /**
     * Start essential background services
     */
    private fun startEssentialServices() {
        applicationScope.launch {
            try {
                // Start sync service for offline data synchronization
                val syncIntent = Intent(this@AURAApplication, SyncService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(syncIntent)
                } else {
                    startService(syncIntent)
                }

                logger.info("Essential services started")
            } catch (e: Exception) {
                logger.error("Failed to start essential services", e)
                // Services are critical, but app should continue
            }
        }
    }

    /**
     * Handle initialization errors gracefully
     */
    private fun handleInitializationError(e: Exception) {
        try {
            logger.error("Critical initialization error", e)
            // Report to crash reporting service
            CrashReporting.recordException(e)

            // Attempt to continue in safe mode
            initializeSafeMode()

        } catch (safeModeError: Exception) {
            // Last resort - log and continue
            // Android won't let us easily restart the app from here
            android.util.Log.e("AURAApp", "Critical initialization failure", safeModeError)
        }
    }

    /**
     * Initialize app in safe mode with limited functionality
     */
    private fun initializeSafeMode() {
        logger.warn("Initializing in safe mode with limited functionality")

        // Initialize only the most critical components
        logger = Logger(this)

        // Database is critical for basic functionality
        try {
            initializeDatabase()
        } catch (e: Exception) {
            android.util.Log.e("AURAApp", "Safe mode database initialization failed", e)
        }
    }

    /**
     * Get singleton instance of the application
     */
    companion object {

        @Volatile
        private var INSTANCE: AURAApplication? = null

        fun getInstance(context: Context): AURAApplication {
            return when (context) {
                is AURAApplication -> context
                else -> {
                    val appContext = context.applicationContext
                    if (appContext is AURAApplication) {
                        appContext
                    } else {
                        throw IllegalStateException("AURAApplication not found in context hierarchy")
                    }
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return getInstance(context).database
        }

        fun getSupabaseClient(context: Context): SupabaseClient {
            return getInstance(context).supabaseClient
        }
    }

    /**
     * Provide WorkManager configuration
     */
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()
    }

    /**
     * Handle low memory situations
     */
    override fun onLowMemory() {
        super.onLowMemory()
        logger.warn("System is low on memory")

        // Clear caches and reduce memory usage
        applicationScope.launch {
            try {
                // Close non-essential database connections temporarily
                database.clearAllTables()
                logger.info("Cleared database tables to free memory")
            } catch (e: Exception) {
                logger.error("Failed to clear database during low memory", e)
            }
        }
    }

    /**
     * Handle trim memory requests
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        logger.info("Memory trim requested with level: $level")

        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                // Release as much memory as possible
                onLowMemory()
            }
            TRIM_MEMORY_RUNNING_LOW -> {
                // Release some memory
                applicationScope.launch {
                    try {
                        // Clear non-critical caches
                        logger.debug("Trimming memory: clearing caches")
                    } catch (e: Exception) {
                        logger.error("Failed to trim memory", e)
                    }
                }
            }
        }
    }

    /**
     * Cleanup resources when app is terminated
     */
    override fun onTerminate() {
        super.onTerminate()
        logger.info("AURA Application is terminating")

        applicationScope.launch {
            try {
                // Close database connections
                database.close()
                logger.debug("Database connections closed")
            } catch (e: Exception) {
                logger.error("Error during application termination", e)
            }
        }
    }
}