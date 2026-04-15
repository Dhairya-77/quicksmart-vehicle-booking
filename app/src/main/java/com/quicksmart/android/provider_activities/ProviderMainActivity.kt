package com.quicksmart.android.provider_activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.quicksmart.android.R
import com.quicksmart.android.common_activities.NotificationsActivity
import com.quicksmart.android.common_activities.QuickSmartFCMService
import com.quicksmart.android.common_activities.saveFcmToken
import com.quicksmart.android.provider_activities.fragments.ProviderAccountCenterFragment
import com.quicksmart.android.provider_activities.fragments.ProviderHomeFragment
import com.quicksmart.android.reusable_methods.NotificationPermissionHelper
import com.quicksmart.android.reusable_methods.getString

class ProviderMainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance(Firebase.app, "quicksmart-db")

    private lateinit var toolbar         : MaterialToolbar
    private lateinit var bottomNav       : BottomNavigationView
    private lateinit var notifPermission : NotificationPermissionHelper

    private var notificationBadge : TextView?            = null
    private var notificationListener: ListenerRegistration? = null
    private var isFirstLoad       : Boolean              = true   // to avoid showing existing unread as popups on start

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provider_main)

        toolbar   = findViewById(R.id.toolbar)
        bottomNav = findViewById(R.id.bottom_nav)

        setSupportActionBar(toolbar)

        // ── Notification permission (Android 13+) ─────────────────────────────
        notifPermission = NotificationPermissionHelper(this)
        notifPermission.request()

        // Ensure FCM token is saved / refreshed
        val userId = getString("userId")
        if (userId.isNotEmpty()) saveFcmToken(userId)

        if (savedInstanceState == null) {
            loadFragment(ProviderHomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_provider_home    -> loadFragment(ProviderHomeFragment())
                R.id.nav_provider_account -> loadFragment(ProviderAccountCenterFragment())
            }
            true
        }
    }

    // ── Lifecycle: start / stop the status listener ───────────────────────────

    override fun onStart() {
        super.onStart()
        startNotificationListener()
    }

    override fun onStop() {
        super.onStop()
        notificationListener?.remove()
        notificationListener = null
    }

    override fun onResume() {
        super.onResume()
        loadUnreadCount()
    }

    // ── Firestore real-time notification listener ────────────────────────────
    //
    // Watches the flat 'notifications' collection for this user.
    // If a new UNREAD document is added while the app is active, 
    // it triggers a local system notification popup.
    //
    private fun startNotificationListener() {
        val userId = getString("userId")
        if (userId.isEmpty()) return

        notificationListener = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                // On first load, snapshot contains ALL existing unread notifications.
                // We only want to show popups for notifications that arrive AFTER the listener starts.
                if (isFirstLoad) {
                    isFirstLoad = false
                    updateNotificationBadge(snapshot.size())
                    return@addSnapshotListener
                }

                for (change in snapshot.documentChanges) {
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val doc     = change.document
                        val title   = doc.getString("title")   ?: "Notification"
                        val message = doc.getString("message") ?: ""
                        
                        showLocalSystemNotification(title, message)
                    }
                }
                
                updateNotificationBadge(snapshot.size())
            }
    }

    /** Shows a system-tray notification for any new message */
    private fun showLocalSystemNotification(title: String, message: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                QuickSmartFCMService.CHANNEL_ID,
                QuickSmartFCMService.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, ProviderMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, QuickSmartFCMService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Use current time as ID to ensure multiple notifications don't overwrite each other
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // ── Options menu / notification badge ─────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_notification_menu, menu)

        val actionView = menu.findItem(R.id.action_notifications)?.actionView
        notificationBadge = actionView?.findViewById(R.id.notificationBadge)

        actionView?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        loadUnreadCount()
        return true
    }

    /** Fetch unread count from flat `notifications` collection */
    private fun loadUnreadCount() {
        val userId = getString("userId")
        if (userId.isEmpty()) return

        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { snapshot ->
                updateNotificationBadge(snapshot.size())
            }
    }

    fun updateNotificationBadge(count: Int) {
        notificationBadge?.apply {
            if (count > 0) {
                text = if (count > 99) "99+" else count.toString()
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    companion object {
        // IDs are now dynamic (System.currentTimeMillis) to allow multiple popups
    }
}
