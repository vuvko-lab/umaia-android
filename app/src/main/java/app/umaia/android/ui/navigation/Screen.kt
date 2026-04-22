package app.umaia.android.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Login      : Screen("login")
    object Dashboard  : Screen("dashboard")
    object Steps      : Screen("steps")
    object Nutrition  : Screen("nutrition")
    object Oracle     : Screen("oracle")
    object Profile    : Screen("profile")
}

val BOTTOM_NAV_ROUTES = listOf(
    Screen.Dashboard.route,
    Screen.Steps.route,
    Screen.Nutrition.route,
    Screen.Oracle.route,
    Screen.Profile.route
)
