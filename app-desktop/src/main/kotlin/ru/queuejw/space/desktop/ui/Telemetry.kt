/*
 * Desktop Telemetry overlay matching Android's telemetry: autopilot panel and catalog text.
 */
package ru.queuejw.space.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.queuejw.space.game.*
import java.util.Locale

private fun getSystemDesignation(universe: Universe): String = "UDC-${'$'}{universe.randomSeed % 100_000}"

@Composable
fun Telemetry(universe: Universe, showControls: Boolean, mcpMode: Boolean = false) {
    var topVisible by remember { mutableStateOf(false) }
    var bottomVisible by remember { mutableStateOf(false) }

    var catalogFontSize by remember { mutableStateOf(9.sp) }

    val textStyle =
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            lineHeight = 12.sp,
        )

    LaunchedEffect(Unit) {
        // Simple stagger like Android's flickerFadeInAfterDelay
        topVisible = true
        bottomVisible = true
    }

    val explored = universe.planets.filter { it.explored }

    val recomposeScope = currentRecomposeScope
    Telescope(universe) { recomposeScope.invalidate() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        // MCP mode indicator in top-right corner
        if (mcpMode) {
            AnimatedVisibility(
                visible = topVisible,
                modifier = Modifier.align(Alignment.TopEnd),
                enter = flickerFadeIn
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Colors.Autopilot.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    androidx.compose.material3.Text(
                        text = "MCP MODE",
                        style = textStyle.copy(fontSize = 10.sp),
                        color = Colors.Autopilot
                    )
                }
            }
        }

        val wide = maxWidth > maxHeight
        Column(
            modifier =
                Modifier
                    .align(if (wide) Alignment.BottomEnd else Alignment.BottomStart)
                    .fillMaxWidth(if (wide) 0.45f else 1.0f)
        ) {
            val autopilotEnabled = universe.ship.autopilot?.enabled == true
            if (autopilotEnabled) {
                universe.ship.autopilot?.let { autopilot ->
                    AnimatedVisibility(visible = bottomVisible, enter = flickerFadeIn) {
                        androidx.compose.material3.Text(
                            style = textStyle,
                            color = Colors.Autopilot,
                            modifier = Modifier.align(Alignment.Start),
                            text = autopilot.telemetry,
                        )
                    }
                }
            }

            Row(modifier = Modifier.padding(top = 6.dp)) {
                AnimatedVisibility(
                    modifier = Modifier.weight(1f),
                    visible = bottomVisible,
                    enter = flickerFadeIn,
                ) {
                    androidx.compose.material3.Text(
                        style = textStyle,
                        color = Colors.Console,
                        text = with(universe.ship) {
                            val closest = universe.closestPlanet()
                            val distToClosest = ((closest.pos - pos).mag() - closest.radius).toInt()
                            listOfNotNull(
                                landing?.let {
                                    "LND: ${it.planet.name.uppercase()}\n" +
                                        "JOB: ${it.text.uppercase()}"
                                } ?: if (distToClosest < 10_000) {
                                    "ALT: $distToClosest"
                                } else null,
                                "HULL: ${hull.toInt()}/${hullCapacity.toInt()}",
                                "FUEL: ${fuel.toInt()}/${fuelCapacity.toInt()}",
                                "THR: %.0f%%".format(thrust.mag() * 100f),
                                "POS: %s".format(pos.str("%+7.0f")),
                                "VEL: %.0f".format(velocity.mag()),
                            ).joinToString("\n")
                        },
                    )
                }

                if (showControls) {
                    AnimatedVisibility(visible = bottomVisible, enter = flickerFadeInAfterDelay(500)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ConsoleButton(
                                textStyle = textStyle,
                                color = Colors.Console,
                                bgColor = if (autopilotEnabled) Colors.Autopilot else androidx.compose.ui.graphics.Color.Transparent,
                                borderColor = Colors.Console,
                                text = "AUTO",
                            ) {
                                universe.ship.autopilot?.let {
                                    it.enabled = !it.enabled
                                    if (!it.enabled) universe.ship.thrust = Vec2.Zero
                                }
                            }
                            androidx.compose.material3.Text(
                                modifier = Modifier.padding(start = 8.dp),
                                style = textStyle,
                                color = if (autopilotEnabled) Colors.Autopilot else Colors.Console,
                                text = if (autopilotEnabled) "AUTO: ON" else "AUTO: OFF",
                            )
                        }
                    }
                }
            }

            // Fuel visualization bar
            AnimatedVisibility(visible = bottomVisible, enter = flickerFadeInAfterDelay(150)) {
                val frac = (universe.ship.fuel / kotlin.math.max(1f, universe.ship.fuelCapacity)).coerceIn(0f, 1f)
                val fuelColor = when {
                    frac < 0.2f -> androidx.compose.ui.graphics.Color(0xFFFF5252)
                    frac < 0.5f -> androidx.compose.ui.graphics.Color(0xFFFFC107)
                    else -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                }
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Colors.Console.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(frac)
                            .background(fuelColor, RoundedCornerShape(3.dp))
                    )
                }
            }

            // Hull visualization bar
            AnimatedVisibility(visible = bottomVisible, enter = flickerFadeInAfterDelay(200)) {
                val hfrac = (universe.ship.hull / kotlin.math.max(1f, universe.ship.hullCapacity)).coerceIn(0f, 1f)
                val hullColor = when {
                    hfrac < 0.2f -> androidx.compose.ui.graphics.Color(0xFFFF5252)
                    hfrac < 0.5f -> androidx.compose.ui.graphics.Color(0xFFFFC107)
                    else -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                }
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Colors.Console.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(hfrac)
                            .background(hullColor, RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopStart),
            visible = topVisible,
            enter = flickerFadeInAfterDelay(1000),
        ) {
            androidx.compose.material3.Text(
                style = textStyle,
                fontSize = catalogFontSize,
                lineHeight = catalogFontSize,
                letterSpacing = 1.sp,
                color = Colors.Console,
                onTextLayout = { tlr -> if (tlr.didOverflowHeight) catalogFontSize = 8.sp },
                text = (
                    with(universe.star) {
                        listOf(
                            "  STAR: $name (${getSystemDesignation(universe)})",
                            " CLASS: ${cls.name}",
                            "RADIUS: ${radius.toInt()}",
                            "  MASS: %.3g".format(mass),
                            "BODIES: ${explored.size} / ${universe.planets.size}",
                            "",
                        )
                    } +
                        explored
                            .map {
                                listOf(
                                    "  BODY: ${it.name}",
                                    "  TYPE: ${it.description.capitalize(Locale.getDefault())}",
                                    "  ATMO: ${it.atmosphere.capitalize()}",
                                    " FAUNA: ${it.fauna.capitalize()}",
                                    " FLORA: ${it.flora.capitalize()}",
                                    "",
                                )
                            }
                            .flatten()
                    ).joinToString("\n"),
            )
        }
    }
}

