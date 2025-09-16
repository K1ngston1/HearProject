package service

import java.io.File
import java.security.KeyPair
import java.util.Base64
import com.google.gson.Gson

class LoginService {
    private val apiClient = ApiClient()
    private var keyPair: KeyPair? = null
    private var publicKeyString: String? = null
    private val gson = Gson()

    fun login(username: String, password: String, selectedFile: File? = null): String? {
        if (username.isBlank() || password.isBlank()) return "Логін та пароль не можуть бути порожніми"
        if (isProbablyPassword(username)) return "Помилка: у полі логіна введено пароль"
        if (isProbablyUsername(password)) return "Помилка: у полі пароля введено логін"

        keyPair = apiClient.generateKeyPair()
        publicKeyString = apiClient.publicKeyToString(keyPair!!.public)

        println("🔐 КЛЮЧІ ПРИ ЛОГІНІ:")
        println("👤 Користувач: $username")
        println("📋 Публічний ключ: $publicKeyString")
        println("🔒 Приватний ключ: ${apiClient.privateKeyToString(keyPair!!.private)}")
        println()

        // Спочатку реєструємо користувача
        val registerResponse = apiClient.sendLoginData(
            url = "http://localhost:8000/auth/register",
            username = username,
            publicKey = publicKeyString!!
        )

        println("📨 Відповідь реєстрації: $registerResponse")

        // Перевіряємо наявність помилок
        if (registerResponse.contains("error", ignoreCase = true) ||
            registerResponse.contains("detail", ignoreCase = true)) {
            return "Помилка реєстрації: $registerResponse"
        }

        // Отримуємо user_id з відповіді
        val userId = apiClient.extractUserIdFromResponse(registerResponse)
        if (userId == null) {
            return "Помилка: не вдалося отримати user_id з відповіді: $registerResponse"
        }

        println("✅ Користувач зареєстрований, user_id: $userId")

        // Запитуємо challenge
        val challengeResponse = apiClient.requestChallenge(userId)
        println("📨 Відповідь challenge: $challengeResponse")

        val challenge = extractChallengeFromResponse(challengeResponse)
        if (challenge == null) {
            return "Помилка: не вдалося отримати challenge: $challengeResponse"
        }

        println("🔑 Отримано challenge: ${challenge.take(20)}...")

        // ВИПРАВЛЕННЯ: підписуємо raw байти challenge (після розкодування з Base64)
        val challengeBytes = Base64.getDecoder().decode(challenge)
        val signature = apiClient.signMessage(keyPair!!.private, challenge)

        println("✍️ ПІДПИС:")
        println("📝 Повідомлення для підпису (Base64): ${challenge.take(20)}...")
        println("📏 Розмір challenge: ${challengeBytes.size} байт")
        println("🔏 Підпис: $signature")

        // Верифікуємо підпис
        val isVerified = apiClient.verifySignature(userId, challenge, signature)

        return if (isVerified) {
            println("✅ Вхід успішний!")
            null // Успішний вхід
        } else {
            "❌ Помилка верифікації підпису"
        }
    }

    private fun extractChallengeFromResponse(response: String): String? {
        return try {
            val jsonMap = gson.fromJson(response, Map::class.java)
            jsonMap["challenge"] as? String
        } catch (e: Exception) {
            null
        }
    }

    private fun isProbablyPassword(value: String) =
        (value.any { it.isDigit() } || value.any { !it.isLetterOrDigit() }) && value.length >= 6

    private fun isProbablyUsername(value: String) =
        value.all { it.isLetter() } && value.length in 3..12
}