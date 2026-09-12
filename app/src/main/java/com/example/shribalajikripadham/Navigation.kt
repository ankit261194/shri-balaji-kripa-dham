package com.example.shribalajikripadham

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.shribalajikripadham.theme.SacredTheme
import com.example.shribalajikripadham.ui.admin.AdminDashboardScreen
import com.example.shribalajikripadham.ui.home.HomeScreen
import com.example.shribalajikripadham.ui.info.AshramInfoScreen
import com.example.shribalajikripadham.ui.splash.SplashScreen
import com.example.shribalajikripadham.ui.token.FaceTokenRegistrationScreen
import com.example.shribalajikripadham.ui.token.TokenRegistrationScreen
import com.example.shribalajikripadham.ui.yatra.BalajiYatraScreen
import com.example.shribalajikripadham.ui.yatra.YatraExpenseScreen

enum class AppScreen {
    SPLASH,
    HOME,
    TOKEN,
    FACE_TOKEN,
    YATRA,
    YATRA_EXPENSES,
    ADMIN,
    ASHRAM_INFO,
    PARCHAS
}

@Composable
fun MainNavigation(
    currentTheme: SacredTheme = SacredTheme.ROYAL_MAROON,
    onThemeChanged: (SacredTheme) -> Unit = {}
) {
    val context = LocalContext.current
    var isHindi by remember { mutableStateOf(true) }

    // Persistent Darbar Entrance State: Returning users go directly to Home Screen!
    val prefs = remember { context.getSharedPreferences("shri_balaji_app_prefs", Context.MODE_PRIVATE) }
    val initialScreen = remember {
        val hasEntered = prefs.getBoolean("has_entered_darbar", false)
        if (hasEntered) AppScreen.HOME else AppScreen.SPLASH
    }

    var currentScreen by remember { mutableStateOf(initialScreen) }
    val backStack = remember { mutableStateListOf<AppScreen>() }

    fun navigateTo(screen: AppScreen) {
        backStack.add(currentScreen)
        currentScreen = screen
    }

    fun navigateBack() {
        if (backStack.isNotEmpty()) {
            currentScreen = backStack.removeAt(backStack.size - 1)
        }
    }

    BackHandler(enabled = backStack.isNotEmpty()) {
        navigateBack()
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition",
        modifier = Modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            AppScreen.SPLASH -> SplashScreen(
                onEnterDarbar = {
                    try {
                        prefs.edit().putBoolean("has_entered_darbar", true).apply()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    backStack.clear()
                    currentScreen = AppScreen.HOME
                },
                isHindi = isHindi,
                onToggleLanguage = { isHindi = !isHindi }
            )

            AppScreen.HOME -> HomeScreen(
                isHindi = isHindi,
                currentTheme = currentTheme,
                onThemeChanged = onThemeChanged,
                onNavigateToToken = { navigateTo(AppScreen.TOKEN) },
                onNavigateToFaceToken = { navigateTo(AppScreen.FACE_TOKEN) },
                onNavigateToYatra = { navigateTo(AppScreen.YATRA) },
                onNavigateToInfo = { navigateTo(AppScreen.ASHRAM_INFO) },
                onNavigateToAdmin = { navigateTo(AppScreen.ADMIN) },
                onNavigateToParchas = { navigateTo(AppScreen.PARCHAS) },
                onToggleLanguage = { isHindi = !isHindi }
            )

            AppScreen.FACE_TOKEN -> FaceTokenRegistrationScreen(
                isHindi = isHindi,
                onBack = { navigateBack() },
                onNavigateToManualForm = {
                    navigateBack()
                    navigateTo(AppScreen.TOKEN)
                }
            )

            AppScreen.TOKEN -> TokenRegistrationScreen(
                isHindi = isHindi,
                onBack = { navigateBack() },
                onNavigateToFaceToken = { navigateTo(AppScreen.FACE_TOKEN) }
            )

            AppScreen.YATRA -> BalajiYatraScreen(
                isHindi = isHindi,
                onBack = { navigateBack() },
                onNavigateToExpenses = { navigateTo(AppScreen.YATRA_EXPENSES) }
            )

            AppScreen.YATRA_EXPENSES -> YatraExpenseScreen(
                isHindi = isHindi,
                onBack = { navigateBack() }
            )

            AppScreen.ADMIN -> AdminDashboardScreen(
                isHindi = isHindi,
                onBack = { navigateBack() }
            )

            AppScreen.ASHRAM_INFO -> AshramInfoScreen(
                isHindi = isHindi,
                onBack = { navigateBack() }
            )

            AppScreen.PARCHAS -> com.example.shribalajikripadham.ui.parcha.SacredParchasScreen(
                isHindi = isHindi,
                currentAdmin = null,
                onBack = { navigateBack() }
            )
        }
    }
}
