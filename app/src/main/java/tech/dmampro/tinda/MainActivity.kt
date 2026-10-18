package tech.dmampro.tinda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import tech.dmampro.tinda.navigation.TindaNavGraph
import tech.dmampro.tinda.theme.TindaTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Requis : edge-to-edge imposé à partir de targetSdk 35.
        setContent {
            TindaTheme {
                TindaNavGraph()
            }
        }
    }
}
