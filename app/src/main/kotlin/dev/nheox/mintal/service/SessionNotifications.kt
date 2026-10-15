package dev.nheox.mintal.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nheox.mintal.MainActivity
import dev.nheox.mintal.R
import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.domain.session.RunningSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The persistent session notification (spec §3.1 item 9): live time, a Give Up action, and
 * the grace-period warning that has to be visible before a session is failed (spec §5.3).
 */
@Singleton
class SessionNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager: NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    fun ensureChannel() {
        val session = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.session_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.session_channel_description)
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        // A second channel, and it has to be a second one: Android fixes a channel's
        // importance when it is created and an app may only ever lower it afterwards.
        // The running session wants to sit quietly in the shade, which is what makes the
        // grace warning invisible — the fifteen seconds in which coming back still saves
        // the session arrived silently, in the one place the user was not looking.
        val warning = NotificationChannel(
            WARNING_CHANNEL_ID,
            context.getString(R.string.warning_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.warning_channel_description)
            setShowBadge(false)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        manager?.createNotificationChannel(session)
        manager?.createNotificationChannel(warning)
    }

    /**
     * The heads-up that the session is about to be lost. Separate from the ongoing
     * notification rather than a change to it, because the ongoing one is bound to the
     * quiet channel for as long as it exists.
     *
     * `setOnlyAlertOnce` is what makes the countdown bearable: the first post pops and
     * sounds, and the twice-a-second updates after it change the number in place.
     */
    fun warnGracePeriod(secondsLeft: Int, speciesName: String?) {
        val notification = NotificationCompat.Builder(context, WARNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_leaf)
            .setContentTitle(context.getString(R.string.notification_grace_warning))
            .setContentText(
                context.getString(R.string.notification_grace_seconds, secondsLeft) +
                    (speciesName?.let { " · $it" } ?: ""),
            )
            .setContentIntent(openAppIntent())
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
        runCatching { manager?.notify(WARNING_NOTIFICATION_ID, notification) }
    }

    /** Called when the user comes back, and when the session ends either way. */
    fun clearGraceWarning() {
        runCatching { manager?.cancel(WARNING_NOTIFICATION_ID) }
    }

    fun build(running: RunningSession?, speciesName: String?): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_leaf)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                0,
                context.getString(R.string.notification_give_up),
                giveUpIntent(),
            )

        if (running == null) {
            return builder
                .setContentTitle(context.getString(R.string.notification_session_running))
                .build()
        }

        val grace = running.graceRemainingSeconds
        if (grace != null) {
            builder
                .setContentTitle(context.getString(R.string.notification_grace_warning))
                .setContentText("${grace}s left to come back")
        } else {
            val clock = if (running.mode == SessionMode.COUNTDOWN) {
                MintalTime.formatClock(running.remainingSeconds) + " remaining"
            } else {
                MintalTime.formatClock(running.elapsedSeconds) + " elapsed"
            }
            builder
                .setContentTitle(clock)
                .setContentText(speciesName ?: context.getString(R.string.notification_session_running))
        }

        if (running.mode == SessionMode.COUNTDOWN && running.plannedSeconds > 0) {
            builder.setProgress(running.plannedSeconds, running.elapsedSeconds, false)
        }
        return builder.build()
    }

    fun update(running: RunningSession?, speciesName: String?) {
        manager?.notify(NOTIFICATION_ID, build(running, speciesName))
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun giveUpIntent(): PendingIntent = PendingIntent.getService(
        context,
        REQUEST_GIVE_UP,
        FocusSessionService.giveUpIntent(context),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val CHANNEL_ID = "mintal_session"
        const val WARNING_CHANNEL_ID = "mintal_warning"
        const val NOTIFICATION_ID = 1001
        const val WARNING_NOTIFICATION_ID = 1002
        private const val REQUEST_OPEN = 10
        private const val REQUEST_GIVE_UP = 11
    }
}
