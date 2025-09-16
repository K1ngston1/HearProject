package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.material.*
import service.KeyStorageService
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
        val windowState = rememberWindowState(
            width = 600.dp,
            height = 700.dp,
            placement = androidx.compose.ui.window.WindowPlacement.Floating,
            position = androidx.compose.ui.window.WindowPosition.Aligned(Alignment.Center)
        )

        Window(
            onCloseRequest = { kotlin.system.exitProcess(0) },
            title = "Hear Projects",
            state = windowState
        ) {
            MaterialTheme {
                when (val screen = currentScreen) {
                    is Screen.Login -> LoginScreen(this@Navigator)
                    is Screen.SaveKey -> {
                        SaveKeyScreen(
                            navigator = this@Navigator,
                            privateKey = screen.privateKey,
                            password = screen.password
                        )
                    }
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
    data class SaveKey(val privateKey: String, val password: String) : Screen()
    object Dashboard : Screen()
}

/**
 * LoginScreen отримує доступ до Navigator, щоб робити navigateToNextScreen
 */
@Composable
fun LoginScreen(navigator: Navigator) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        UserProfileCard(
            onLoginSuccess = { privateKey, password ->
                navigator.navigateTo(Screen.SaveKey(privateKey, password))
            }
        )
    }
}

/**
 * Екран збереження ключа
 */
@Composable
fun SaveKeyScreen(navigator: Navigator, privateKey: String, password: String) {
    var selectedDrive by remember { mutableStateOf<File?>(null) }
    var saveInProgress by remember { mutableStateOf(false) }
    var saveResult by remember { mutableStateOf<String?>(null) }

    val availableDrives = remember {
        File.listRoots().filter { it.isDirectory && it.canWrite() }
    }

    val keyStorageService = remember { KeyStorageService() }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp,
            modifier = Modifier
                .width(400.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "💾 Збереження захищеного ключа",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    "Оберіть флешку для збереження зашифрованого приватного ключа:",
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Список доступних дисків
                availableDrives.forEach { drive ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDrive = drive }
                            .padding(8.dp)
                            .background(
                                if (selectedDrive == drive) Color.LightGray.copy(alpha = 0.3f)
                                else Color.Transparent
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDrive == drive,
                            onClick = { selectedDrive = drive }
                        )
                        Text(
                            "${drive.absolutePath} (${drive.freeSpace / (1024 * 1024 * 1024)} GB вільний)",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (saveInProgress) {
                    CircularProgressIndicator()
                }

                saveResult?.let { result ->
                    Text(
                        result,
                        color = if (result.contains("успішно")) Color.Green else Color.Red,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { navigator.navigateTo(Screen.Dashboard) },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray)
                    ) {
                        Text("Скасувати")
                    }

                    Button(
                        onClick = {
                            selectedDrive?.let { drive ->
                                saveInProgress = true
                                saveResult = null

                                coroutineScope.launch {
                                    // Викликаємо логіку збереження ключа в фоновому потоці
                                    val keyFile = File(drive, "encrypted_private_key.bin")
                                    val success = withContext(Dispatchers.IO) {
                                        keyStorageService.encryptAndSavePrivateKey(
                                            privateKey,
                                            password,
                                            keyFile
                                        )
                                    }

                                    saveInProgress = false

                                    if (success) {
                                        saveResult = "✅ Ключ успішно збережено на: ${keyFile.absolutePath}"
                                        // Затримка перед переходом
                                        kotlinx.coroutines.delay(2000) // 2 секунди затримки
                                        navigator.navigateTo(Screen.Dashboard)
                                    } else {
                                        saveResult = "❌ Помилка збереження ключа"
                                    }
                                }
                            }
                        },
                        enabled = selectedDrive != null && !saveInProgress
                    ) {
                        Text("Зберегти ключ")
                    }
                }
            }
        }
    }
}

/**
 * Простий Dashboard екран
 */
@Composable
fun DashboardScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Головна панель",
            style = MaterialTheme.typography.h4
        )
    }
}