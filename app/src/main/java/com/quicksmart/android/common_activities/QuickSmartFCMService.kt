package com.quicksmart.android.common_activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.quicksmart.android.R
import com.quicksmart.android.reusable_methods.getString
import com.quicksmart.android.reusable_methods.putString

class QuickSmartFCMService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID   = "quicksmart_notifications"
        const val CHANNEL_NAME = "QuickSmart Notifications"
    }

    private val db by lazy { FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db") }

    // ── Token refresh: save new token to Firestore ────────────────────────────
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        putString("fcmToken", token)
        // Persist to Firestore if user is already logged in
        val userId = getString("userId")
        if (userId.isNotEmpty()) {
            saveFcmTokenToFirestore(userId, token)
        }
    }

    // ── Incoming message: show system notification + store in Firestore ───────
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title   = message.notification?.title ?: message.data["title"] ?: "QuickSmart"
        val body    = message.notification?.body  ?: message.data["body"]  ?: ""
        val type    = message.data["type"] ?: "info"

        // Store in Firestore notifications collection
        val userId = getString("userId")
        if (userId.isNotEmpty()) {
            storeNotification(userId, title, body, type)
        }

        // Show system push notification
        showSystemNotification(title, body)
    }

    // ── Store in flat `notifications` collection ──────────────────────────────
    private fun storeNotification(userId: String, title: String, message: String, type: String) {
        val doc = hashMapOf(
            "userId"     to userId,
            "title"      to title,
            "message"    to message,
            "type"       to type,
            "isRead"     to false,
            "createdAt"  to com.google.firebase.Timestamp.now(),
            "expiryDate" to com.quicksmart.android.common_activities.NotificationsActivity.expiryTimestamp()
        )
        db.collection("notifications").add(doc)
    }

    // ── Show Android system notification ─────────────────────────────────────
    private fun showSystemNotification(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel (required Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        // ── Route notification tap to the correct main activity based on role ──
        // When consumer FCM is enabled, this already handles routing correctly.
        val role = getString("role", "consumer")
        val targetClass = when (role) {
            "provider" -> com.quicksmart.android.provider_activities.ProviderMainActivity::class.java
            "admin"    -> com.quicksmart.android.admin_activities.AdminMainActivity::class.java
            else       -> com.quicksmart.android.consumer_activities.ConsumerMainActivity::class.java
        }
        val intent = Intent(this, targetClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // ── Helper accessible from other classes ──────────────────────────────────
    fun saveFcmTokenToFirestore(userId: String, token: String) {
        db.collection("users").document(userId)
            .update("fcmToken", token)
    }
}

// ── Standalone helper functions (call from Activity/Fragment) ─────────────

fun Context.saveFcmToken(userId: String) {
    com.google.firebase.messaging.FirebaseMessaging.getInstance().token
        .addOnSuccessListener { token ->
            if (token.isNotEmpty()) {
                val db = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")
                db.collection("users").document(userId).update("fcmToken", token)

                // Also persist locally
                getSharedPreferences("QuickSmartPrefs", Context.MODE_PRIVATE)
                    .edit().putString("fcmToken", token).apply()
            }
        }
}
