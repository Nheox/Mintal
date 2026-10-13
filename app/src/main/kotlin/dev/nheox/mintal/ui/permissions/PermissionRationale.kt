package dev.nheox.mintal.ui.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Every OS permission Mintal can ask for, with the explanation shown *before* the system
 * prompt rather than after it (spec §9).
 *
 * Android's own dialog is one line of the system's wording and two buttons; it cannot say
 * why a focus timer wants a notification, and it certainly cannot say what still works if
 * you refuse. So each one is introduced here first, in Mintal's words, and refusing is
 * always a real option — nothing in this list is a precondition for starting a session.
 *
 * [key] is persisted in "don't show again" state and in backups, so it must stay stable.
 */
enum class MintalPermission(
    val key: String,
    val title: String,
    val why: String,
    val withoutIt: String,
    val grantLabel: String,
) {
    NOTIFICATIONS(
        key = "notifications",
        title = "A notification while a session runs",
        why = "Android only lets a timer keep running while it is showing a notification. " +
            "That notification is also where the time left, the mint you are growing and " +
            "the Give up button live once you put the phone down.",
        withoutIt = "Mintal still runs sessions, but Android is free to stop one in the " +
            "background, and you lose the live countdown outside the app.",
        grantLabel = "Allow notifications",
    ),
    USAGE_ACCESS(
        key = "usage_access",
        title = "Seeing which app is in front",
        why = "Optional, and off unless you turn it on. With Usage Access, Mintal can tell " +
            "an allowlisted app — a dictionary, a calculator — from a distraction, so " +
            "opening one does not fail your session. Mintal reads only which app is in " +
            "the foreground, and nothing leaves the device.",
        withoutIt = "Leaving Mintal at all starts the 15 second warning, whichever app you " +
            "opened. Every other part of the app is unaffected.",
        grantLabel = "Open Usage Access settings",
    ),
    BATTERY(
        key = "battery",
        title = "Not being killed mid-session",
        why = "Some phones stop background timers aggressively, whatever the notification " +
            "says. This asks Android to leave Mintal alone while a session is running. It " +
            "is requested by you, from Settings, and never to start a session.",
        withoutIt = "A long session may be cut short on those phones. On most, nothing " +
            "changes either way.",
        grantLabel = "Ask Android",
    ),
    ;

    companion object {
        fun byKey(key: String): MintalPermission? = entries.firstOrNull { it.key == key }
    }
}

/**
 * The explanation itself, without a container, so the same words appear in the first-run
 * flow, in the dialog before a request, and in Settings afterwards.
 */
@Composable
fun PermissionRationaleBody(
    permission: MintalPermission,
    modifier: Modifier = Modifier,
    hideInFuture: Boolean? = null,
    onHideInFutureChange: (Boolean) -> Unit = {},
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = permission.why, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "If you say no: ${permission.withoutIt}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hideInFuture != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = hideInFuture, onCheckedChange = onHideInFutureChange)
                Text(
                    text = "Don't show this explanation again — it stays in Settings",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

/**
 * Shown immediately before the system prompt, or on demand from Settings. [onGrant] is
 * what triggers the real request, so the two are never more than one tap apart.
 */
@Composable
fun PermissionRationaleDialog(
    permission: MintalPermission,
    onGrant: () -> Unit,
    onDismiss: () -> Unit,
    hideInFuture: Boolean? = null,
    onHideInFutureChange: (Boolean) -> Unit = {},
    grantLabel: String = permission.grantLabel,
    dismissLabel: String = "Not now",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(permission.title) },
        text = {
            PermissionRationaleBody(
                permission = permission,
                hideInFuture = hideInFuture,
                onHideInFutureChange = onHideInFutureChange,
            )
        },
        confirmButton = { TextButton(onClick = onGrant) { Text(grantLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
    )
}
