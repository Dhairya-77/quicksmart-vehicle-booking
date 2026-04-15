package com.quicksmart.android.consumer_activities.adapter_model

data class TransactionModel(
    val title:    String,
    val subtitle: String,
    val amount:   String,
    val date:     String,
    val isCredit: Boolean
)
