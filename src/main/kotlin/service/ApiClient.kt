package service

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.KeyPair as JavaKeyPair

class ApiClient {
    private val client = OkHttpClient()

    fun sendLoginData(url: String, username: String, publicKey: String): String {
        return try {
            val json = """{"username":"$username","publicKey":"$publicKey"}"""
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string() ?: "Пустий відповідь"
            } else {
                "Помилка: ${response.code}"
            }
        } catch (e: Exception) {
            "Помилка: ${e.message}"
        }
    }

    fun generateKeyPair(): JavaKeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EdDSA")
        val parameterSpec = java.security.spec.NamedParameterSpec("Ed25519")
        keyPairGenerator.initialize(parameterSpec)
        return keyPairGenerator.generateKeyPair()
    }

    fun signMessage(privateKey: java.security.PrivateKey, message: String): String {
        val signature = Signature.getInstance("Ed25519")
        signature.initSign(privateKey)
        signature.update(message.toByteArray(Charsets.UTF_8))
        val signatureBytes = signature.sign()
        return Base64.getEncoder().encodeToString(signatureBytes)
    }

    fun publicKeyToString(publicKey: java.security.PublicKey): String {
        return Base64.getEncoder().encodeToString(publicKey.encoded)
    }

    fun privateKeyToString(privateKey: java.security.PrivateKey): String {
        return Base64.getEncoder().encodeToString(privateKey.encoded)
    }

    fun verifySignature(publicKey: java.security.PublicKey, message: String, signature: String): Boolean {
        val sig = Signature.getInstance("Ed25519")
        sig.initVerify(publicKey)
        sig.update(message.toByteArray(Charsets.UTF_8))
        val signatureBytes = Base64.getDecoder().decode(signature)
        return sig.verify(signatureBytes)
    }

    fun testKeyGeneration(): String {
        return try {
            // Генеруємо ключову пару
            val keyPair = generateKeyPair()
            val publicKeyStr = publicKeyToString(keyPair.public)
            val privateKeyStr = privateKeyToString(keyPair.private)

            // Виводимо ключі в консоль
            println("🔐 ЗГЕНЕРОВАНІ КЛЮЧІ:")
            println("📋 Публічний ключ: $publicKeyStr")
            println("🔒 Приватний ключ: $privateKeyStr")
            println("📏 Довжина публічного ключа: ${publicKeyStr.length} символів")
            println("📏 Довжина приватного ключа: ${privateKeyStr.length} символів")
            println()

            // Тестове повідомлення для підпису
            val testMessage = "Hello, Ed25519!"

            // Підписуємо повідомлення
            val signature = signMessage(keyPair.private, testMessage)
            println("✍️ Підпис повідомлення: $signature")
            println("📏 Довжина підпису: ${signature.length} символів")
            println()

            // Перевіряємо підпис
            val isValid = verifySignature(keyPair.public, testMessage, signature)
            println("✅ Верифікація підпису: ${if (isValid) "УСПІШНО" else "НЕ ВДАЛАСЬ"}")

            // Формуємо результат для UI
            """
            ✅ Генерація ключів успішна!
            
            📋 Публічний ключ: ${publicKeyStr.take(20)}... (${publicKeyStr.length} chars)
            🔒 Приватний ключ: ${privateKeyStr.take(20)}... (${privateKeyStr.length} chars)
            
            ✍️ Підпис повідомлення: ${signature.take(20)}... (${signature.length} chars)
            ✅ Верифікація підпису: ${if (isValid) "УСПІШНО" else "НЕ ВДАЛАСЬ"}
            
            Алгоритм: Ed25519
            """.trimIndent()

        } catch (e: Exception) {
            val errorMessage = """
            ❌ Помилка тестування:
            ${e.message}
            
            Stack trace:
            ${e.stackTraceToString()}
            """.trimIndent()

            // Виводимо помилку в консоль
            println("❌ ПОМИЛКА ГЕНЕРАЦІЇ КЛЮЧІВ:")
            println(errorMessage)

            errorMessage
        }
    }
}