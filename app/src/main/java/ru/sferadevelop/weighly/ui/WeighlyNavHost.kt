package ru.sferadevelop.weighly.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import ru.sferadevelop.weighly.ui.history.HistoryScreen

/** The History: the start destination, so launching the app lands straight on it. */
@Serializable
data object HistoryRoute

@Composable
fun WeighlyNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HistoryRoute) {
        composable<HistoryRoute> {
            // The entry form, and with it the add button's destination, arrives next.
            HistoryScreen(onAddRecord = { })
        }
    }
}
