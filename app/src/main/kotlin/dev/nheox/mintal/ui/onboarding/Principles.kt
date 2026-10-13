package dev.nheox.mintal.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nheox.mintal.ui.components.MintalTopBar
import dev.nheox.mintal.ui.components.SectionCard

/** The line under the name, wherever the name appears. */
const val MINTAL_SLOGAN = "Stay focused, stay mintalized"

/**
 * What Mintal is and what it will not do. These three claims are the product, not a
 * welcome screen, so they are shown at **every** launch until the reader says they have
 * read them — and they stay in Settings afterwards, because a promise you can no longer
 * find is not much of a promise.
 */
@Composable
fun PrinciplesBody() {
    // No preamble: the tutorial slide before this one has just said what the app does,
    // and the three cards are the promises. Saying it twice is how the page became a
    // wall in the first place.
    SectionCard(title = "It stays on this device") {
        Text(
            text = "No account, no server, no analytics, no ads, nothing for sale. " +
                "Mintal has no internet permission at all. You can export or delete " +
                "everything whenever you want.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    SectionCard(title = "What it will ask for") {
        Text(
            text = "A notification, so the timer can keep running and show the time " +
                "left. Nothing else is required, and each permission is explained " +
                "before it is requested — in Settings too, whenever you want to read " +
                "it again.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    SectionCard(title = "If you leave mid-session") {
        Text(
            text = "You get a 15 second warning first, and the session is only then " +
                "recorded as failed — with the time you did manage. A dark screen or " +
                "a phone call never counts as leaving. Nothing here takes over your " +
                "phone, and you can always exit.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * The principles, kept where they can always be found: Settings → Mintal's principles.
 * The first-run deck is where they are first read; nothing makes anyone read them twice.
 */
@Composable
fun PrinciplesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { MintalTopBar(title = "Mintal's principles", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = MINTAL_SLOGAN,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            PrinciplesBody()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
