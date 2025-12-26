package arg.adegtiarev.ciclismo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import arg.adegtiarev.ciclismo.ui.navigation.Navigation
import arg.adegtiarev.ciclismo.ui.theme.CiclismoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CiclismoTheme {
                Navigation()
            }
        }
    }
}