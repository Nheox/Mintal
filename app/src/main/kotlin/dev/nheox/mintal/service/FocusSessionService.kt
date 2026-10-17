package dev.nheox.mintal.service

import android.app.KeyguardManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
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
import dev.nheox.mintal.domain.session.AwaySignals
import dev.nheox.mintal.domain.session.DistractionPolicy
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

    /**
     * The last app the usage log saw come to the front. Sticky on purpose: the log records
     * a *switch*, not a state, so an app that has been in front for a while has no recent
     * event at all. Reading "no event in the last few seconds" as "nothing in front" meant
     * anyone who opened one app and stayed in it had their grace period cleared before it
     * could run out, and was never caught.
     */
    private var lastAppInFront: String? = null

    private val processObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            appInForeground = true
            // Whatever was in front before Mintal came back is history. If the next switch
            // away goes unrecorded, this makes it read as Mintal rather than as stale news.
            lastAppInFront = packageName
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
        notifications.clearGraceWarning()
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
        val notification = notifications.build(
            running,
            running?.speciesId?.let { speciesNames[it] },
        )
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
                val current = controller.running.value
                notifications.update(current, speciesNames[running.speciesId])
                // The one alert the app makes: the ongoing notification is deliberately
                // silent, so without this the grace period runs out behind whatever the
                // user is actually looking at.
                val grace = current?.graceRemainingSeconds
                if (grace != null) {
                    notifications.warnGracePeriod(grace, speciesNames[running.speciesId])
                } else {
                    notifications.clearGraceWarning()
                }
                delay(TICK_MILLIS)
            }
        }
    }

    /** Gathers what Android will say; [DistractionPolicy] decides what it means. */
    private fun isUserAway(): Boolean {
        val canSeeAppInFront = settings.detectForegroundApp && UsageAccess.isGranted(this)
        val screenOn = isScreenOn()
        val locked = isDeviceLocked()
        val signals = AwaySignals(
            detectionOn = settings.detectLeavingApp,
            mintalInFront = appInForeground,
            screenOn = screenOn,
            deviceLocked = locked,
            inCall = isInCall(),
            canSeeAppInFront = canSeeAppInFront,
            // The usage log is only worth reading when its answer could matter, which
            // keeps a long session with the screen off from querying it twice a second.
            appInFront = if (canSeeAppInFront && !appInForeground && screenOn && !locked) {
                currentForegroundPackage()
            } else {
                null
            },
        )
        return DistractionPolicy.isAway(signals, permitted = implicitlyAllowed + allowedPackages)
    }

    private fun isScreenOn(): Boolean {
        val power = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return power.isInteractive
    }

    /**
     * The lock screen is not another app.
     *
     * [PowerManager.isInteractive] is false only while the device is actually asleep, and
     * the keyguard is emphatically interactive — so waking the phone to glance at the
     * clock, an always-on display, and lift-to-wake all read as "the app lost the
     * foreground" and failed the session. Nothing can be done on a locked phone except
     * unlock it, so a locked device is never someone being distracted by something else.
     */
    private fun isDeviceLocked(): Boolean {
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            ?: return false
        return runCatching { keyguard.isKeyguardLocked }.getOrDefault(false)
    }

    private fun isInCall(): Boolean {
        val audio = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return audio.mode != AudioManager.MODE_NORMAL
    }

    /** The newest switch in the recent log, or the last one seen if there is none. */
    private fun currentForegroundPackage(): String? {
        val usage = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return lastAppInFront
        val now = clock.nowMillis()
        val events = runCatching { usage.queryEvents(now - FOREGROUND_LOOKBACK_MILLIS, now) }
            .getOrNull() ?: return lastAppInFront
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastAppInFront = event.packageName
            }
        }
        return lastAppInFront
    }

    /**
     * System surfaces that are always permitted (spec §5.3).
     *
     * The test is not "is this app harmless" but **is the user using something else**.
     * The lock screen, the notification shade, the recents switcher and a permission
     * dialog all appear over whatever you were doing, often without being asked for; the
     * keyboard is part of using an app rather than leaving it; a call or an alarm arrives
     * on its own schedule; and the home screen is where you are when you are not using
     * any app at all. None of those is a decision to stop focusing.
     *
     * The camera, the browser and the gallery are deliberately **not** here. Opening one
     * is a decision to use something else, which is exactly what Mintal is for noticing.
     */
    private fun resolveImplicitlyAllowedPackages(): Set<String> {
        val resolved = HashSet<String>()
        resolved += packageName

        val telecom = runCatching {
            (getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager)
                ?.defaultDialerPackage
        }.getOrNull()
        if (!telecom.isNullOrBlank()) resolved += telecom

        // Whichever keyboard is actually in use, rather than a guess at its name. The
        // stored value is a ComponentName, so the package is everything before the slash.
        val ime = runCatching {
            Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                ?.substringBefore('/')
        }.getOrNull()
        if (!ime.isNullOrBlank()) resolved += ime

        // Every installed home screen, not just the default: the user may have more than
        // one, and with no default chosen Android shows its own picker in between.
        val homes = runCatching {
            packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                PackageManager.MATCH_ALL,
            ).mapNotNull { it.activityInfo?.packageName }
        }.getOrDefault(emptyList())
        resolved += homes

        resolved += listOf(
            // The lock screen, the shade, recents, the power menu: one package, and the
            // one that made leaving the app look like leaving the app.
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.dialer",
            "com.google.android.dialer",
            // Permission and package dialogs, which Mintal itself can raise.
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            // The clock app owns alarms and timers on most devices.
            "com.android.deskclock",
            "com.google.android.deskclock",
        )
        return resolved
    }

    private suspend fun onSessionEnded() {
        notifications.clearGraceWarning()
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
