package service

import java.io.File
import java.security.KeyPair

class LoginService {
    private val apiClient = ApiClient()
    private var keyPair: KeyPair? = null
    private var publicKeyString: String? = null

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

        // Генерація ключової пари Ed25519
        keyPair = apiClient.generateKeyPair()
        publicKeyString = apiClient.publicKeyToString(keyPair!!.public)

        // Виводимо ключі в консоль при логіні
        println("🔐 КЛЮЧІ ПРИ ЛОГІНІ:")
        println("👤 Користувач: $username")
        println("📋 Публічний ключ: $publicKeyString")
        println("🔒 Приватний ключ: ${apiClient.privateKeyToString(keyPair!!.private)}")
        println()

        // Виклик API для отримання challenge
        val response = apiClient.sendLoginData(
            url = "http://localhost:8000/auth/register", // Оновлений URL
            username = username,
            publicKey = publicKeyString!!
        )

        return if (response.contains("error", ignoreCase = true)) {
            response // якщо API повернув помилку → показуємо користувачу
        } else {
            // Тут буде обробка challenge та відправка підпису
            processChallenge(response, password)
        }
    }

    private fun processChallenge(challengeResponse: String, password: String): String? {
        return try {
            // Парсимо challenge з відповіді сервера
            val challenge = challengeResponse // Тимчасово, потрібно парсити JSON

            // Створюємо повідомлення для підпису: challenge + пароль
            val messageToSign = "$challenge:$password"

            // Підписуємо повідомлення
            val signature = apiClient.signMessage(keyPair!!.private, messageToSign)

            // Виводимо інформацію про підпис в консоль
            println("✍️ ПІДПИС ПРИ ЛОГІНІ:")
            println("📝 Повідомлення: $messageToSign")
            println("🔏 Підпис: $signature")
            println("📏 Довжина підпису: ${signature.length} символів")
            println()

            // Відправляємо підпис на сервер для верифікації
            // (тут потрібно додати метод для відправки підпису)
            null // Тимчасово повертаємо null - успішний вхід
        } catch (e: Exception) {
            val errorMessage = "Помилка обробки challenge: ${e.message}"
            println("❌ $errorMessage")
            errorMessage
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