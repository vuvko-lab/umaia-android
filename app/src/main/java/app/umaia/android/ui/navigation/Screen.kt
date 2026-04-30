package app.umaia.android.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding  : Screen("onboarding")
    object Login       : Screen("login")
    object CompanyGate : Screen("company_gate")    // Loading state while we decide CompanyCode vs Steps.
    object CompanyCode : Screen("company_code")
    object Steps       : Screen("steps")           // Walk tab — new home.
    object Oracle      : Screen("oracle")          // Seer tab (cohort-only).
    object Profile     : Screen("profile")         // UMAIA tab.
}

/** Routes that show the bottom navigation bar. The Oracle tab is conditionally
 *  visible — see BottomNavBar — but its route still belongs to the bottom-nav
 *  family so that navigating to it from a deep link doesn't steal the bar. */
val BOTTOM_NAV_ROUTES = listOf(
    Screen.Steps.route,
    Screen.Oracle.route,
    Screen.Profile.route
)
