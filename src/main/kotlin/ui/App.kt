package com.example.ui

import androidx.compose.foundation.Image
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
                onLoginSuccess = {
                    navigator.navigateTo(Screen.Dashboard)
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
fun UserProfileCard(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    val loginService = remember { LoginService() }

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
            .height(420.dp)
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
                        onLoginSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.onSurface)
            ) {
                Text("Увійти", color = MaterialTheme.colors.surface)
            }
        }
    }
}
