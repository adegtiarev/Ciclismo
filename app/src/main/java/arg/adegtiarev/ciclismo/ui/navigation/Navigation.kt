package arg.adegtiarev.ciclismo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import arg.adegtiarev.ciclismo.ui.screen.home.HomeScreen
import arg.adegtiarev.ciclismo.ui.screen.tracking.TrackingScreen


sealed class Screen(val route: String) {
    object Tracking : Screen("tracking")
    object Home : Screen("home")
}

@Composable
fun Navigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Tracking.route) {
            TrackingScreen(
                navController = navController
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTracking = {
                    navController.navigate(Screen.Tracking.route)
                }
            )
        }
    }
}
