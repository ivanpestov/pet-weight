package ru.sferadevelop.weighly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import ru.sferadevelop.weighly.ui.theme.WeighlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeighlyTheme {
                // The History screen lands here; until then the app is a bare themed surface.
                Surface(modifier = Modifier.fillMaxSize()) { }
            }
        }
    }
}
