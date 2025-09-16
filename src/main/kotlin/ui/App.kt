package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.awt.FileDialog
import java.io.File
import java.awt.Frame
import androidx.compose.ui.graphics.Color
import service.ApiClient
import service.LoginService
import ui.Navigator
import ui.Screen


@Composable
fun App(navigator: Navigator) {
    MaterialTheme {
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
}


@Composable
fun ServerResponseView(response: String) {
    if (response.isNotEmpty()) {
        Text(
            text = response,
            maxLines = 3, // максимум рядків
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            style = MaterialTheme.typography.body2
        )
    }
}
@Composable
fun FilePickerButton(selectedFile: File?, onFileSelected: (File) -> Unit) {
    Button(onClick = {
        val fd = FileDialog(Frame(), "Оберіть файл", FileDialog.LOAD)
        fd.isVisible = true
        val file = fd.files.firstOrNull()
        if (file != null) onFileSelected(file)
    }, modifier = Modifier.fillMaxWidth() , colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black , contentColor = Color.White)) {
        Text(selectedFile?.name ?: "Оберіть файл")
    }
}

@Composable
fun SaveKeyScreen(
    onKeySaved: () -> Unit,
    onCancel: () -> Unit
) {
    var selectedDrive by remember { mutableStateOf<File?>(null) }
    var saveInProgress by remember { mutableStateOf(false) }
    var saveResult by remember { mutableStateOf<String?>(null) }

    val availableDrives = remember {
        File.listRoots().filter { it.isDirectory && it.canWrite() }
    }

    Card(
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
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray)
                ) {
                    Text("Скасувати")
                }

                Button(
                    onClick = {
                        selectedDrive?.let { drive ->
                            saveInProgress = true
                            // Тут викликаємо логіку збереження ключа
                            // Після завершення викликаємо onKeySaved()
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

@Composable
fun UserProfileCard(onLoginSuccess: (privateKey: String, password: String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var testResult by remember { mutableStateOf<String?>(null) }

    val loginService = remember { LoginService() }
    val apiClient = remember { ApiClient() }

    val avatarBitmap: ImageBitmap? = remember {
        val stream: InputStream? = object {}.javaClass.getResourceAsStream("/avatar.png")
        stream?.use {
            Image.makeFromEncoded(it.readBytes()).asImageBitmap()
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = 8.dp,
        modifier = Modifier
            .width(320.dp)
            .height(500.dp) // Збільшимо висоту для нової кнопки
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            avatarBitmap?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Логін:") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль:") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Text(
                        text = if (showPassword) "👁️" else "🙈",
                        modifier = Modifier.clickable { showPassword = !showPassword }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            FilePickerButton(selectedFile) { file ->
                selectedFile = file
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val result = loginService.login(username, password, selectedFile)
                    if (result != null) {
                        javax.swing.JOptionPane.showMessageDialog(
                            null, result, "Помилка логіна", javax.swing.JOptionPane.ERROR_MESSAGE
                        )
                    } else {
                        // Тут передаємо ключ і пароль
                        val privateKey = "PRIVATE_KEY_STRING" // Отримати реально з LoginService
                        onLoginSuccess(privateKey, password)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Увійти")
            }

            }
        }
    }

