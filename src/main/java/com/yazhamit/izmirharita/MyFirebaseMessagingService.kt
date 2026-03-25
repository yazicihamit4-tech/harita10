package com.yazhamit.izmirharita

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Sinyal 35.5 Bildirimi"
        val message = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Yeni bir bildiriminiz var"

        showNotification(title, message)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token yenilendiğinde burası tetiklenir, ancak biz ihtiyacımız olan yerlerde (Girişte ve Sinyal atarken)
        // manuel olarak token alıp Firebase'e kaydettiğimiz için buraya şimdilik ekstra bir kod yazmıyoruz.
    }

    private fun showNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "sinyal_35_5_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Gerekirse burayı kendi icon'unuz ile değiştirin (ör: R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 (Oreo) ve üzeri için Notification Channel zorunludur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sinyal 35.5 Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Uygulama bildirimlerini gösterir"
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }
}
