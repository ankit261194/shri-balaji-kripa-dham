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
import com.example.shribalajikripadham.ui.panchang.PanchangScreen
import com.example.shribalajikripadham.ui.granth.SacredGranthScreen

enum class AppScreen {
    SPLASH,
    HOME,
    TOKEN,
    FACE_TOKEN,
    YATRA,
    YATRA_EXPENSES,
    ADMIN,
    ASHRAM_INFO,
    PARCHAS,
    LIVE_DARBAR,
    HALL_DISPLAY,
    PANCHANG,
    SACRED_GRANTH
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

    val repository = remember { com.example.shribalajikripadham.data.repository.AshramRepository(context) }
    var settings by remember { mutableStateOf(com.example.shribalajikripadham.data.model.AshramSettings()) }
    LaunchedEffect(currentScreen) {
        settings = repository.getSettings()
    }

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
                onNavigateToYatra = {
                    if (settings.isYatraServiceEnabled) {
                        navigateTo(AppScreen.YATRA)
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            if (isHindi) "यात्रा व दूरी सेवा व्यवस्थापक द्वारा अस्थायी रूप से बंद है।" else "Yatra service is currently disabled by Admin.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onNavigateToInfo = { navigateTo(AppScreen.ASHRAM_INFO) },
                onNavigateToAdmin = { navigateTo(AppScreen.ADMIN) },
                onNavigateToParchas = { navigateTo(AppScreen.PARCHAS) },
                onNavigateToLiveDarbar = { navigateTo(AppScreen.LIVE_DARBAR) },
                onNavigateToPanchang = { navigateTo(AppScreen.PANCHANG) },
                onNavigateToSacredGranth = { navigateTo(AppScreen.SACRED_GRANTH) },
                onNavigateToYatraExpenses = {
                    if (settings.isYatraServiceEnabled && settings.canDevoteeViewYatraDiary) {
                        navigateTo(AppScreen.YATRA_EXPENSES)
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            if (isHindi) "यात्रा खर्च डायरी सेवा बंद है।" else "Yatra Expense Diary is disabled.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
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

            AppScreen.YATRA -> {
                if (settings.isYatraServiceEnabled) {
                    BalajiYatraScreen(
                        isHindi = isHindi,
                        onBack = { navigateBack() },
                        onNavigateToExpenses = { navigateTo(AppScreen.YATRA_EXPENSES) }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        android.widget.Toast.makeText(
                            context,
                            if (isHindi) "यात्रा सेवा वर्तमान में बंद है।" else "Yatra service is disabled.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        navigateBack()
                    }
                }
            }

            AppScreen.YATRA_EXPENSES -> {
                if (settings.isYatraServiceEnabled && settings.canDevoteeViewYatraDiary) {
                    YatraExpenseScreen(
                        isHindi = isHindi,
                        onBack = { navigateBack() }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navigateBack()
                    }
                }
            }

            AppScreen.ADMIN -> AdminDashboardScreen(
                isHindi = isHindi,
                onBack = { navigateBack() },
                onNavigateToHallDisplay = { navigateTo(AppScreen.HALL_DISPLAY) }
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

            AppScreen.LIVE_DARBAR -> com.example.shribalajikripadham.ui.live.LiveDarbarAndBhajanScreen(
                isHindi = isHindi,
                onBack = { navigateBack() }
            )

            AppScreen.HALL_DISPLAY -> com.example.shribalajikripadham.ui.tv.AshramHallDisplayScreen(
                isHindi = isHindi,
                onBack = { navigateBack() }
            )

            AppScreen.PANCHANG -> PanchangScreen(
                isHindi = isHindi,
                onBack = { navigateBack() }
            )

            AppScreen.SACRED_GRANTH -> SacredGranthScreen(
                isHindi = isHindi,
                onBack = { navigateBack() }
            )
        }
    }
}
