package app.umaia.android.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import app.umaia.android.data.auth.AuthState
import app.umaia.android.data.local.GamePreferences
import app.umaia.android.ui.screens.auth.AuthViewModel
import app.umaia.android.ui.screens.auth.LoginScreen
import app.umaia.android.ui.screens.companycode.CompanyCodeScreen
import app.umaia.android.ui.screens.onboarding.OnboardingScreen
import app.umaia.android.ui.screens.oracle.OracleScreen
import app.umaia.android.ui.screens.profile.ProfileScreen
import app.umaia.android.ui.screens.steps.StepsScreen
import app.umaia.android.ui.theme.Gold
import app.umaia.android.ui.theme.TC

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    gamePrefs: GamePreferences,
    authViewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: app.umaia.android.ui.screens.profile.ProfileViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()

    val startDest = when {
        !gamePrefs.onboardingDone -> Screen.Onboarding.route
        authState is AuthState.Authenticated -> Screen.CompanyGate.route
        else -> Screen.Login.route
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showSeer = profileState.profile?.isCompanyMember == true

    Scaffold(
        bottomBar = {
            if (currentRoute in BOTTOM_NAV_ROUTES) {
                BottomNavBar(navController, showSeer = showSeer)
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
                    navController.navigate(Screen.CompanyGate.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                })
            }

            // Loading bridge: refresh the profile, then route to CompanyCode
            // (when the user hasn't picked a cohort yet) or Steps.
            composable(Screen.CompanyGate.route) {
                CompanyGateRoute(
                    gamePrefs = gamePrefs,
                    authService = authViewModel.authService,
                    profileViewModel = profileViewModel,
                    onShowCompanyCode = {
                        navController.navigate(Screen.CompanyCode.route) {
                            popUpTo(Screen.CompanyGate.route) { inclusive = true }
                        }
                    },
                    onShowSteps = {
                        navController.navigate(Screen.Steps.route) {
                            popUpTo(Screen.CompanyGate.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.CompanyCode.route) {
                CompanyCodeScreen(
                    onDone = {
                        // First-time gate (came from CompanyGate or Login —
                        // those got popped, so previous is empty / outside
                        // BOTTOM_NAV) → forward to Steps. In-app cohort
                        // change (came from Profile) → pop back to Profile.
                        val cameFromProfile =
                            navController.previousBackStackEntry?.destination?.route == Screen.Profile.route
                        if (cameFromProfile) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Screen.Steps.route) {
                                popUpTo(Screen.CompanyCode.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.Steps.route) { StepsScreen() }

            composable(Screen.Oracle.route) {
                OracleScreen(onComplete = {
                    navController.navigate(Screen.Steps.route) {
                        popUpTo(Screen.Oracle.route) { inclusive = true }
                    }
                })
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onChangeCohort = { navController.navigate(Screen.CompanyCode.route) }
                )
            }
        }
    }

    // Auth-gate navigation — when auth state flips, snap to the right top-level.
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
                    navController.navigate(Screen.CompanyGate.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun CompanyGateRoute(
    gamePrefs: GamePreferences,
    authService: app.umaia.android.data.auth.AuthService,
    profileViewModel: app.umaia.android.ui.screens.profile.ProfileViewModel,
    onShowCompanyCode: () -> Unit,
    onShowSteps: () -> Unit
) {
    LaunchedEffect(Unit) { profileViewModel.load() }
    val state by profileViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.profile) {
        val p = state.profile ?: return@LaunchedEffect
        // Defence in depth: if `state.profile` is for a different user than
        // the currently-authenticated one (a stale cache from before
        // sign-out — the root cause is now fixed in AuthService.signOut()
        // calling profileRepository.clearCache(), but we keep the guard so
        // any future leak of the same shape can't misroute), wait for the
        // freshly-loaded profile and ignore this emission.
        val authUid = authService.currentUserId ?: return@LaunchedEffect
        if (p.userId != authUid) return@LaunchedEffect
        // Already in a cohort, OR user has explicitly skipped — go to Steps.
        if (p.companyCode != null || gamePrefs.isCompanyChoiceMade(p.userId)) {
            onShowSteps()
        } else {
            onShowCompanyCode()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(TC.bg), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Gold)
    }
}
