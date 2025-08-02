package com.samikhan.applock.features.setpassword.ui

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.samikhan.applock.AppLockApplication
import com.samikhan.applock.core.navigation.Screen
import com.samikhan.applock.core.ui.shapes
import com.samikhan.applock.features.lockscreen.ui.KeypadRow
import com.samikhan.applock.ui.icons.Backspace
import com.samikhan.applock.ui.components.PatternLockView
import com.samikhan.applock.data.repository.LockType

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun SetPasswordScreen(
    navController: NavController,
    isFirstTimeSetup: Boolean
) {
    var passwordState by remember { mutableStateOf("") }
    var confirmPasswordState by remember { mutableStateOf("") }
    var isConfirmationMode by remember { mutableStateOf(false) }
    var selectedLockType by remember { mutableStateOf(LockType.PIN) }
    var showLockTypeSelection by remember { mutableStateOf(isFirstTimeSetup) }
    var currentLockType by remember { mutableStateOf(LockType.PIN) }

    var isVerifyOldPasswordMode by remember { mutableStateOf(!isFirstTimeSetup) }

    var showMismatchError by remember { mutableStateOf(false) }
    var showLengthError by remember { mutableStateOf(false) }
    var showInvalidOldPasswordError by remember { mutableStateOf(false) }
    var showPatternTooShortError by remember { mutableStateOf(false) }
    val maxLength = 6

    val context = LocalContext.current
    val activity = LocalActivity.current as? ComponentActivity
    val appLockRepository = remember {
        (context.applicationContext as? AppLockApplication)?.appLockRepository
    }
    
    // Load current lock type
    LaunchedEffect(appLockRepository) {
        appLockRepository?.let { repo ->
            currentLockType = repo.getLockType()
            if (!isFirstTimeSetup) {
                selectedLockType = currentLockType
            }
        }
    }

    BackHandler {
        if (isFirstTimeSetup) {
            Toast.makeText(context, "Please set a lock to continue", Toast.LENGTH_SHORT).show()
        } else {
            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            } else {
                activity?.finish()
            }
        }
    }

    val fragmentActivity = LocalActivity.current as? androidx.fragment.app.FragmentActivity

    fun launchDeviceCredentialAuth() {
        if (fragmentActivity == null) return
        val executor = ContextCompat.getMainExecutor(context)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate to reset lock")
            .setSubtitle("Use your device PIN, pattern, or password")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        val biometricPrompt = BiometricPrompt(
            fragmentActivity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isVerifyOldPasswordMode = false
                    passwordState = ""
                    confirmPasswordState = ""
                    showInvalidOldPasswordError = false
                }
            })
        biometricPrompt.authenticate(promptInfo)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            showLockTypeSelection -> "Choose Lock Type"
                            isFirstTimeSetup -> "Welcome to App Lock"
                            isVerifyOldPasswordMode -> "Enter Current ${if (currentLockType == LockType.PATTERN) "Pattern" else "PIN"}"
                            isConfirmationMode -> "Confirm ${if (selectedLockType == LockType.PATTERN) "Pattern" else "PIN"}"
                            else -> "Set New ${if (selectedLockType == LockType.PATTERN) "Pattern" else "PIN"}"
                        },
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            if (showLockTypeSelection) {
                // Lock Type Selection Screen
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Choose Your Lock Type",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Select how you want to protect your apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // PIN Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable {
                            selectedLockType = LockType.PIN
                            showLockTypeSelection = false
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        if (selectedLockType == LockType.PIN) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "PIN Lock",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 16.dp))
                        Column {
                            Text(
                                text = "PIN Lock",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Use a 6-digit PIN to secure your apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pattern Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable {
                            selectedLockType = LockType.PATTERN
                            showLockTypeSelection = false
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        if (selectedLockType == LockType.PATTERN) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pattern,
                            contentDescription = "Pattern Lock",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 16.dp))
                        Column {
                            Text(
                                text = "Pattern Lock",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Draw a pattern on a 3x3 grid to secure your apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                return@Column
            }

            if (isFirstTimeSetup && !isConfirmationMode && !isVerifyOldPasswordMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Secure Your Apps",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Please create a ${if (selectedLockType == LockType.PATTERN) "pattern" else "PIN"} to protect your locked apps. This ${if (selectedLockType == LockType.PATTERN) "pattern" else "PIN"} will be required whenever you try to access a locked app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = when {
                        isVerifyOldPasswordMode -> "Enter your current ${if (appLockRepository?.getLockType() == LockType.PATTERN) "pattern" else "PIN"}"
                        isConfirmationMode -> "Confirm your new ${if (selectedLockType == LockType.PATTERN) "pattern" else "PIN"}"
                        else -> "Create a new ${if (selectedLockType == LockType.PATTERN) "pattern" else "PIN"}"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                    tooltip = {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = when {
                                    isVerifyOldPasswordMode -> "Enter your current ${if (currentLockType == LockType.PATTERN) "pattern" else "PIN"} to continue"
                                    isConfirmationMode -> "Please enter the same ${if (selectedLockType == LockType.PATTERN) "pattern" else "PIN"} again to confirm"
                                    else -> "Create a ${if (selectedLockType == LockType.PATTERN) "pattern (minimum 4 points)" else "6-digit PIN"} to protect your apps"
                                },
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Information",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (showMismatchError) {
                Text(
                    text = "${if (selectedLockType == LockType.PATTERN) "Patterns" else "PINs"} don't match. Try again.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }
            if (showLengthError) {
                Text(
                    text = if (selectedLockType == LockType.PATTERN) "Pattern must have at least 4 points" else "PIN must be 6 digits",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }
            if (showInvalidOldPasswordError) {
                Text(
                    text = "Incorrect ${if (currentLockType == LockType.PATTERN) "pattern" else "PIN"}. Please try again.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }
            if (showPatternTooShortError) {
                Text(
                    text = "Pattern must have at least 4 points",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }

            if (selectedLockType == LockType.PIN) {
                // PIN Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 20.dp)
                ) {
                    val currentPassword = when {
                        isVerifyOldPasswordMode -> passwordState
                        isConfirmationMode -> confirmPasswordState
                        else -> passwordState
                    }
                    repeat(maxLength) { index ->
                        val filled = index < currentPassword.length
                        val isNext = index == currentPassword.length && index < maxLength
                        val indicatorState = remember(filled, isNext) {
                            when {
                                filled -> "filled"; isNext -> "next"; else -> "empty"
                            }
                        }
                        val scale by animateFloatAsState(
                            targetValue = if (filled) 1.2f else if (isNext) 1.1f else 1.0f,
                            animationSpec = tween(
                                durationMillis = 100,
                                easing = FastOutSlowInEasing
                            ),
                            label = "indicatorScale"
                        )
                        AnimatedContent(
                            targetState = indicatorState,
                            transitionSpec = {
                                fadeIn(tween(100)) togetherWith fadeOut(tween(50))
                            },
                            label = "indicatorAnimation"
                        ) { state ->
                            val shape = when (state) {
                                "filled" -> shapes[index % shapes.size].toShape()
                                "next" -> MaterialShapes.Diamond.toShape()
                                else -> MaterialShapes.Circle.toShape()
                            }
                            val color = when (state) {
                                "filled" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .size(24.dp)
                                    .background(color = color, shape = shape)
                            )
                        }
                    }
                }
            }

            Text(
                text = when {
                    isVerifyOldPasswordMode -> "Enter your current ${if (appLockRepository?.getLockType() == LockType.PATTERN) "pattern" else "PIN"}"
                    isConfirmationMode -> "Re-enter your new ${if (selectedLockType == LockType.PATTERN) "pattern" else "PIN"} to confirm"
                    else -> "Enter a ${if (selectedLockType == LockType.PATTERN) "pattern (minimum 4 points)" else "6-digit PIN"}"
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.alpha(0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            if (isVerifyOldPasswordMode) {
                TextButton(onClick = { launchDeviceCredentialAuth() }) {
                    Text("Reset using device password")
                }
            }

            if (isVerifyOldPasswordMode || isConfirmationMode) {
                TextButton(
                    onClick = {
                        if (isVerifyOldPasswordMode) {
                            if (navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            } else {
                                activity?.finish()
                            }
                        } else {
                            isConfirmationMode = false
                            if (!isFirstTimeSetup) {
                                isVerifyOldPasswordMode = true
                            }
                        }
                        // Reset states
                        passwordState = ""
                        confirmPasswordState = ""
                        showMismatchError = false
                        showLengthError = false
                        showInvalidOldPasswordError = false
                        showPatternTooShortError = false
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(if (isVerifyOldPasswordMode) "Cancel" else "Start Over")
                }
            }

            if (selectedLockType == LockType.PATTERN || (isVerifyOldPasswordMode && currentLockType == LockType.PATTERN)) {
                // Pattern Lock View
                PatternLockView(
                    onPatternComplete = { pattern ->
                        // Handle verification mode
                        if (isVerifyOldPasswordMode && currentLockType == LockType.PATTERN) {
                            android.util.Log.d("SetPasswordScreen", "Verifying old pattern: '$pattern'")
                            if (appLockRepository!!.validatePassword(pattern)) {
                                android.util.Log.d("SetPasswordScreen", "Old pattern verified successfully")
                                isVerifyOldPasswordMode = false
                                passwordState = ""
                                showInvalidOldPasswordError = false
                                // Show lock type selection after verifying old password
                                showLockTypeSelection = true
                            } else {
                                android.util.Log.d("SetPasswordScreen", "Old pattern verification failed")
                                showInvalidOldPasswordError = true
                                passwordState = ""
                            }
                            return@PatternLockView
                        }

                        // Handle pattern setting mode
                        val currentActivePassword = when {
                            isConfirmationMode -> confirmPasswordState
                            else -> passwordState
                        }
                        val updatePassword: (String) -> Unit = when {
                            isConfirmationMode -> { newPass -> confirmPasswordState = newPass }
                            else -> { newPass -> passwordState = newPass }
                        }

                        // Validate pattern length
                        val patternPoints = pattern.split(",").filter { it.isNotEmpty() }
                        if (patternPoints.size < 4) {
                            showPatternTooShortError = true
                            return@PatternLockView
                        }

                        showPatternTooShortError = false
                        updatePassword(pattern)

                        // Auto-proceed if pattern is valid
                        when {
                            !isConfirmationMode -> {
                                android.util.Log.d("SetPasswordScreen", "Setting initial pattern: '$pattern'")
                                isConfirmationMode = true
                                showLengthError = false
                            }
                            else -> { // Confirmation mode
                                android.util.Log.d("SetPasswordScreen", "Confirming pattern - Original: '$passwordState', Confirmation: '$pattern'")
                                if (passwordState == pattern) {
                                    android.util.Log.d("SetPasswordScreen", "Pattern confirmed successfully")
                                    appLockRepository?.setPattern(pattern)
                                    appLockRepository?.setLockType(LockType.PATTERN)
                                    Toast.makeText(
                                        context,
                                        "Pattern set successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Navigate to Main screen after setting pattern
                                    navController.navigate(Screen.Main.route) {
                                        popUpTo(Screen.SetPassword.route) {
                                            inclusive = true
                                        }
                                        if (isFirstTimeSetup) {
                                            popUpTo(Screen.AppIntro.route) {
                                                inclusive = true
                                            }
                                        }
                                    }
                                } else {
                                    android.util.Log.d("SetPasswordScreen", "Pattern confirmation failed - patterns don't match")
                                    showMismatchError = true
                                    confirmPasswordState = ""
                                }
                            }
                        }
                    },
                    isEnabled = !showPatternTooShortError && !showInvalidOldPasswordError
                )
            } else {
                // PIN Keypad
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val onKeyClick: (String) -> Unit = { key ->
                        val currentActivePassword = when {
                            isVerifyOldPasswordMode -> passwordState
                            isConfirmationMode -> confirmPasswordState
                            else -> passwordState
                        }
                        val updatePassword: (String) -> Unit = when {
                            isVerifyOldPasswordMode -> { newPass -> passwordState = newPass }
                            isConfirmationMode -> { newPass -> confirmPasswordState = newPass }
                            else -> { newPass -> passwordState = newPass }
                        }

                        when (key) {
                            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" -> {
                                if (currentActivePassword.length < maxLength) {
                                    updatePassword(currentActivePassword + key)
                                }
                            }

                            "backspace" -> {
                                if (currentActivePassword.isNotEmpty()) {
                                    updatePassword(currentActivePassword.dropLast(1))
                                }
                                showMismatchError = false
                                showLengthError = false
                                showInvalidOldPasswordError = false
                            }

                            "proceed" -> {
                                when {
                                    isVerifyOldPasswordMode -> {
                                        if (currentLockType == LockType.PIN && passwordState.length == maxLength) {
                                            if (appLockRepository!!.validatePassword(passwordState)) {
                                                isVerifyOldPasswordMode = false
                                                passwordState = ""
                                                showInvalidOldPasswordError = false
                                                // Show lock type selection after verifying old password
                                                showLockTypeSelection = true
                                            } else {
                                                showInvalidOldPasswordError = true
                                                passwordState = ""
                                            }
                                        } else if (currentLockType == LockType.PIN) {
                                            showLengthError = true
                                        }
                                    }

                                    !isConfirmationMode -> {
                                        if (passwordState.length == maxLength) {
                                            isConfirmationMode = true
                                            showLengthError = false
                                        } else {
                                            showLengthError = true
                                        }
                                    }

                                    else -> { // Confirmation mode
                                        if (confirmPasswordState.length == maxLength) {
                                            if (passwordState == confirmPasswordState) {
                                                appLockRepository?.setPassword(passwordState)
                                                appLockRepository?.setLockType(LockType.PIN)
                                                Toast.makeText(
                                                    context,
                                                    "PIN set successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                // Navigate to Main screen after setting password
                                                navController.navigate(Screen.Main.route) {
                                                    popUpTo(Screen.SetPassword.route) {
                                                        inclusive = true
                                                    }
                                                    if (isFirstTimeSetup) {
                                                        popUpTo(Screen.AppIntro.route) {
                                                            inclusive = true
                                                        }
                                                    }
                                                }
                                            } else {
                                                showMismatchError = true
                                                confirmPasswordState = ""
                                            }
                                        } else {
                                            showLengthError = true
                                        }
                                    }
                                }
                            }
                        }
                    }

                    KeypadRow(
                        keys = listOf("1", "2", "3"),
                        onKeyClick = onKeyClick
                    )
                    KeypadRow(
                        keys = listOf("4", "5", "6"),
                        onKeyClick = onKeyClick
                    )
                    KeypadRow(
                        keys = listOf("7", "8", "9"),
                        onKeyClick = onKeyClick
                    )
                    KeypadRow(
                        keys = listOf("backspace", "0", "proceed"),
                        icons = listOf(
                            Backspace,
                            null,
                            if (isConfirmationMode || isVerifyOldPasswordMode) Icons.Default.Check else Icons.AutoMirrored.Rounded.KeyboardArrowRight
                        ),
                        onKeyClick = onKeyClick
                    )
                }
            }
        }
    }
}
