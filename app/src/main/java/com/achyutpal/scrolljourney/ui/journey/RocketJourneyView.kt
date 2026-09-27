package com.achyutpal.scrolljourney.ui.journey

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.model.Planet
import com.achyutpal.scrolljourney.domain.model.RocketState
import com.achyutpal.scrolljourney.ui.common.emoji
import com.achyutpal.scrolljourney.ui.common.nameRes
import com.achyutpal.scrolljourney.ui.theme.ScrollJourneyTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val RocketSize = 32.dp

/**
 * The hero of the dashboard:
 * - the current leg (reached planet → next planet) with the rocket placed by fuel progress,
 * - the whole route Earth → Pluto underneath, reached stops lit up.
 */
@Composable
fun RocketJourneyView(
    rocket: RocketState,
    journeyStartDate: LocalDate?,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(R.string.rocket_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.rocket_fuel_label, rocket.currentFuel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = headline(rocket),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))

            val next = rocket.nextPlanet
            if (next != null) {
                CurrentLeg(from = rocket.highestReachedPlanet, to = next, progress = rocket.legProgress)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.rocket_fuel_to_next,
                        rocket.currentFuel,
                        next.fuelRequired,
                        stringResource(next.nameRes()),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                Text(stringResource(R.string.rocket_pluto_done), color = MaterialTheme.colorScheme.tertiary)
            }

            Spacer(Modifier.height(16.dp))
            RouteStrip(reached = rocket.highestReachedPlanet)

            Spacer(Modifier.height(12.dp))
            val dateFormat = remember { DateTimeFormatter.ofPattern("d MMM") }
            Text(
                text = journeyStartDate?.let { stringResource(R.string.rocket_started, it.format(dateFormat)) }
                    ?: stringResource(R.string.rocket_not_started),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.rocket_rules),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun headline(rocket: RocketState): String =
    if (rocket.highestReachedPlanet == Planet.EARTH) {
        stringResource(R.string.rocket_at_earth)
    } else {
        stringResource(
            R.string.rocket_reached,
            rocket.highestReachedPlanet.emoji(),
            stringResource(rocket.highestReachedPlanet.nameRes()),
        )
    }

/** Planet ── dashed path with the rocket on it ── next planet. */
@Composable
private fun CurrentLeg(from: Planet, to: Planet, progress: Float) {
    // Animates the rocket to its new spot whenever fuel changes.
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900),
        label = "rocketProgress",
    )
    val travelled = MaterialTheme.colorScheme.primary
    val remaining = MaterialTheme.colorScheme.surfaceVariant

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(from.emoji(), fontSize = 36.sp)
        Spacer(Modifier.width(8.dp))
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(RocketSize),
            contentAlignment = Alignment.CenterStart,
        ) {
            val trackWidth = maxWidth - RocketSize
            // Canvas = draw directly with pixels: a dashed line for the whole leg and a solid one for the travelled part.
            Canvas(Modifier.matchParentSize()) {
                val y = size.height / 2
                val rocketCentre = RocketSize.toPx() / 2 + trackWidth.toPx() * animatedProgress
                drawLine(
                    color = remaining,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                )
                drawLine(
                    color = travelled,
                    start = Offset(0f, y),
                    end = Offset(rocketCentre, y),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            Text(
                text = "🚀",
                fontSize = 24.sp,
                modifier = Modifier
                    .offset(x = trackWidth * animatedProgress)
                    .size(RocketSize)
                    .rotate(45f), // the emoji points up-right; turn it to face the next planet
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(to.emoji(), fontSize = 36.sp)
    }
}

/** Earth … Pluto; reached stops are bright, the current one is marked with a dot. */
@Composable
private fun RouteStrip(reached: Planet) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Planet.entries.forEach { planet ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = planet.emoji(),
                    fontSize = 20.sp,
                    modifier = Modifier.alpha(if (planet <= reached) 1f else 0.3f),
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(6.dp)
                        .background(
                            color = if (planet == reached) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}

@Preview
@Composable
private fun RocketJourneyPreview() {
    ScrollJourneyTheme(darkTheme = true) {
        RocketJourneyView(
            rocket = RocketState(currentFuel = 5, lifetimeFuel = 6, highestReachedPlanet = Planet.MOON),
            journeyStartDate = LocalDate.of(2026, 9, 27),
        )
    }
}
