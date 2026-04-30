package app.umaia.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import app.umaia.android.ui.strings.AppStrings
import app.umaia.android.ui.strings.LocalStrings
import app.umaia.android.ui.theme.*

private data class NavItem(
    val screen: Screen,
    val label: (AppStrings) -> String,
    val icon: ImageVector
)

@Composable
fun BottomNavBar(navController: NavController, showSeer: Boolean) {
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val strings = LocalStrings.current

    // Seer tab is only rendered for company-cohort members. Public-pool users
    // see Walk + UMAIA only — keeps the visible feature set minimal for the
    // wider audience.
    val items = buildList {
        add(NavItem(Screen.Steps, { it.tabSteps }, Icons.Filled.DirectionsWalk))
        if (showSeer) add(NavItem(Screen.Oracle, { it.tabSeer }, Icons.Filled.AutoAwesome))
        add(NavItem(Screen.Profile, { it.tabUmaia }, Icons.Filled.Star))
    }

    NavigationBar(containerColor = TC.bg, contentColor = Gold) {
        items.forEach { item ->
            NavigationBarItem(
                selected = current == item.screen.route,
                onClick = {
                    if (current != item.screen.route) {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Steps.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(item.label(strings)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Gold,
                    selectedTextColor   = Gold,
                    unselectedIconColor = TC.muted,
                    unselectedTextColor = TC.muted,
                    indicatorColor      = TC.bg
                )
            )
        }
    }
}
