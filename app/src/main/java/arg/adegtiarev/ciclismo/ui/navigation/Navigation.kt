package arg.adegtiarev.ciclismo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import arg.adegtiarev.ciclismo.ui.screen.TrackingScreen


sealed class Screen(val route: String) {
    object Tracking : Screen("tracking")
    object History : Screen("history")
}

@Composable
fun Navigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Tracking.route
    ) {
        composable(Screen.Tracking.route) {
            TrackingScreen()
        }
        composable(Screen.History.route) {
            //HistoryScreen() // Здесь будет HistoryScreen в нем будет общая статистика по всем поездкам + список всех поездок
        }
    }
}