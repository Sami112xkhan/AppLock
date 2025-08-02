package com.samikhan.applock.core.biometric

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.samikhan.applock.data.repository.AppLockRepository
import com.samikhan.applock.services.AppLockManager
import java.util.concurrent.Executor

/**
 * Comprehensive Biometric Authentication Manager
 * Handles all biometric operations with proper error handling and state management
 */
class BiometricAuthManager(
    private val context: Context,
    private val appLockRepository: AppLockRepository
) {
    companion object {
        private const val TAG = "BiometricAuthManager"
    }

    // Biometric availability states
    enum class BiometricAvailability {
        AVAILABLE,
        NOT_AVAILABLE,
        NO_HARDWARE,
        NO_ENROLLED_BIOMETRICS,
        NO_PERMISSION
    }

    // Authentication states
    enum class AuthState {
        IDLE,
        AUTHENTICATING,
        SUCCESS,
        FAILED,
        ERROR,
        CANCELLED
    }

    private var currentAuthState = AuthState.IDLE
    private var currentPackageName: String? = null
    private var onAuthSuccess: ((String) -> Unit)? = null
    private var onAuthFailure: ((String, String) -> Unit)? = null
    private var onAuthError: ((Int, String) -> Unit)? = null

    /**
     * Check if biometric authentication is available on this device
     */
    fun checkBiometricAvailability(): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        Log.d(TAG, "Biometric availability check result: $canAuthenticate")

        return when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "Biometric authentication is AVAILABLE")
                BiometricAvailability.AVAILABLE
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.w(TAG, "Biometric authentication NOT_AVAILABLE: No hardware")
                BiometricAvailability.NO_HARDWARE
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.w(TAG, "Biometric authentication NOT_AVAILABLE: Hardware unavailable")
                BiometricAvailability.NOT_AVAILABLE
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.w(TAG, "Biometric authentication NOT_AVAILABLE: No biometrics enrolled")
                BiometricAvailability.NO_ENROLLED_BIOMETRICS
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Log.w(TAG, "Biometric authentication NOT_AVAILABLE: Security update required")
                BiometricAvailability.NOT_AVAILABLE
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                Log.w(TAG, "Biometric authentication NOT_AVAILABLE: Unsupported")
                BiometricAvailability.NOT_AVAILABLE
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                Log.w(TAG, "Biometric authentication NOT_AVAILABLE: Status unknown")
                BiometricAvailability.NOT_AVAILABLE
            }
            else -> {
                Log.w(TAG, "Biometric authentication NOT_AVAILABLE: Unknown error code $canAuthenticate")
                BiometricAvailability.NOT_AVAILABLE
            }
        }
    }

    /**
     * Check if biometric authentication is enabled in app settings
     */
    fun isBiometricEnabled(): Boolean {
        return appLockRepository.isBiometricAuthEnabled()
    }

    /**
     * Check if we should prompt for biometric authentication
     */
    fun shouldPromptForBiometric(): Boolean {
        return isBiometricEnabled() && appLockRepository.shouldPromptForBiometricAuth()
    }

    /**
     * Start biometric authentication for a specific app
     */
    fun authenticateForApp(
        activity: FragmentActivity,
        packageName: String,
        appName: String,
        onSuccess: (String) -> Unit,
        onFailure: (String, String) -> Unit,
        onError: (Int, String) -> Unit
    ) {
        Log.d(TAG, "Starting biometric authentication for $packageName ($appName)")
        
        // Check if biometric is available
        val availability = checkBiometricAvailability()
        Log.d(TAG, "Biometric availability: $availability")
        
        if (availability != BiometricAvailability.AVAILABLE) {
            val errorMessage = when (availability) {
                BiometricAvailability.NO_HARDWARE -> "No biometric hardware available"
                BiometricAvailability.NO_ENROLLED_BIOMETRICS -> "No biometrics enrolled"
                BiometricAvailability.NOT_AVAILABLE -> "Biometric authentication not available"
                BiometricAvailability.NO_PERMISSION -> "Biometric permission not granted"
                else -> "Biometric authentication unavailable"
            }
            Log.w(TAG, "Biometric not available: $errorMessage")
            onError(BiometricPrompt.ERROR_HW_NOT_PRESENT, errorMessage)
            return
        }

        // Check if biometric is enabled in settings
        val isEnabled = isBiometricEnabled()
        Log.d(TAG, "Biometric enabled in settings: $isEnabled")
        
        if (!isEnabled) {
            Log.w(TAG, "Biometric authentication is disabled in app settings")
            onError(BiometricPrompt.ERROR_HW_NOT_PRESENT, "Biometric authentication is disabled")
            return
        }

        // Check if already authenticating
        if (currentAuthState == AuthState.AUTHENTICATING) {
            Log.w(TAG, "Biometric authentication already in progress")
            return
        }

        // Set up authentication
        currentPackageName = packageName
        currentAuthState = AuthState.AUTHENTICATING
        onAuthSuccess = onSuccess
        onAuthFailure = onFailure
        onAuthError = onError

        // Report to AppLockManager
        AppLockManager.reportBiometricAuthStarted()

        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val biometricPrompt = BiometricPrompt(activity, executor, createAuthenticationCallback())

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock $appName")
                .setSubtitle("Use your fingerprint, face, or device PIN to continue")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .setConfirmationRequired(false)
                .build()

            Log.d(TAG, "Starting biometric authentication for $packageName")
            biometricPrompt.authenticate(promptInfo)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting biometric authentication: ${e.message}", e)
            currentAuthState = AuthState.ERROR
            AppLockManager.reportBiometricAuthFinished()
            onError(BiometricPrompt.ERROR_HW_UNAVAILABLE, "Failed to start biometric authentication: ${e.message}")
        }
    }

    /**
     * Create the authentication callback
     */
    private fun createAuthenticationCallback(): BiometricPrompt.AuthenticationCallback {
        return object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.w(TAG, "Biometric authentication error: $errString ($errorCode)")
                
                currentAuthState = AuthState.ERROR
                AppLockManager.reportBiometricAuthFinished()
                
                // Handle specific error codes
                when (errorCode) {
                    BiometricPrompt.ERROR_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        currentAuthState = AuthState.CANCELLED
                        Log.d(TAG, "User cancelled biometric authentication")
                    }
                    BiometricPrompt.ERROR_LOCKOUT -> {
                        Log.w(TAG, "Biometric lockout - too many failed attempts")
                    }
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        Log.w(TAG, "Permanent biometric lockout")
                    }
                    BiometricPrompt.ERROR_HW_NOT_PRESENT -> {
                        Log.w(TAG, "No biometric hardware available")
                    }
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                        Log.w(TAG, "Biometric hardware unavailable")
                    }
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                        Log.w(TAG, "No biometrics enrolled")
                    }
                    BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED -> {
                        Log.w(TAG, "Security update required for biometrics")
                    }
                    BiometricPrompt.ERROR_TIMEOUT -> {
                        Log.w(TAG, "Biometric authentication timeout")
                    }
                    BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> {
                        Log.w(TAG, "Unable to process biometric")
                    }
                    BiometricPrompt.ERROR_USER_CANCELED -> {
                        currentAuthState = AuthState.CANCELLED
                        Log.d(TAG, "User cancelled biometric authentication")
                    }
                    BiometricPrompt.ERROR_VENDOR -> {
                        Log.w(TAG, "Vendor-specific biometric error")
                    }
                }
                
                onAuthError?.invoke(errorCode, errString.toString())
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "Biometric authentication succeeded for $currentPackageName")
                
                currentAuthState = AuthState.SUCCESS
                AppLockManager.reportBiometricAuthFinished()
                
                // Unlock the app
                currentPackageName?.let { packageName ->
                    AppLockManager.temporarilyUnlockAppWithBiometrics(packageName)
                    onAuthSuccess?.invoke(packageName)
                }
                
                // Reset state
                resetState()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.w(TAG, "Biometric authentication failed - not recognized")
                
                currentAuthState = AuthState.FAILED
                
                // Don't reset state here - let user try again
                currentPackageName?.let { packageName ->
                    onAuthFailure?.invoke(packageName, "Biometric not recognized")
                }
            }
        }
    }

    /**
     * Cancel current authentication
     */
    fun cancelAuthentication() {
        if (currentAuthState == AuthState.AUTHENTICATING) {
            Log.d(TAG, "Cancelling biometric authentication")
            currentAuthState = AuthState.CANCELLED
            AppLockManager.reportBiometricAuthFinished()
            resetState()
        }
    }

    /**
     * Check if currently authenticating
     */
    fun isAuthenticating(): Boolean {
        return currentAuthState == AuthState.AUTHENTICATING
    }

    /**
     * Get current authentication state
     */
    fun getAuthState(): AuthState {
        return currentAuthState
    }

    /**
     * Reset internal state
     */
    private fun resetState() {
        currentAuthState = AuthState.IDLE
        currentPackageName = null
        onAuthSuccess = null
        onAuthFailure = null
        onAuthError = null
    }

    /**
     * Get user-friendly error message for error codes
     */
    fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            BiometricPrompt.ERROR_CANCELED -> "Authentication cancelled"
            BiometricPrompt.ERROR_NEGATIVE_BUTTON -> "Authentication cancelled"
            BiometricPrompt.ERROR_LOCKOUT -> "Too many failed attempts. Try again later."
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "Biometric authentication permanently locked"
            BiometricPrompt.ERROR_HW_NOT_PRESENT -> "No biometric hardware available"
            BiometricPrompt.ERROR_HW_UNAVAILABLE -> "Biometric hardware unavailable"
            BiometricPrompt.ERROR_NO_BIOMETRICS -> "No biometrics enrolled on device"
            BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED -> "Security update required"
            BiometricPrompt.ERROR_TIMEOUT -> "Authentication timeout"
            BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> "Unable to process biometric"
            BiometricPrompt.ERROR_USER_CANCELED -> "Authentication cancelled"
            BiometricPrompt.ERROR_VENDOR -> "Biometric authentication error"
            else -> "Unknown biometric error"
        }
    }

    /**
     * Diagnostic method to check biometric status
     */
    fun getDiagnosticInfo(): String {
        val availability = checkBiometricAvailability()
        val isEnabled = isBiometricEnabled()
        val shouldPrompt = shouldPromptForBiometric()
        val currentState = getAuthState()
        
        return """
            Biometric Diagnostic Info:
            - Availability: $availability
            - Enabled in settings: $isEnabled
            - Should prompt: $shouldPrompt
            - Current state: $currentState
            - Context: ${context.packageName}
        """.trimIndent()
    }
} 