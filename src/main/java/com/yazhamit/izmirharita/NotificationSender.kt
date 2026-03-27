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

    // Kendi sunucunuzu ayağa kaldırdığınızda bu URL'i o sunucunun IP'si veya Domain'i ile değiştirin.
    // Şimdilik Android Emulator üzerinden yerel makineye bağlanmak için 10.0.2.2 kullanıyoruz.
    // Ancak dinamik olarak backendUrl parametresi alacağız.

    fun sendNotificationToUser(context: Context, fcmToken: String, durum: String, cevap: String, backendUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonObject = JSONObject().apply {
                    put("token", fcmToken)
                    put("durum", durum)
                    put("cevap", cevap)
                }

                val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url("$backendUrl/notify-user")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val client = OkHttpClient()
                client.newCall(request).execute().use { response ->
                    println("Backend Yanıtı (User): ${response.code} ${response.message}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendNotificationToAdmins(context: Context, isim: String, mesaj: String, backendUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonObject = JSONObject().apply {
                    put("isim", isim)
                    put("mesaj", mesaj)
                }

                val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url("$backendUrl/notify-admin")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val client = OkHttpClient()
                client.newCall(request).execute().use { response ->
                    println("Backend Yanıtı (Admin): ${response.code} ${response.message}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
