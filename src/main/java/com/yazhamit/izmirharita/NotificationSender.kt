package com.yazhamit.izmirharita

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object NotificationSender {

    // Kendi Firebase Cloud Function URL'inizi buraya yazın. (Örnek URL aşağıdadır)
    // Firebase Console -> Functions -> url kopyalayın
    private const val NOTIFY_USER_FUNCTION_URL = "https://us-central1-izmirharita.cloudfunctions.net/notifyUser"
    private const val NOTIFY_ADMIN_FUNCTION_URL = "https://us-central1-izmirharita.cloudfunctions.net/notifyAdmins"

    fun sendNotificationToUser(
        context: Context,
        fcmToken: String,
        durum: String,
        cevap: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonObject = JSONObject().apply {
                    put("token", fcmToken)
                    put("durum", durum)
                    put("cevap", cevap)
                }

                val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url(NOTIFY_USER_FUNCTION_URL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val client = OkHttpClient()
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        onResult(true, "Kullanıcıya bildirim başarıyla gönderildi.")
                    } else {
                        onResult(false, "Cloud Function Hatası (${response.code}): $responseBody")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Bağlantı Hatası: ${e.message}")
            }
        }
    }

    fun sendNotificationToAdmins(
        context: Context,
        isim: String,
        mesaj: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonObject = JSONObject().apply {
                    put("isim", isim)
                    put("mesaj", mesaj)
                }

                val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url(NOTIFY_ADMIN_FUNCTION_URL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val client = OkHttpClient()
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        onResult(true, "Adminlere bildirim başarıyla gönderildi.")
                    } else {
                        onResult(false, "Cloud Function Hatası (${response.code}): $responseBody")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Bağlantı Hatası: ${e.message}")
            }
        }
    }
}
