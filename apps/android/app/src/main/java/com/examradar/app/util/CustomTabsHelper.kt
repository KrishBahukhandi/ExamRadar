package com.examradar.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object CustomTabsHelper {
    /**
     * Safely opens an official recruitment portal or notice link in Chrome Custom Tabs,
     * falling back to standard ACTION_VIEW intent if Custom Tabs is unavailable.
     */
    fun openOfficialUrl(context: Context, url: String) {
        if (url.isBlank()) return
        try {
            val uri = Uri.parse(url)
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(context, uri)
        } catch (e: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(browserIntent)
            } catch (_: Exception) {
                // Safely handle environments without browser
            }
        }
    }
}
