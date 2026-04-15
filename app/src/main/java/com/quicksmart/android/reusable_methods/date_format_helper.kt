package com.quicksmart.android.reusable_methods

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Safely extracts a Timestamp from Firestore, handling both Timestamp and legacy String fields.
 * Use this to avoid RuntimeException if some documents have the field as a String.
 */
fun DocumentSnapshot.getTimestampSafe(field: String): Timestamp? {
    return try {
        this.getTimestamp(field)
    } catch (e: Exception) {
        // Fallback for legacy data stored as String
        val dateStr = this.getString(field) ?: return null
        
        // Try various common formats used in the app previously
        val formats = listOf("dd MMM yyyy", "dd-MMM-yyyy", "dd/MM/yyyy")
        for (format in formats) {
            try {
                val date = SimpleDateFormat(format, Locale.getDefault()).parse(dateStr)
                if (date != null) return Timestamp(date)
            } catch (ex: Exception) { /* continue */ }
        }
        null
    }
}

/**
 * Converts a Firestore Timestamp → "10-Apr-2026"
 *
 * Usage: doc.getTimestamp("createdAt")?.toFormattedDate() ?: "—"
 */
fun Timestamp.toFormattedDate(): String =
    SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).format(toDate())

/**
 * Converts a Firestore Timestamp → "10-Apr-2026 14:30"
 *
 * Usage: doc.getTimestamp("fromDate")?.toFormattedDateTime() ?: "—"
 */
fun Timestamp.toFormattedDateTime(): String =
    SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.ENGLISH).format(toDate())


/**
 * Converts a Firestore Timestamp → a human-readable relative time string.
 *
 * Examples:
 *   < 1 min  → "Just now"
 *   < 1 hour → "5 min ago"
 *   < 1 day  → "2 hours ago"
 *   = 1 day  → "Yesterday"
 *   < 7 days → "3 days ago"
 *   < 4 wks  → "2 weeks ago"
 *   < 1 yr   → "1 month ago" / "3 months ago"
 *   ≥ 1 yr   → falls back to formatted date "10-Apr-2025"
 *
 * Usage: doc.getTimestamp("createdAt")?.toRelativeTime() ?: "Just now"
 */
fun Timestamp.toRelativeTime(): String {
    val nowMs    = System.currentTimeMillis()
    val thenMs   = toDate().time
    val diffMs   = nowMs - thenMs

    if (diffMs < 0) return "Just now"           // clock skew guard

    val seconds  = TimeUnit.MILLISECONDS.toSeconds(diffMs)
    val minutes  = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours    = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days     = TimeUnit.MILLISECONDS.toDays(diffMs)
    val weeks    = days / 7
    val months   = days / 30

    return when {
        seconds < 60          -> "Just now"
        minutes < 60          -> "$minutes min ago"
        hours   < 24          -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
        days    == 1L         -> "Yesterday"
        days    < 7           -> "$days days ago"
        weeks   == 1L         -> "1 week ago"
        weeks   < 4           -> "$weeks weeks ago"
        months  == 1L         -> "1 month ago"
        months  < 12          -> "$months months ago"
        else                  -> toFormattedDate()   // fall back to absolute date
    }
}
