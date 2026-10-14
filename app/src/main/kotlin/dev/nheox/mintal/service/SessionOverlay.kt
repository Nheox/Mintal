package dev.nheox.mintal.service

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nheox.mintal.MainActivity
import dev.nheox.mintal.R
import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.domain.session.RunningSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * A small floating bubble over whatever app is in front, carrying the time left and the
 * grace countdown (spec §5.2, §5.3).
 *
 * The notification already carries both, but a notification is behind a swipe: the one
 * moment the number matters most is the fifteen seconds after you have left Mintal, and
 * that is exactly the moment it is not on screen. The bubble puts it in front of you, and
 * tapping it is one tap back into the session.
 *
 * Plain Android views rather than Compose. A `ComposeView` in a window manager window has
 * to be given a lifecycle, a saved-state registry and a view-model store by hand before
 * it will draw at all, which is a great deal of machinery for one line of text — and none
 * of it can be rendered by the screenshot tests either way.
 *
 * Nothing here asks for a permission. [OverlayAccess] is checked on every show, so
 * revoking the access mid-session simply stops the bubble appearing.
 */
@Singleton
class SessionOverlay @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val windowManager: WindowManager? =
        context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    private var root: View? = null
    private var label: TextView? = null
    private var leaf: ImageView? = null
    private var bubble: GradientDrawable? = null
    private var layout: WindowManager.LayoutParams? = null

    private var shownDark = false
    private var shownAlerting: Boolean? = null
    private var shownText: String? = null

    // Where the user last dragged it. Kept for the life of the process so the bubble
    // comes back where it was left rather than jumping to the corner every session.
    private var restingX: Int? = null
    private var restingY: Int? = null

    fun show(running: RunningSession, darkTheme: Boolean) {
        if (!OverlayAccess.isGranted(context)) {
            hide()
            return
        }
        if (root != null && shownDark != darkTheme) hide()
        if (root == null && !attach(darkTheme)) return

        val alerting = running.inGracePeriod
        if (alerting != shownAlerting) {
            paint(alerting, darkTheme)
            shownAlerting = alerting
        }
        val text = textFor(running)
        if (text != shownText) {
            label?.text = text
            root?.contentDescription = context.getString(R.string.overlay_description, text)
            shownText = text
        }
    }

    fun hide() {
        val view = root ?: return
        layout?.let {
            restingX = it.x
            restingY = it.y
        }
        runCatching { windowManager?.removeView(view) }
        root = null
        label = null
        leaf = null
        bubble = null
        layout = null
        shownAlerting = null
        shownText = null
    }

    private fun textFor(running: RunningSession): String {
        val grace = running.graceRemainingSeconds
        return when {
            grace != null -> context.getString(R.string.overlay_grace, grace)
            running.mode == SessionMode.COUNTDOWN ->
                MintalTime.formatClock(running.remainingSeconds)

            else -> MintalTime.formatClock(running.elapsedSeconds)
        }
    }

    /** False — and nothing left behind — if the window could not be added. */
    private fun attach(darkTheme: Boolean): Boolean {
        val manager = windowManager ?: return false
        val shape = GradientDrawable().apply {
            cornerRadius = dp(22f).toFloat()
        }
        val icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_notification_leaf)
            layoutParams = LinearLayout.LayoutParams(dp(18f), dp(18f)).apply {
                marginEnd = dp(8f)
            }
        }
        val text = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), dp(9f), dp(14f), dp(9f))
            background = shape
            addView(icon)
            addView(text)
            setOnClickListener { openMintal() }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Not focusable, so the bubble never takes the keyboard or the back button
            // away from the app underneath it. It is still touchable: that is the point.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = restingX ?: dp(16f)
            y = restingY ?: dp(96f)
        }

        row.setOnTouchListener(DragListener(params, manager))

        if (!runCatching { manager.addView(row, params) }.isSuccess) return false

        root = row
        label = text
        leaf = icon
        bubble = shape
        layout = params
        shownDark = darkTheme
        paint(alerting = false, darkTheme = darkTheme)
        shownAlerting = false
        return true
    }

    /**
     * Two states, and the alerting one is deliberately loud: the bubble turns the error
     * colour for the fifteen seconds in which coming back still saves the session.
     */
    private fun paint(alerting: Boolean, darkTheme: Boolean) {
        val surface = colour(
            when {
                alerting && darkTheme -> R.color.mintal_overlay_alert_dark
                alerting -> R.color.mintal_overlay_alert_light
                darkTheme -> R.color.mintal_overlay_surface_dark
                else -> R.color.mintal_overlay_surface_light
            },
        )
        val ink = colour(
            when {
                alerting && darkTheme -> R.color.mintal_overlay_on_alert_dark
                alerting -> R.color.mintal_overlay_on_alert_light
                darkTheme -> R.color.mintal_overlay_on_surface_dark
                else -> R.color.mintal_overlay_on_surface_light
            },
        )
        val outline = colour(
            if (darkTheme) R.color.mintal_overlay_outline_dark else R.color.mintal_overlay_outline_light,
        )
        bubble?.apply {
            setColor(surface)
            setStroke(dp(1.5f).coerceAtLeast(1), outline)
        }
        label?.setTextColor(ink)
        leaf?.imageTintList = ColorStateList.valueOf(ink)
    }

    private fun openMintal() {
        // Starting an activity from the background is restricted from Android 10, but an
        // app holding the overlay access is exempt — which is the only case we are in.
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        runCatching { context.startActivity(intent) }
    }

    private fun colour(id: Int): Int = ContextCompat.getColor(context, id)

    private fun dp(value: Float): Int =
        (value * context.resources.displayMetrics.density).roundToInt()

    /**
     * Drag to move, tap to open. The slop check is what keeps the two apart: without it
     * every tap travels a pixel or two and opens the app from under the finger.
     */
    private inner class DragListener(
        private val params: WindowManager.LayoutParams,
        private val manager: WindowManager,
    ) : View.OnTouchListener {

        private val slop = ViewConfiguration.get(context).scaledTouchSlop
        private var downX = 0f
        private var downY = 0f
        private var originX = 0
        private var originY = 0
        private var dragging = false

        override fun onTouch(view: View, event: MotionEvent): Boolean = when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                originX = params.x
                originY = params.y
                dragging = false
                true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (!dragging && hypot(dx, dy) > slop) dragging = true
                if (dragging) {
                    val metrics = context.resources.displayMetrics
                    // Clamped so the bubble can never be dragged off the screen and
                    // left somewhere it cannot be dragged back from.
                    params.x = (originX + dx.toInt())
                        .coerceIn(0, (metrics.widthPixels - view.width).coerceAtLeast(0))
                    params.y = (originY + dy.toInt())
                        .coerceIn(0, (metrics.heightPixels - view.height).coerceAtLeast(0))
                    runCatching { manager.updateViewLayout(view, params) }
                }
                true
            }

            MotionEvent.ACTION_UP -> {
                if (!dragging) view.performClick()
                restingX = params.x
                restingY = params.y
                true
            }

            else -> false
        }
    }
}
