package dev.nheox.mintal.ui.settings

import android.content.Intent
import androidx.core.net.toUri

/**
 * The one place the donation address lives.
 *
 * Mintal declares no `INTERNET` permission and must not acquire one for this (spec §9):
 * the button hands a URL to whichever browser the user already has, and Mintal itself
 * never opens a socket. That also keeps the F-Droid build honest — nothing is fetched at
 * build time or at runtime.
 *
 * [URL] is deliberately empty until the owner has an address to put in it. While it is
 * empty the Settings card still renders, with the button disabled and a line saying so,
 * rather than shipping a link that goes nowhere.
 */
object Donation {

    /** A PayPal.me link, e.g. "https://www.paypal.com/paypalme/<handle>". */
    val URL: String = ""

    val isConfigured: Boolean get() = URL.isNotBlank()

    fun intent(): Intent = Intent(Intent.ACTION_VIEW, URL.toUri())
}
