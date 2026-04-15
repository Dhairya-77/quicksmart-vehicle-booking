package com.quicksmart.android.reusable_methods

import android.content.Context
import androidx.appcompat.app.AlertDialog

// 1. Simple info dialog — single OK button
fun Context.showInfoDialog(
    title       : String,
    message     : String,
    buttonLabel : String = "OK",
    cancelable  : Boolean = true,
    onDismiss   : (() -> Unit)? = null
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setCancelable(cancelable)
        .setPositiveButton(buttonLabel) { dialog, _ ->
            dialog.dismiss()
            onDismiss?.invoke()
        }
        .show()
}


// 2. Confirm dialog — OK + Cancel, result returned via lambda
fun Context.showConfirmDialog(
    title         : String,
    message       : String,
    positiveLabel : String = "Yes",
    negativeLabel : String = "No",
    cancelable    : Boolean = true,
    onResult      : (confirmed: Boolean) -> Unit
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setCancelable(cancelable)
        .setPositiveButton(positiveLabel) { dialog, _ ->
            dialog.dismiss()
            onResult(true)
        }
        .setNegativeButton(negativeLabel) { dialog, _ ->
            dialog.dismiss()
            onResult(false)
        }
        .show()
}

// 3. Full custom dialog — separate positive / negative callbacks
fun Context.showDialog(
    title         : String,
    message       : String,
    positiveLabel : String = "OK",
    negativeLabel : String? = null,
    cancelable    : Boolean = true,
    onPositive    : () -> Unit = {},
    onNegative    : (() -> Unit)? = null
) {
    val builder = AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setCancelable(cancelable)
        .setPositiveButton(positiveLabel) { dialog, _ ->
            dialog.dismiss()
            onPositive()
        }

    if (negativeLabel != null) {
        builder.setNegativeButton(negativeLabel) { dialog, _ ->
            dialog.dismiss()
            onNegative?.invoke()
        }
    }

    builder.show()
}

// 4. Permission rationale dialog (non-cancelable by default)
fun Context.showPermissionDialog(
    title         : String,
    message       : String,
    onAllow       : () -> Unit,
    onDeny        : () -> Unit = {}
) {
    showDialog(
        title         = title,
        message       = message,
        positiveLabel = "Allow",
        negativeLabel = "Cancel",
        cancelable    = false,
        onPositive    = onAllow,
        onNegative    = onDeny
    )
}


// 5. Settings redirect dialog
fun Context.showSettingsDialog(
    title         : String,
    message       : String,
    positiveLabel : String = "Open Settings",
    negativeLabel : String = "Cancel",
    onOpenSettings: () -> Unit,
    onCancel      : () -> Unit = {}
) {
    showDialog(
        title         = title,
        message       = message,
        positiveLabel = positiveLabel,
        negativeLabel = negativeLabel,
        cancelable    = false,
        onPositive    = onOpenSettings,
        onNegative    = onCancel
    )
}
