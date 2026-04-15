package com.quicksmart.android.reusable_methods

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Manages the POST_NOTIFICATIONS runtime permission required on Android 13+ (API 33).
 *
 * Usage in Activity.onCreate():
 *
 *   private lateinit var notifPermission: NotificationPermissionHelper
 *
 *   override fun onCreate(...) {
 *       notifPermission = NotificationPermissionHelper(this)
 *       notifPermission.request()
 *   }
 */
class NotificationPermissionHelper(private val activity: AppCompatActivity) {

    private val launcher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                // Permission denied — show a soft explanation (not blocking)
                showDeniedRationale()
            }
        }

    /** Call once from onCreate — does nothing on API < 33 */
    fun request() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return // Not needed below API 33

        when {
            // Already granted — nothing to do
            ContextCompat.checkSelfPermission(
                activity, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> return

            // Should show rationale (user previously denied, not permanently)
            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                showRationaleAndRequest()
            }

            // First time asking
            else -> launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** True if notification permission is currently granted (or API < 33) */
    fun isGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            activity, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun showRationaleAndRequest() {
        AlertDialog.Builder(activity)
            .setTitle("Enable Notifications")
            .setMessage(
                "QuickSmart needs notification permission to alert you about important " +
                "updates like account approvals and ride requests. " +
                "Please allow notifications to stay informed."
            )
            .setCancelable(false)
            .setPositiveButton("Allow") { _, _ ->
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Not now", null)
            .show()
    }

    private fun showDeniedRationale() {
        AlertDialog.Builder(activity)
            .setTitle("Notifications Disabled")
            .setMessage(
                "You won't receive push notifications from QuickSmart. " +
                "You can enable them anytime from your phone's Settings → Apps → QuickSmart → Notifications."
            )
            .setPositiveButton("OK", null)
            .show()
    }
}
