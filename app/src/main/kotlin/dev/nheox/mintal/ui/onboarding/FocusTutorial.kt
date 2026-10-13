package dev.nheox.mintal.ui.onboarding

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.domain.grove.PlantBuilder
import dev.nheox.mintal.ui.components.PlantColours
import dev.nheox.mintal.ui.components.drawPlant
import dev.nheox.mintal.ui.theme.mintalPalette

/**
 * The whole app in three beats: set a length, put the phone down, watch it grow.
 *
 * Drawn rather than played. A video or GIF would be a megabyte of asset, and Mintal
 * downloads nothing at runtime and ships nothing it cannot build from source (spec §9),
 * so the tutorial is the same Compose canvas and the same [drawPlant] the Grove uses —
 * which also means it can never drift from what the app actually does.
 */
private const val BEATS = 3
private const val BEAT_MILLIS = 2_600

private val CAPTIONS = listOf(
    "Set how long you want to focus",
    "Put the phone down and leave it alone",
    "The session is planted in your Grove",
)

@Composable
fun FocusTutorial(
    species: Species?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Someone who has turned animations off system-wide has asked not to be shown one.
    val animated = remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f
        }.getOrDefault(true)
    }

    if (!animated) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CAPTIONS.forEachIndexed { index, caption ->
                Text(
                    text = "${index + 1}. $caption",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "tutorial")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = BEATS.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(BEAT_MILLIS * BEATS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "beat",
    )
    FocusTutorialFrame(species = species, progress = progress, modifier = modifier)
}

/**
 * One frame of the tutorial, at [progress] in 0f..3f. Stateless on purpose: an infinite
 * animation never lets Robolectric's main looper go idle, so the screenshot renderer
 * would hang on the drawing rather than draw it. This is also what the reduced-motion
 * path and any preview would use.
 */
@Composable
fun FocusTutorialFrame(
    species: Species?,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val beat = progress.toInt().coerceIn(0, BEATS - 1)
    val within = (progress - beat).coerceIn(0f, 1f)
    val palette = mintalPalette
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "How it works: " + CAPTIONS.joinToString(", then ")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(132.dp)) {
            when (beat) {
                0 -> drawSettingTheTimer(within, accent, ink)
                1 -> drawPuttingThePhoneDown(within, accent, ink)
                else -> drawTheMintGrowing(within, species, palette)
            }
        }

        Text(
            text = CAPTIONS[beat],
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        // Three dots, so it reads as a sequence rather than a single looping doodle.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(BEATS) { index ->
                Canvas(modifier = Modifier.height(8.dp).padding(top = 2.dp).fillMaxWidth(0.02f)) {
                    drawCircle(
                        color = if (index == beat) accent else ink.copy(alpha = 0.3f),
                        radius = size.minDimension.coerceAtLeast(3f),
                    )
                }
            }
        }
    }
}

/** The growth track from the hub, with the bud marker sliding out to a length. */
private fun DrawScope.drawSettingTheTimer(within: Float, accent: Color, ink: Color) {
    val trackY = size.height * 0.55f
    val left = size.width * 0.2f
    val right = size.width * 0.8f
    val eased = ease(within.coerceAtMost(0.75f) / 0.75f)

    drawLine(
        color = ink.copy(alpha = 0.35f),
        start = Offset(left, trackY),
        end = Offset(right, trackY),
        strokeWidth = size.height * 0.06f,
        cap = StrokeCap.Round,
    )
    for (step in 0..4) {
        val x = left + (right - left) * step / 4f
        drawLine(
            color = ink.copy(alpha = 0.3f),
            start = Offset(x, trackY - size.height * 0.08f),
            end = Offset(x, trackY + size.height * 0.08f),
            strokeWidth = 2f,
        )
    }
    val budX = left + (right - left) * eased
    drawLine(
        color = accent,
        start = Offset(left, trackY),
        end = Offset(budX, trackY),
        strokeWidth = size.height * 0.06f,
        cap = StrokeCap.Round,
    )
    drawCircle(color = accent, radius = size.height * 0.11f, center = Offset(budX, trackY))
}

/** A phone tipping face down, and the screen going dark as it goes. */
private fun DrawScope.drawPuttingThePhoneDown(within: Float, accent: Color, ink: Color) {
    val eased = ease(within.coerceAtMost(0.7f) / 0.7f)
    val phoneWidth = size.height * 0.34f
    val phoneHeight = size.height * 0.62f
    val baseX = size.width / 2f
    val baseY = size.height * 0.82f

    drawLine(
        color = ink.copy(alpha = 0.3f),
        start = Offset(size.width * 0.25f, baseY),
        end = Offset(size.width * 0.75f, baseY),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
    )

    rotateRad(radians = eased * 1.22f, pivot = Offset(baseX, baseY)) {
        val topLeft = Offset(baseX - phoneWidth / 2f, baseY - phoneHeight)
        drawRoundRect(
            color = ink.copy(alpha = 0.18f + 0.3f * eased),
            topLeft = topLeft,
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(phoneWidth * 0.18f, phoneWidth * 0.18f),
        )
        drawRoundRect(
            color = ink.copy(alpha = 0.6f),
            topLeft = topLeft,
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(phoneWidth * 0.18f, phoneWidth * 0.18f),
            style = Stroke(width = 3f),
        )
        // The running session's bar, still going as the phone goes down.
        val barInset = phoneWidth * 0.16f
        drawRoundRect(
            color = accent.copy(alpha = 1f - 0.5f * eased),
            topLeft = Offset(topLeft.x + barInset, baseY - phoneHeight * 0.58f),
            size = Size((phoneWidth - 2 * barInset) * 0.62f, phoneHeight * 0.1f),
            cornerRadius = CornerRadius(phoneHeight * 0.05f, phoneHeight * 0.05f),
        )
    }
}

/** The real plant, gaining the node the session just bought. */
private fun DrawScope.drawTheMintGrowing(
    within: Float,
    species: Species?,
    palette: dev.nheox.mintal.ui.theme.MintalPalette,
) {
    if (species == null) return
    val nodes = (1 + (within * 4f).toInt()).coerceIn(1, 5)
    val plant = PlantBuilder.grow(
        speciesId = species.id,
        plantIndex = 0,
        sessionIds = (1L..nodes.toLong()).toList(),
        habit = species.art.habit,
        seed = 11L,
    )
    val unit = minOf(
        size.height / (-plant.minY + 1.4f),
        size.width / ((plant.maxX - plant.minX) + 3f),
    )
    drawPlant(
        plant = plant,
        origin = Offset(size.width / 2f, size.height * 0.94f),
        unitPx = unit,
        species = species,
        colours = PlantColours.of(palette, species),
        showFlowers = false,
    )
}

/** Smoothstep, so nothing starts or stops with a jolt. */
private fun ease(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}
