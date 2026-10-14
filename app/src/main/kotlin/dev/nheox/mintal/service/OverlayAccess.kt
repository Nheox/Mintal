package dev.nheox.mintal.service

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

/**
 * The floating session bubble needs "Display over other apps", a special access the user
 * grants in system settings. Like Usage Access it is off by default, explained before
 * Mintal ever sends anyone there, and never required: refuse it and every other part of
 * the app behaves exactly as before (spec §9).
 */
object OverlayAccess {

    fun isGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun settingsIntent(context: Context): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        "package:${context.packageName}".toUri(),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
