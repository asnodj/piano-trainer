package dev.asnodj.pianotrainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import kotlin.math.sin
import kotlin.random.Random

/** Upper bound on simultaneously alive sparkles (a chord spawns dozens). */
private const val MAX_SPARKLES = 400

/** Sparkles spawned per key press. */
private const val SPARKLES_PER_PRESS = 16

/** How long a light beam keeps fading after its key is released, in seconds. */
private const val BEAM_RELEASE_FADE_SECONDS = 0.3f

/**
 * One rising light particle. Positions and velocities are stored as fractions
 * of the overlay size so the system is resolution-independent.
 */
private data class Sparkle(
    val bornAtNanos: Long,
    val lifeSeconds: Float,
    val startXFraction: Float,
    val driftXFractionPerSecond: Float,
    val riseFractionPerSecond: Float,
    val radiusDp: Float,
    val twinklePhase: Float,
    val color: Color,
)

/**
 * The light column rising from a held key (the signature look of piano
 * light-show videos): full while held, quick fade once released.
 */
private data class Beam(
    val note: Int,
    val bornAtNanos: Long,
    val releasedAtNanos: Long?,
    val color: Color,
)

/**
 * Rousseau-style key-press light show: while a key is held, a soft light
 * column rises from it; on the press, a burst of glowing particles shoots up
 * and twinkles out. Everything is drawn additively so overlapping lights
 * brighten each other like real light sources.
 *
 * @param pressedNotes Physical keys currently held down.
 * @param colorFor Resolves the light color of a key at press time (e.g. green
 *   for correct, red for wrong, gold in discovery).
 * @param modifier Layout modifier; lights rise from this overlay's bottom.
 */
@Composable
fun KeySparkles(
    pressedNotes: Set<Int>,
    colorFor: (Int) -> Color,
    modifier: Modifier = Modifier,
) {
    val sparkles = remember { mutableStateListOf<Sparkle>() }
    val beams = remember { mutableStateListOf<Beam>() }
    var frameTimeNanos by remember { mutableLongStateOf(0L) }
    var previousPressed by remember { mutableStateOf(emptySet<Int>()) }

    LaunchedEffect(pressedNotes) {
        val newlyPressed = pressedNotes - previousPressed
        val released = previousPressed - pressedNotes
        previousPressed = pressedNotes
        val nowNanos = System.nanoTime()

        released.forEach { note ->
            val beamIndex = beams.indexOfLast { beam -> beam.note == note && beam.releasedAtNanos == null }
            if (beamIndex >= 0) {
                beams[beamIndex] = beams[beamIndex].copy(releasedAtNanos = nowNanos)
            }
        }
        newlyPressed.forEach { note ->
            val burstColor = colorFor(note)
            beams.add(Beam(note = note, bornAtNanos = nowNanos, releasedAtNanos = null, color = burstColor))
            val keyCenterFraction = keyLeftFraction(note) + keyWidthFraction(note) / 2f
            repeat(SPARKLES_PER_PRESS) { sparkleIndex ->
                val isFastSpark = sparkleIndex % 3 == 0
                sparkles.add(
                    Sparkle(
                        bornAtNanos = nowNanos,
                        lifeSeconds = if (isFastSpark) {
                            Random.nextFloat() * 0.25f + 0.3f
                        } else {
                            Random.nextFloat() * 0.7f + 0.7f
                        },
                        startXFraction = keyCenterFraction + (Random.nextFloat() - 0.5f) * keyWidthFraction(note),
                        driftXFractionPerSecond = (Random.nextFloat() - 0.5f) * 0.025f,
                        riseFractionPerSecond = if (isFastSpark) {
                            Random.nextFloat() * 0.4f + 0.5f
                        } else {
                            Random.nextFloat() * 0.25f + 0.12f
                        },
                        radiusDp = if (isFastSpark) {
                            Random.nextFloat() * 1.5f + 1f
                        } else {
                            Random.nextFloat() * 2.5f + 2f
                        },
                        twinklePhase = Random.nextFloat() * 6.28f,
                        color = burstColor,
                    )
                )
            }
        }
        while (sparkles.size > MAX_SPARKLES) {
            sparkles.removeAt(0)
        }
    }

    // Frame loop: only runs while lights are alive, then goes fully idle.
    LaunchedEffect(sparkles.isNotEmpty() || beams.isNotEmpty()) {
        while (sparkles.isNotEmpty() || beams.isNotEmpty()) {
            withFrameNanos { nowNanos ->
                frameTimeNanos = nowNanos
                sparkles.removeAll { sparkle ->
                    (nowNanos - sparkle.bornAtNanos) / 1_000_000_000f > sparkle.lifeSeconds
                }
                beams.removeAll { beam ->
                    val releasedAt = beam.releasedAtNanos
                    releasedAt != null &&
                        (nowNanos - releasedAt) / 1_000_000_000f > BEAM_RELEASE_FADE_SECONDS
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        // Reading frameTimeNanos invalidates this draw on every animation frame.
        val nowNanos = frameTimeNanos
        beams.forEach { beam -> drawBeam(beam, nowNanos) }
        sparkles.forEach { sparkle -> drawSparkle(sparkle, nowNanos) }
    }
}

/**
 * Draws the light column of one held key: a wide soft gradient plus a narrower
 * brighter core, both additive, gently shimmering.
 */
private fun DrawScope.drawBeam(beam: Beam, nowNanos: Long) {
    val ageSeconds = (nowNanos - beam.bornAtNanos) / 1_000_000_000f
    val attack = (ageSeconds / 0.08f).coerceAtMost(1f)
    val release = beam.releasedAtNanos?.let { releasedAt ->
        1f - ((nowNanos - releasedAt) / 1_000_000_000f / BEAM_RELEASE_FADE_SECONDS).coerceIn(0f, 1f)
    } ?: 1f
    val shimmer = 0.9f + 0.1f * sin(ageSeconds * 6f)
    val intensity = attack * release * shimmer
    if (intensity <= 0f) {
        return
    }

    val keyLeft = keyLeftFraction(beam.note) * size.width
    val keyWidth = keyWidthFraction(beam.note) * size.width
    val beamHeight = size.height * 0.55f

    /**
     * One vertical gradient column, transparent at the top, brightest at the
     * keyboard edge.
     */
    fun drawColumn(width: Float, alpha: Float, color: Color) {
        val left = keyLeft + keyWidth / 2f - width / 2f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, color.copy(alpha = alpha * intensity)),
                startY = size.height - beamHeight,
                endY = size.height,
            ),
            topLeft = Offset(left, size.height - beamHeight),
            size = Size(width, beamHeight),
            blendMode = BlendMode.Plus,
        )
    }

    drawColumn(width = keyWidth * 1.8f, alpha = 0.28f, color = beam.color)
    drawColumn(width = keyWidth * 0.7f, alpha = 0.35f, color = lerp(beam.color, Color.White, 0.5f))
}

/**
 * Draws one particle as a soft radial glow with a bright white-hot core,
 * twinkling and fading as it rises.
 */
private fun DrawScope.drawSparkle(sparkle: Sparkle, nowNanos: Long) {
    val ageSeconds = (nowNanos - sparkle.bornAtNanos) / 1_000_000_000f
    val progress = (ageSeconds / sparkle.lifeSeconds).coerceIn(0f, 1f)
    val twinkle = 0.75f + 0.25f * sin(ageSeconds * 9f + sparkle.twinklePhase)
    val alpha = (1f - progress) * twinkle
    if (alpha <= 0.01f) {
        return
    }

    val center = Offset(
        x = (sparkle.startXFraction + sparkle.driftXFractionPerSecond * ageSeconds) * size.width,
        y = size.height - sparkle.riseFractionPerSecond * ageSeconds * size.height,
    )
    val coreRadius = sparkle.radiusDp * density * (1f - 0.35f * progress)
    val glowRadius = coreRadius * 3.2f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(sparkle.color.copy(alpha = alpha * 0.5f), Color.Transparent),
            center = center,
            radius = glowRadius,
        ),
        radius = glowRadius,
        center = center,
        blendMode = BlendMode.Plus,
    )
    drawCircle(
        color = lerp(sparkle.color, Color.White, 0.65f).copy(alpha = alpha),
        radius = coreRadius,
        center = center,
        blendMode = BlendMode.Plus,
    )
}
