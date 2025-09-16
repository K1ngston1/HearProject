package com.example.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ui.Navigator

fun main() = application {
    val navigator = Navigator()
    navigator.StartApp()
}