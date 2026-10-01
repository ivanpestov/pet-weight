package ru.sferadevelop.weighly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.sferadevelop.weighly.ui.WeighlyNavHost
import ru.sferadevelop.weighly.ui.theme.WeighlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeighlyTheme {
                WeighlyNavHost()
            }
        }
    }
}
