package service

import java.io.File

class LoginService {

    private val apiClient = ApiClient()

    /**
     * Повертає:
     * - null → успішний вхід
     * - String → текст помилки
     */
    fun login(username: String, password: String, selectedFile: File? = null): String? {
        // Порожні поля
        if (username.isBlank() || password.isBlank()) {
            return "Логін та пароль не можуть бути порожніми"
        }

        // Перевірка переплутаних полів
        if (isProbablyPassword(username)) {
            return "Помилка: у полі логіна введено пароль"
        }
        if (isProbablyUsername(password)) {
            return "Помилка: у полі пароля введено логін"
        }

        // Виклик API
        val response = apiClient.sendLoginData(
            url = "http://localhost:8000/request-challenge",
            username = username
        )

        return if (response.contains("error", ignoreCase = true)) {
            response // якщо API повернув помилку → показуємо користувачу
        } else {
            null // успішний вхід
        }
    }

    private fun isProbablyPassword(value: String): Boolean {
        val hasDigit = value.any { it.isDigit() }
        val hasSpecial = value.any { !it.isLetterOrDigit() }
        return (hasDigit || hasSpecial) && value.length >= 6
    }

    private fun isProbablyUsername(value: String): Boolean {
        return value.all { it.isLetter() } && value.length in 3..12
    }
}

