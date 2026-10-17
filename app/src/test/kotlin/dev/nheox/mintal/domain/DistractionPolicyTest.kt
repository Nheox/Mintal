package dev.nheox.mintal.domain

import dev.nheox.mintal.domain.session.AwaySignals
import dev.nheox.mintal.domain.session.DistractionPolicy
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * When leaving Mintal starts the grace period (spec §5.3). Two of these were device
 * reports of a plant dying for nothing — the lock screen, then the home screen — so each
 * exemption is pinned here by name.
 */
class DistractionPolicyTest {

    private val launcher = "com.example.launcher"
    private val camera = "com.example.camera"
    private val allowlisted = "com.example.dictionary"
    private val permitted = setOf("dev.nheox.mintal", launcher, allowlisted)

    /** On the phone, unlocked, screen on, somewhere other than Mintal. */
    private fun away(
        canSeeAppInFront: Boolean = false,
        appInFront: String? = null,
    ) = AwaySignals(
        detectionOn = true,
        mintalInFront = false,
        screenOn = true,
        deviceLocked = false,
        inCall = false,
        canSeeAppInFront = canSeeAppInFront,
        appInFront = appInFront,
    )

    private fun isAway(signals: AwaySignals) = DistractionPolicy.isAway(signals, permitted)

    @Test
    fun `being in Mintal is never away`() {
        assertFalse(isAway(away().copy(mintalInFront = true)))
    }

    @Test
    fun `the lock screen is not away, even though the screen is on`() {
        assertFalse(isAway(away().copy(deviceLocked = true)))
    }

    @Test
    fun `a dark screen is not away`() {
        assertFalse(isAway(away().copy(screenOn = false)))
    }

    @Test
    fun `a phone call is not away`() {
        assertFalse(isAway(away().copy(inCall = true)))
    }

    @Test
    fun `with detection switched off nothing is away`() {
        assertFalse(isAway(away().copy(detectionOn = false)))
    }

    @Test
    fun `without usage access the home screen cannot be told from an app, so both count`() {
        // Not a choice: Android tells Mintal it lost the foreground and nothing more.
        assertTrue(isAway(away(canSeeAppInFront = false)))
    }

    @Test
    fun `with usage access the home screen is not away`() {
        assertFalse(isAway(away(canSeeAppInFront = true, appInFront = launcher)))
    }

    @Test
    fun `with usage access the camera is away`() {
        assertTrue(isAway(away(canSeeAppInFront = true, appInFront = camera)))
    }

    @Test
    fun `with usage access an allowlisted app is not away`() {
        assertFalse(isAway(away(canSeeAppInFront = true, appInFront = allowlisted)))
    }

    @Test
    fun `with usage access but nothing recorded, the user gets the benefit of the doubt`() {
        assertFalse(isAway(away(canSeeAppInFront = true, appInFront = null)))
    }
}
