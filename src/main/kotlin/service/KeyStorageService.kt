// ------------------------- KeyStorageService.kt -------------------------
package service

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.spec.KeySpec

class KeyStorageService {

    fun encryptAndSavePrivateKey(privateKey: String, password: String, outputFile: File): Boolean {
        return try {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
            val tmp = factory.generateSecret(spec)
            val secretKey: SecretKey = SecretKeySpec(tmp.encoded, "AES")

            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

            val cipherText = cipher.doFinal(privateKey.toByteArray(StandardCharsets.UTF_8))

            val output = ByteBuffer.allocate(salt.size + iv.size + cipherText.size)
            output.put(salt)
            output.put(iv)
            output.put(cipherText)

            outputFile.writeBytes(output.array())
            true
        } catch (e: Exception) {
            println("❌ Помилка шифрування: ${e.message}")
            false
        }
    }

    fun decryptPrivateKey(encryptedFile: File, password: String): String? {
        return try {
            val data = encryptedFile.readBytes()
            val buffer = ByteBuffer.wrap(data)

            val salt = ByteArray(16).also { buffer.get(it) }
            val iv = ByteArray(12).also { buffer.get(it) }
            val cipherText = ByteArray(data.size - 28).also { buffer.get(it) }

            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
            val tmp = factory.generateSecret(spec)
            val secretKey: SecretKey = SecretKeySpec(tmp.encoded, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

            String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            println("❌ Помилка розшифрування: ${e.message}")
            null
        }
    }
}
