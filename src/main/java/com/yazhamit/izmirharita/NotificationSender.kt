package com.yazhamit.izmirharita

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject


/**
 * =========================================================================================
 * ⚠️ DİKKAT (GÜVENLİK UYARISI):
 * Google Service Account (service_account.json) dosyasının uygulamanın içine (Android APK)
 * gömülmesi (Serverless FCM Yöntemi), Google tarafından ÖNERİLMEYEN ve güvensiz kabul edilen bir
 * mimaridir. Kötü niyetli kişiler APK dosyasını açıp (Decompile) bu JSON dosyasını
 * bulabilir ve Firebase projenize tam erişim (Admin) hakkı kazanabilir.
 * Kullanıcı "Sunucu/Backend kullanmak istemiyorum, direkt Android içinden gitsin"
 * talebinde bulunduğu için bu mimari kurulmuştur.
 * =========================================================================================
 */
object NotificationSender {

    // Kendi Firebase projenizin "Project ID" sini buraya yazın:
    private const val PROJECT_ID = "izmirharita"
    private const val FCM_API_URL = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"

    private fun getAccessToken(context: Context): String? {
        return try {
            val inputStream = context.resources.openRawResource(R.raw.service_account)
            val credentials = GoogleCredentials.fromStream(inputStream)
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
            credentials.refreshIfExpired()
            credentials.accessToken.tokenValue
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
                val accessToken = getAccessToken(context)
                if (accessToken == null) {
                    onResult(false, "Service Account dosyası bulunamadı veya geçersiz. (R.raw.service_account)")
                    return@launch
                }

                val notificationBody = JSONObject().apply {
                    put("title", "Sinyalinizin Durumu Güncellendi: $durum")
                    put("body", "Yetkili Yanıtı: $cevap")
                }

                val message = JSONObject().apply {
                    put("token", fcmToken)
                    put("notification", notificationBody)
                }

                val root = JSONObject().apply {
                    put("message", message)
                }

                val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url(FCM_API_URL)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val client = OkHttpClient()
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        onResult(true, "Kullanıcıya bildirim başarıyla gönderildi.")
                    } else {
                        onResult(false, "FCM Hatası (${response.code}): $responseBody")
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
                // Admin tokenlarını Firestore'dan çekelim
                val snapshot = FirebaseFirestore.getInstance().collection("admin_tokens").get().await()
                val tokens = snapshot.documents.mapNotNull { it.getString("token") }

                if (tokens.isEmpty()) {
                    onResult(false, "Kayıtlı admin cihazı bulunamadı.")
                    return@launch
                }

                val accessToken = getAccessToken(context)
                if (accessToken == null) {
                    onResult(false, "Service Account dosyası bulunamadı veya geçersiz.")
                    return@launch
                }

                val notificationBody = JSONObject().apply {
                    put("title", "🚨 Yeni Sinyal Çakıldı")
                    put("body", "$isim: $mesaj")
                }

                var successCount = 0
                var failMessage = ""

                val client = OkHttpClient()

                // v1 API doğrudan Multicast'i (tek istekte çok token) desteklemediği için token'lar üzerinde döngü kuruyoruz
                // Daha verimli yöntem Topic kullanmaktır ancak token yapısını korumak için döngü ekliyoruz:
                for (token in tokens) {
                    val messageObj = JSONObject().apply {
                        put("token", token)
                        put("notification", notificationBody)
                    }
                    val root = JSONObject().apply {
                        put("message", messageObj)
                    }

                    val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                    val request = Request.Builder()
                        .url(FCM_API_URL)
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer $accessToken")
                        .addHeader("Content-Type", "application/json")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            successCount++
                        } else {
                            failMessage = response.body?.string() ?: "Bilinmeyen FCM hatası"
                        }
                    }
                }

                if (successCount > 0) {
                    onResult(true, "$successCount Admine bildirim başarıyla gönderildi.")
                } else {
                    onResult(false, "Hiçbir admine bildirim gönderilemedi. Hata: $failMessage")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Bağlantı/Veritabanı Hatası: ${e.message}")
            }
        }
    }
}
