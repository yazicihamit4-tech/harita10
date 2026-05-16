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

    // Firebase Cloud Functions HTTP Tetikleyici (Trigger) URL'leriniz
    // firebase deploy --only functions yazdığınızda ekrana çıkan kendi "us-central1" ile başlayan linklerinizi buraya yazın.
    private const val NOTIFY_USER_FUNCTION_URL = "https://us-central1-izmirharita-d0d08.cloudfunctions.net/notifyUser"
    private const val NOTIFY_ADMIN_FUNCTION_URL = "https://us-central1-izmirharita-d0d08.cloudfunctions.net/notifyAdmins"
    private const val BROADCAST_FUNCTION_URL = "https://us-central1-izmirharita-d0d08.cloudfunctions.net/broadcastNotification"

    fun sendBroadcastNotification(
        context: Context,
        title: String,
        body: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonObject = JSONObject().apply {
                    put("title", title)
                    put("body", body)
                }

                val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url(BROADCAST_FUNCTION_URL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val client = OkHttpClient()
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        try {
                            val jsonRes = JSONObject(responseBody)
                            if (jsonRes.optBoolean("success", true)) {
                                onResult(true, "Tüm kullanıcılara duyuru gönderildi.")
                            } else {
                                onResult(false, "Firebase Functions Hatası: ${jsonRes.optString("error", "Bilinmeyen Hata")}")
                            }
                        } catch (e: Exception) {
                            onResult(true, "Tüm kullanıcılara duyuru gönderildi.")
                        }
                    } else {
                        onResult(false, "Sunucu Hatası (${response.code}): $responseBody")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Bağlantı Hatası: ${e.message}")
            }
        }
    }

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
                        try {
                            val jsonRes = JSONObject(responseBody)
                            if (jsonRes.optBoolean("success", true)) {
                                onResult(true, "Kullanıcıya bildirim başarıyla gönderildi.")
                            } else {
                                onResult(false, "Firebase Functions Hatası: ${jsonRes.optString("error", "Bilinmeyen Hata")}")
                            }
                        } catch (e: Exception) {
                            onResult(true, "Kullanıcıya bildirim gönderildi.")
                        }
                    } else {
                        onResult(false, "Sunucu Hatası (${response.code}): $responseBody")
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
                        try {
                            val jsonRes = JSONObject(responseBody)
                            if (jsonRes.optBoolean("success", true)) {
                                onResult(true, "Adminlere bildirim başarıyla gönderildi.")
                            } else {
                                onResult(false, "Firebase Functions Hatası: ${jsonRes.optString("error", "Bilinmeyen Hata")}")
                            }
                        } catch (e: Exception) {
                            onResult(true, "Adminlere bildirim gönderildi.")
                        }
                    } else {
                        onResult(false, "Sunucu Hatası (${response.code}): $responseBody")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Bağlantı Hatası: ${e.message}")
            }
        }
    }
}
