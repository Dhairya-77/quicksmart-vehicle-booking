package com.quicksmart.android.reusable_methods

import android.content.Context

private const val PREFS_NAME = "QuickSmartPrefs"

// Save a String value to SharedPreferences
fun Context.putString(key: String, value: String) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(key, value).apply()
}

// Save a Boolean value to SharedPreferences
fun Context.putBool(key: String, value: Boolean) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putBoolean(key, value).apply()
}

// Get a String value from SharedPreferences. Returns [default] if key not found
fun Context.getString(key: String, default: String = ""): String =
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(key, default) ?: default

// Get a Boolean value from SharedPreferences. Returns [default] if key not found
fun Context.getBool(key: String, default: Boolean = false): Boolean =
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(key, default)

// Remove a single key from SharedPreferences
fun Context.clearKey(key: String) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().remove(key).apply()
}

// Remove multiple keys from SharedPreferences at once
//fun Context.clearKeys(vararg keys: String) {
//    val editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
//    keys.forEach { editor.remove(it) }
//    editor.apply()
//}

// Clear ALL data from SharedPreferences
fun Context.clearAll() {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().clear().apply()
}
