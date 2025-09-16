package service

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.KeyPair as JavaKeyPair
import com.google.gson.Gson

// Виправлена data class - використовуємо snake_case для відповідності серверу
data class LoginRequest(val username: String, val public_key: String)

class ApiClient {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val baseUrl = "http://localhost:8000/auth"

    fun sendLoginData(url: String, username: String, publicKey: String): String {
        return try {
            // Використовуємо snake_case для public_key
            val json = gson.toJson(LoginRequest(username, publicKey))
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            println("📤 Відправляємо запит: $json")

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: "Пустий відповідь"
                println("📥 Отримано відповідь: $responseBody")
                responseBody
            }
        } catch (e: Exception) {
            "Помилка: ${e.message}"
        }
    }

    // Новий метод для запиту challenge
    fun requestChallenge(userId: Int): String {
        return try {
            val json = gson.toJson(ChallengeRequest(userId))
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/challenge")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                response.body?.string() ?: "Пустий відповідь"
            }
        } catch (e: Exception) {
            "Помилка запиту challenge: ${e.message}"
        }
    }

    // Новий метод для верифікації підпису
    fun verifySignature(userId: Int, challenge: String, signature: String): Boolean {
        return try {
            val json = gson.toJson(SignatureRequest(userId, challenge, signature))
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/signature")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: return false
                val signatureResponse = gson.fromJson(responseBody, SignatureResponse::class.java)
                signatureResponse.is_success
            }
        } catch (e: Exception) {
            false
        }
    }

    // Допоміжний метод для отримання user_id з відповіді реєстрації
    fun extractUserIdFromResponse(response: String): Int? {
        return try {
            val jsonMap = gson.fromJson(response, Map::class.java)
            (jsonMap["user_id"] as? Double)?.toInt() ?: (jsonMap["user_id"] as? Int)
        } catch (e: Exception) {
            null
        }
    }

    fun generateKeyPair(): JavaKeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EdDSA")
        keyPairGenerator.initialize(java.security.spec.NamedParameterSpec("Ed25519"))
        return keyPairGenerator.generateKeyPair()
    }

    // ВИПРАВЛЕНА ФУНКЦІЯ: повертає raw 32-байтний публічний ключ
    fun publicKeyToString(publicKey: java.security.PublicKey): String {
        val encoded = publicKey.encoded
        // Для Ed25519 публічний ключ - останні 32 байти
        // Структура: 12 байт заголовка + 32 байти ключа
        val rawPublicKey = encoded.copyOfRange(encoded.size - 32, encoded.size)
        return Base64.getEncoder().encodeToString(rawPublicKey)
    }

    // ВИПРАВЛЕНА ФУНКЦІЯ: повертає raw 32-байтний приватний ключ
    fun privateKeyToString(privateKey: java.security.PrivateKey): String {
        val encoded = privateKey.encoded
        // Для Ed25519 приватний ключ - останні 32 байти
        // Структура: 16 байт заголовка + 32 байти ключа
        val rawPrivateKey = encoded.copyOfRange(encoded.size - 32, encoded.size)
        return Base64.getEncoder().encodeToString(rawPrivateKey)
    }

    fun signMessage(privateKey: java.security.PrivateKey, message: String): String {
        val signature = Signature.getInstance("Ed25519")
        signature.initSign(privateKey)
        // Розкодовуємо Base64 повідомлення перед підписом
        val messageBytes = Base64.getDecoder().decode(message)
        signature.update(messageBytes) // Підписуємо raw байти
        val signatureBytes = signature.sign()
        return Base64.getEncoder().encodeToString(signatureBytes)
    }

    fun verifySignature(publicKey: java.security.PublicKey, message: String, signature: String): Boolean {
        val sig = Signature.getInstance("Ed25519")
        sig.initVerify(publicKey)
        sig.update(message.toByteArray(Charsets.UTF_8))
        return sig.verify(Base64.getDecoder().decode(signature))
    }

    fun testKeyGeneration(): String {
        return try {
            val keyPair = generateKeyPair()
            val pub = publicKeyToString(keyPair.public)
            val priv = privateKeyToString(keyPair.private)
            val msg = "Hello Ed25519!"
            val sig = signMessage(keyPair.private, msg)
            val valid = verifySignature(keyPair.public, msg, sig)

            // Перевірка розміру ключів
            val pubBytes = Base64.getDecoder().decode(pub)
            val privBytes = Base64.getDecoder().decode(priv)

            """
            ✅ Генерація ключів успішна!
            📋 Публічний ключ (RAW): ${pub.take(20)}... (${pub.length} chars)
            📏 Розмір публічного ключа: ${pubBytes.size} байт (очікується: 32)
            🔒 Приватний ключ (RAW): ${priv.take(20)}... (${priv.length} chars)
            📏 Розмір приватного ключа: ${privBytes.size} байт (очікується: 32)
            ✍️ Підпис повідомлення: ${sig.take(20)}... (${sig.length} chars)
            ✅ Верифікація: ${if (valid) "УСПІШНО" else "НЕ ВДАЛАСЬ"}
            """.trimIndent()
        } catch (e: Exception) {
            "❌ Помилка: ${e.message}"
        }
    }
}

// Допоміжні data classes для інших запитів
data class ChallengeRequest(val user_id: Int)
data class SignatureRequest(val user_id: Int, val challenge: String, val signature: String)
data class SignatureResponse(val is_success: Boolean)