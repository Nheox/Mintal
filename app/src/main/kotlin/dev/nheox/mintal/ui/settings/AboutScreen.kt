package dev.nheox.mintal.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nheox.mintal.BuildConfig
import dev.nheox.mintal.ui.components.MintalTopBar
import dev.nheox.mintal.ui.components.SectionCard
import dev.nheox.mintal.ui.components.StatLine

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(topBar = { MintalTopBar(title = "About", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard(title = "Mintal") {
                StatLine("Version", BuildConfig.VERSION_NAME)
                StatLine("Licence", "GPL-3.0")
                StatLine("Network permission", "none")
            }

            SectionCard(title = "What this app does not do") {
                Text(
                    text = "No account, no server of ours, no analytics, no crash reporting, " +
                        "no ads, nothing for sale. The app holds no INTERNET permission at " +
                        "all, so it cannot send your data anywhere even by accident — you " +
                        "can check that in the manifest of the source it was built from.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SectionCard(title = "Prior art") {
                Text(
                    text = "A focus timer with a visual reward loop is a well-established " +
                        "category. Mintal is an independent implementation written from a " +
                        "behavioural specification; no code, assets, text or branding from " +
                        "any existing app is used. The duration picker, the session bar and " +
                        "the Grove were each chosen to differ from the best-known app in " +
                        "the category. See docs/PRIOR_ART.md in the source repository.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SectionCard(title = "How this was built") {
                Text(
                    text = "Mintal was written with Claude, Anthropic's coding " +
                        "assistant, from a written specification. The decisions, the " +
                        "testing and the judgement are the author's; the code was " +
                        "written by Claude under that direction. Every commit in the " +
                        "repository records it.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SectionCard(title = "The mints") {
                Text(
                    text = "Every plant in Mintal is a real member of the genus Mentha, with " +
                        "a one-line botanical note. Species are told apart by leaf shape, " +
                        "leaf margin and growth habit — the art is drawn from those traits, " +
                        "not from a palette swap.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
