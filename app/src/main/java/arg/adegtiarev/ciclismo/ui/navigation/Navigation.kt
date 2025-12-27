package arg.adegtiarev.ciclismo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import arg.adegtiarev.ciclismo.ui.screen.detail.RideDetailScreen
import arg.adegtiarev.ciclismo.ui.screen.home.HomeScreen
import arg.adegtiarev.ciclismo.ui.screen.tracking.TrackingScreen


sealed class Screen(val route: String) {
    object Tracking : Screen("tracking")
    object Home : Screen("home")
    object Detail : Screen("detail/{rideId}") {
        fun createRoute(rideId: Long) = "detail/$rideId"
    }
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
                navController = navController,
                onNavigateToDetail = { rideId ->
                    // При переходе с трекинга на детали, убираем трекинг из стека, 
                    // чтобы кнопка "Назад" возвращала на Главный экран
                    navController.navigate(Screen.Detail.createRoute(rideId)) {
                        popUpTo(Screen.Home.route) {
                            inclusive = false
                        }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTracking = {
                    navController.navigate(Screen.Tracking.route)
                },
                onNavigateToDetail = { rideId ->
                    navController.navigate(Screen.Detail.createRoute(rideId))
                }
            )
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("rideId") { type = NavType.LongType })
        ) {
            RideDetailScreen(
                navController = navController
            )
        }
    }
}
