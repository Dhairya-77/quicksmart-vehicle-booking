package com.quicksmart.android.common_activities.adapter_model

data class NotificationModel(
    val id         : String  = "",
    val userId     : String  = "",
    val title      : String  = "",
    val message    : String  = "",
    val type       : String  = "",       // "approval", "info", etc.
    val isRead     : Boolean = false,
    val timestamp  : String  = "",       // relative time string e.g. "2 hours ago"
    val expiryDate : String  = ""        // formatted date after which it auto-deletes in Firestore
)
