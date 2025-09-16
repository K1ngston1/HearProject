package ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.material.*
import androidx.compose.ui.unit.dp
import com.example.ui.UserProfileCard

/**
 * Простий навігаційний об'єкт для Compose Desktop
 */
class Navigator {

    // Стан поточного екрану
    var currentScreen by mutableStateOf<Screen>(Screen.Login)
        private set

    // Перейти на новий екран
    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    @Composable
    fun StartApp() {
        val windowState = rememberWindowState(width = 600.dp, height = 700.dp)
        Window(
            onCloseRequest = { kotlin.system.exitProcess(0) }, // <-- замість ::exitApplication
            title = "Hear Projects",
            state = windowState
        ) {
            MaterialTheme {
                when (currentScreen) {
                    is Screen.Login -> LoginScreen(this@Navigator)
                    is Screen.Dashboard -> DashboardScreen()
                }
            }
        }

    }
}

/**
 * Екрани для навігатора
 */
sealed class Screen {
    object Login : Screen()
    object Dashboard : Screen()
}

/**
 * LoginScreen отримує доступ до Navigator, щоб робити navigateToNextScreen
 */
@Composable
fun LoginScreen(navigator: Navigator) {
    // Твій UserProfileCard можна тут вставити
    UserProfileCard(
        onLoginSuccess = {
            navigator.navigateTo(Screen.Dashboard)
        }
    )
}

/**
 * Простий екран після логіна
 */
@Composable
fun DashboardScreen() {
    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text("Ви успішно увійшли! Це новий екран", style = MaterialTheme.typography.h5)
    }
}
