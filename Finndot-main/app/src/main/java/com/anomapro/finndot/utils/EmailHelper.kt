package com.anomapro.finndot.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.net.toUri

object EmailHelper {
    /**
     * Opens email app with only email apps in chooser
     */
    fun openEmailApp(context: Context, email: String, subject: String, body: String, chooserTitle: String = "Select Email App") {
        try {
            // Try ACTION_SENDTO with mailto: first (this should only show email apps)
            // Build mailto URI - try with query parameters first
            val mailtoUri = try {
                "mailto:$email".toUri().buildUpon()
                    .appendQueryParameter("subject", subject)
                    .appendQueryParameter("body", body)
                    .build()
            } catch (e: Exception) {
                // If building fails, use simple mailto
                Log.w("EmailHelper", "Failed to build mailto URI with params, using simple: ${e.message}")
                "mailto:$email".toUri()
            }
            
            val sendToIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = mailtoUri
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            // Check if any email app can handle this
            val emailApps = context.packageManager.queryIntentActivities(sendToIntent, PackageManager.MATCH_DEFAULT_ONLY)
            
            if (emailApps.isNotEmpty()) {
                // ACTION_SENDTO with mailto: should only show email apps
                // If only one email app, open it directly
                if (emailApps.size == 1) {
                    sendToIntent.setPackage(emailApps[0].activityInfo.packageName)
                    context.startActivity(sendToIntent)
                    Log.d("EmailHelper", "Opened email app directly: ${emailApps[0].activityInfo.packageName}")
                } else {
                    // Multiple email apps - create chooser (should only show email apps)
                    val chooserIntent = Intent.createChooser(sendToIntent, chooserTitle)
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooserIntent)
                    Log.d("EmailHelper", "Opened email chooser with ${emailApps.size} apps")
                }
            } else {
                // No email app found for ACTION_SENDTO, try ACTION_SEND as fallback
                Log.w("EmailHelper", "No email app found for ACTION_SENDTO, trying ACTION_SEND")
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                
                // Try to resolve - if it works, use it (will show chooser with all apps that handle message/rfc822)
                val resolved = context.packageManager.resolveActivity(sendIntent, PackageManager.MATCH_DEFAULT_ONLY)
                if (resolved != null) {
                    val chooserIntent = Intent.createChooser(sendIntent, chooserTitle)
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooserIntent)
                    Log.d("EmailHelper", "Used ACTION_SEND fallback")
                } else {
                    Log.e("EmailHelper", "No email app found at all")
                }
            }
        } catch (e: Exception) {
            Log.e("EmailHelper", "Error opening email app", e)
            e.printStackTrace()
        }
    }
}

