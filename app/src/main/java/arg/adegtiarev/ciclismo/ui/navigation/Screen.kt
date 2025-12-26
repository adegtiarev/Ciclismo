package arg.adegtiarev.ciclismo.ui.navigation

sealed class Screen(val route: String) {
    object Tracking : Screen("tracking")
    object History : Screen("history")
}