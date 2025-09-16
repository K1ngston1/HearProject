package service
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ApiClient {
    private val client = OkHttpClient()

    fun sendLoginData(url: String, username: String): String {
        return try {

            val json = """{"username":"$username"}"""
            val body = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                println(response.body!!.string())
                response.body?.string() ?: "Пустий відповідь"
            } else {
                "Помилка: ${response.code}"
            }
        } catch (e: Exception) {
            "Помилка: ${e.message}"
        }
    }
}