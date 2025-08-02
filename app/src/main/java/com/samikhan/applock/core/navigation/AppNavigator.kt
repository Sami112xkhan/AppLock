package com.samikhan.applock.core.navigation

import android.util.Log
import androidx.activity.compose.LocalActivity
import com.samikhan.applock.core.biometric.BiometricAuthManager
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.samikhan.applock.AppLockApplication
import com.samikhan.applock.features.appintro.ui.AppIntroScreen
import com.samikhan.applock.features.applist.ui.MainScreen
import com.samikhan.applock.features.lockscreen.ui.PasswordOverlayScreen
import com.samikhan.applock.features.setpassword.ui.SetPasswordScreen
import com.samikhan.applock.features.settings.ui.SettingsScreen

@Composable
fun AppNavHost(navController: NavHostController, startDestination: String) {
    val duration = 400

    val application = LocalContext.current.applicationContext as AppLockApplication

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(duration)) +
                    scaleIn(initialScale = 0.9f, animationSpec = tween(duration))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(duration)) +
                    scaleIn(initialScale = 0.9f, animationSpec = tween(duration))
        },
    ) {
        composable(Screen.AppIntro.route) { AppIntroScreen(navController) }

        composable(Screen.SetPassword.route) { SetPasswordScreen(navController, true) }

        composable(Screen.ChangePassword.route) { SetPasswordScreen(navController, false) }

        composable(Screen.Main.route) { MainScreen(navController) }

        composable(Screen.PasswordOverlay.route) {
            val context = LocalActivity.current as FragmentActivity

            PasswordOverlayScreen(
                showBiometricButton = application.appLockRepository.isBiometricAuthEnabled(),
                fromMainActivity = true,
                onBiometricAuth = {
                    val biometricAuthManager = BiometricAuthManager(context, application.appLockRepository)
                    biometricAuthManager.authenticateForApp(
                        activity = context,
                        packageName = "main_activity",
                        appName = "App Lock",
                        onSuccess = { _ ->
                            navController.navigate(Screen.Main.route) {
                                popUpTo(Screen.PasswordOverlay.route) { inclusive = true }
                            }
                        },
                        onFailure = { _, reason ->
                            Log.w("AppNavigator", "Biometric authentication failed: $reason")
                        },
                        onError = { errorCode, errorMessage ->
                            Log.w("AppNavigator", "Biometric authentication error ($errorCode): $errorMessage")
                        }
                    )
                },
                onAuthSuccess = {
                    // if there is back stack, pop back, otherwise navigate to Main
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.PasswordOverlay.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
    }
}

