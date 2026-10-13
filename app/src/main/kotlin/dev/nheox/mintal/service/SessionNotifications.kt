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
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.session_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.session_channel_description)
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager?.createNotificationChannel(channel)
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
        const val NOTIFICATION_ID = 1001
        private const val REQUEST_OPEN = 10
        private const val REQUEST_GIVE_UP = 11
    }
}
