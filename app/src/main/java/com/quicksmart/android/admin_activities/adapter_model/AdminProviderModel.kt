package com.quicksmart.android.admin_activities.adapter_model

/**
 * Represents a provider in Firestore (collection: "users", role: "provider").
 */
data class AdminProviderModel(
    val userId        : String = "",
    val firstName     : String = "",
    val lastName      : String = "",
    val mobile        : String = "",
    val email         : String = "",
    val status        : String = "pending",   // "pending" | "approved" | "rejected"
    val createdAt     : String = "",
    val profileImg    : String = "",          // Firebase Storage URL — shown as live avatar
    val aadhaarUrl    : String = "",
    val licenceUrl    : String = ""
)
