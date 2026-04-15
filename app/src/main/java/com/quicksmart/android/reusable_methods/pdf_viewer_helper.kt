package com.quicksmart.android.reusable_methods

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Opens a PDF from a URL (e.g. Firebase Storage download URL).
 *
 * Strategy: pass the URL through Google Docs Viewer, which opens in
 * the browser — works for any https:// URL without needing a PDF app.
 *
 * Usage:
 *   context.openPdf(item.aadhaarUrl)
 */
fun Context.openPdf(url: String) {
    if (url.isBlank()) {
        Toast.makeText(this, "Document not available", Toast.LENGTH_SHORT).show()
        return
    }

    val viewerUrl = "https://docs.google.com/gview?embedded=true&url=${Uri.encode(url)}"

    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(viewerUrl)))
    } catch (e: Exception) {
        Toast.makeText(this, "No browser found to open document", Toast.LENGTH_SHORT).show()
    }
}
