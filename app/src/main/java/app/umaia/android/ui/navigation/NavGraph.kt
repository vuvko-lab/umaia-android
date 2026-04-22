package app.umaia.android.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import app.umaia.android.data.auth.AuthState
import app.umaia.android.data.local.GamePreferences
import app.umaia.android.ui.screens.auth.AuthViewModel
import app.umaia.android.ui.screens.auth.LoginScreen
import app.umaia.android.ui.screens.dashboard.DashboardScreen
import app.umaia.android.ui.screens.nutrition.NutritionScreen
import app.umaia.android.ui.screens.onboarding.OnboardingScreen
import app.umaia.android.ui.screens.oracle.OracleScreen
import app.umaia.android.ui.screens.profile.ProfileScreen
import app.umaia.android.ui.screens.steps.StepsScreen
import app.umaia.android.ui.theme.TC
import javax.inject.Inject

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    gamePrefs: GamePreferences,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    val startDest = when {
        !gamePrefs.onboardingDone -> Screen.Onboarding.route
        authState is AuthState.Authenticated -> Screen.Dashboard.route
        else -> Screen.Login.route
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in BOTTOM_NAV_ROUTES) {
                BottomNavBar(navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier
                .fillMaxSize()
                .background(TC.bg)
                .padding(padding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onDone = {
                    gamePrefs.onboardingDone = true
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                })
            }

            composable(Screen.Login.route) {
                LoginScreen(onLoggedIn = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                })
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToOracle = { navController.navigate(Screen.Oracle.route) }
                )
            }

            composable(Screen.Steps.route)     { StepsScreen() }
            composable(Screen.Nutrition.route) { NutritionScreen() }

            composable(Screen.Oracle.route) {
                OracleScreen(onComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Oracle.route) { inclusive = true }
                    }
                })
            }

            composable(Screen.Profile.route) { ProfileScreen() }
        }
    }

    // Auth-gate navigation — mirrors iOS AuthGatedView
    LaunchedEffect(authState) {
        if (!gamePrefs.onboardingDone) return@LaunchedEffect
        when (authState) {
            is AuthState.Unauthenticated -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.Authenticated -> {
                val route = navController.currentDestination?.route
                if (route == Screen.Login.route || route == null) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {}
        }
    }
}
