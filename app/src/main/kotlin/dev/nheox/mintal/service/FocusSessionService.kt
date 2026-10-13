package dev.nheox.mintal.service

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.AndroidEntryPoint
import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.time.MintalClock
import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.data.prefs.MintalSettings
import dev.nheox.mintal.data.repository.AllowlistRepository
import dev.nheox.mintal.data.species.SpeciesCatalog
import dev.nheox.mintal.domain.session.SessionController
import dev.nheox.mintal.domain.session.SessionRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps the session alive (spec §5.2) and owns distraction detection (spec §5.3).
 *
 * The service only decides *whether the user is away*; failing the session is the
 * controller's job. Nothing here asks for a permission: layer A is plain app lifecycle,
 * and layer B is used only if the user has already granted Usage Access.
 */
@AndroidEntryPoint
class FocusSessionService : Service() {

    @Inject lateinit var controller: SessionController

    @Inject lateinit var notifications: SessionNotifications

    @Inject lateinit var preferences: MintalPreferences

    @Inject lateinit var allowlistRepository: AllowlistRepository

    @Inject lateinit var speciesCatalog: SpeciesCatalog

    @Inject lateinit var clock: MintalClock

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loopJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var appInForeground = true
    private var settings = MintalSettings()
    private var allowedPackages: Set<String> = emptySet()
    private var speciesNames: Map<String, String> = emptyMap()
    private var implicitlyAllowed: Set<String> = emptySet()

    private val processObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            appInForeground = true
        }

        override fun onStop(owner: LifecycleOwner) {
            appInForeground = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        notifications.ensureChannel()
        ProcessLifecycleOwner.get().lifecycle.addObserver(processObserver)
        implicitlyAllowed = resolveImplicitlyAllowedPackages()
        serviceScope.launch { preferences.settings.collect { settings = it } }
        serviceScope.launch { allowlistRepository.observePackages().collect { allowedPackages = it } }
        serviceScope.launch {
            speciesNames = speciesCatalog.all().associate { it.id to it.commonName }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_GIVE_UP -> serviceScope.launch { controller.giveUp() }
            ACTION_STOP_COUNT_UP -> serviceScope.launch { controller.stopCountUp() }
            else -> if (!controller.isRunning) stopNow()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        loopJob?.cancel()
        releaseWakeLock()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handleStart(intent: Intent) {
        val mode = runCatching {
            SessionMode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: SessionMode.COUNTDOWN.name)
        }.getOrDefault(SessionMode.COUNTDOWN)
        val plannedSeconds = intent.getIntExtra(
            EXTRA_PLANNED_SECONDS,
            SessionRules.DEFAULT_MINUTES * 60,
        )
        val speciesId = intent.getStringExtra(EXTRA_SPECIES_ID) ?: "spicata"
        val tagId = intent.getLongExtra(EXTRA_TAG_ID, -1L).takeIf { it >= 0L }

        if (!controller.isRunning) {
            controller.start(mode, plannedSeconds, speciesId, tagId)
        }

        goForeground()
        acquireWakeLock()
        startLoop()
    }

    private fun goForeground() {
        val running = controller.running.value
        val notification = notifications.build(running, speciesNames[running?.speciesId])
        val type = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            SessionNotifications.NOTIFICATION_ID,
            notification,
            type,
        )
    }

    private fun startLoop() {
        if (loopJob?.isActive == true) return
        loopJob = serviceScope.launch {
            while (isActive) {
                val running = controller.running.value
                if (running == null) {
                    onSessionEnded()
                    return@launch
                }
                controller.setDistracted(isUserAway())
                controller.tick()
                notifications.update(
                    controller.running.value,
                    speciesNames[running.speciesId],
                )
                delay(TICK_MILLIS)
            }
        }
    }

    /**
     * Layer A is enough on its own (spec §5.3): the app lost the foreground. Layer B, when
     * the user has granted it, keeps an allowlisted app from starting the countdown at all.
     * A dark screen and a phone call never count as leaving.
     */
    private fun isUserAway(): Boolean {
        if (!settings.detectLeavingApp) return false
        if (appInForeground) return false
        if (!isScreenOn()) return false
        if (isInCall()) return false
        if (settings.detectForegroundApp && UsageAccess.isGranted(this)) {
            val foreground = currentForegroundPackage()
            if (foreground == null ||
                foreground == packageName ||
                foreground in allowedPackages ||
                foreground in implicitlyAllowed
            ) {
                return false
            }
        }
        return true
    }

    private fun isScreenOn(): Boolean {
        val power = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return power.isInteractive
    }

    private fun isInCall(): Boolean {
        val audio = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return audio.mode != AudioManager.MODE_NORMAL
    }

    private fun currentForegroundPackage(): String? {
        val usage = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        val now = clock.nowMillis()
        val events = runCatching { usage.queryEvents(now - FOREGROUND_LOOKBACK_MILLIS, now) }
            .getOrNull() ?: return null
        var latest: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                latest = event.packageName
            }
        }
        return latest
    }

    /** The system dialler and system settings are always permitted (spec §5.3). */
    private fun resolveImplicitlyAllowedPackages(): Set<String> {
        val resolved = HashSet<String>()
        resolved += packageName
        val telecom = runCatching {
            (getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager)
                ?.defaultDialerPackage
        }.getOrNull()
        if (!telecom.isNullOrBlank()) resolved += telecom
        resolved += listOf(
            "com.android.settings",
            "com.android.phone",
            "com.android.dialer",
            "com.google.android.dialer",
        )
        return resolved
    }

    private suspend fun onSessionEnded() {
        if (settings.vibrateOnFinish) vibrateOnce()
        releaseWakeLock()
        stopNow()
    }

    private fun stopNow() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun vibrateOnce() {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        runCatching {
            vibrator?.vibrate(VibrationEffect.createOneShot(220L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val power = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            // Never longer than the longest possible session, so a stuck session cannot
            // hold the CPU awake indefinitely.
            acquire((SessionRules.MAX_SECONDS + 300) * 1000L)
        }
    }

    private fun releaseWakeLock() {
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
    }

    companion object {
        private const val TICK_MILLIS = 500L
        private const val FOREGROUND_LOOKBACK_MILLIS = 10_000L
        private const val WAKE_LOCK_TAG = "mintal:session"

        const val ACTION_START = "dev.nheox.mintal.action.START"
        const val ACTION_GIVE_UP = "dev.nheox.mintal.action.GIVE_UP"
        const val ACTION_STOP_COUNT_UP = "dev.nheox.mintal.action.STOP_COUNT_UP"

        private const val EXTRA_MODE = "mode"
        private const val EXTRA_PLANNED_SECONDS = "planned_seconds"
        private const val EXTRA_SPECIES_ID = "species_id"
        private const val EXTRA_TAG_ID = "tag_id"

        fun startIntent(
            context: Context,
            mode: SessionMode,
            plannedSeconds: Int,
            speciesId: String,
            tagId: Long?,
        ): Intent = Intent(context, FocusSessionService::class.java).apply {
            action = ACTION_START
            putExtra(EXTRA_MODE, mode.name)
            putExtra(EXTRA_PLANNED_SECONDS, plannedSeconds)
            putExtra(EXTRA_SPECIES_ID, speciesId)
            putExtra(EXTRA_TAG_ID, tagId ?: -1L)
        }

        fun giveUpIntent(context: Context): Intent =
            Intent(context, FocusSessionService::class.java).apply { action = ACTION_GIVE_UP }

        fun stopCountUpIntent(context: Context): Intent =
            Intent(context, FocusSessionService::class.java).apply { action = ACTION_STOP_COUNT_UP }

        fun start(
            context: Context,
            mode: SessionMode,
            plannedSeconds: Int,
            speciesId: String,
            tagId: Long?,
        ) {
            ContextCompat.startForegroundService(
                context,
                startIntent(context, mode, plannedSeconds, speciesId, tagId),
            )
        }
    }
}
