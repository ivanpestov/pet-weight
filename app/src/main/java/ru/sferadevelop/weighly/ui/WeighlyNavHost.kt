package ru.sferadevelop.weighly.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import ru.sferadevelop.weighly.ui.form.FormScreen
import ru.sferadevelop.weighly.ui.history.HistoryScreen

/** The History: the start destination, so launching the app lands straight on it. */
@Serializable
data object HistoryRoute

/**
 * The entry form. [epochDay] absent means creating a Record; a date means editing the Record
 * stored on it.
 */
@Serializable
data class FormRoute(val epochDay: Long? = null)

@Composable
fun WeighlyNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HistoryRoute) {
        composable<HistoryRoute> {
            HistoryScreen(onAddRecord = { navController.navigate(FormRoute()) })
        }
        composable<FormRoute> {
            // Save and Cancel leave the Form the same way, which is what the system back does too.
            FormScreen(onDone = { navController.popBackStack() })
        }
    }
}
