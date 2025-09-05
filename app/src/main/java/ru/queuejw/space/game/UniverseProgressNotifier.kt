/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.queuejw.space.game

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.DisposableHandle

const val CHANNEL_ID = "progress"
const val CHANNEL_NAME = "Spacecraft progress"
const val UPDATE_FREQUENCY_SEC = 1f

fun lerpRange(range: ClosedFloatingPointRange<Float>, x: Float): Float =
    lerp(range.start, range.endInclusive, x)

class UniverseProgressNotifier(context: Context, val universe: Universe) {
    private val notificationId = universe.randomSeed.toInt()
    @RequiresApi(Build.VERSION_CODES.O)
    private val chan =
        NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            .apply { lockscreenVisibility = Notification.VISIBILITY_PUBLIC }
    private val noman =
        context.getSystemService(NotificationManager::class.java)?.apply {
            createNotificationChannel(chan)
        }

    private val registration: DisposableHandle =
        universe.addSimulationStepListener(this::onSimulationStep)

    private val spacecraftIcon = Icon.createWithResource(context, R.drawable.ic_spacecraft_filled)
    private val planetIcons =
        listOf(
            (lerpRange(PLANET_RADIUS_RANGE, 0.75f)) to
                Icon.createWithResource(context, R.drawable.ic_planet_large),
            (lerpRange(PLANET_RADIUS_RANGE, 0.5f)) to
                Icon.createWithResource(context, R.drawable.ic_planet_medium),
            (lerpRange(PLANET_RADIUS_RANGE, 0.25f)) to
                Icon.createWithResource(context, R.drawable.ic_planet_small),
            (PLANET_RADIUS_RANGE.start to
                Icon.createWithResource(context, R.drawable.ic_planet_tiny)),
        )

    private fun getPlanetIcon(planet: Planet): Icon {
        for ((radius, icon) in planetIcons) {
            if (planet.radius > radius) return icon
        }
        return planetIcons.last().second
    }

    private val progress = Notification.ProgressStyle().setProgressTrackerIcon(spacecraftIcon)

    private val builder =
        Notification.Builder(context, CHANNEL_ID)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .setFlag(Notification.FLAG_ONLY_ALERT_ONCE, true)
            .setColorized(true)
            .setOngoing(true)
            .setColor(Colors.Eigengrau2.toArgb())
            .setStyle(progress)

    private var lastUpdate = 0f
    private var initialDistToTarget = 0

    private fun onSimulationStep() {
        if (universe.now - lastUpdate >= UPDATE_FREQUENCY_SEC) {
            lastUpdate = universe.now
            // android.util.Log.v("Landroid", "posting notification at time ${universe.now}")

            var distToTarget = 0
            val autopilot = universe.ship.autopilot
            val autopilotEnabled: Boolean = autopilot?.enabled == true
            val target = autopilot?.target
            val landing = universe.ship.landing
            val speed = universe.ship.velocity.mag()

            if (landing != null) {
                // landed
                builder.setContentTitle("landed: ${landing.planet.name}")
                builder.setContentText("currently: ${landing.text}")
                builder.setShortCriticalText("landed")

                progress.setProgress(progress.progressMax)
                progress.setProgressIndeterminate(false)

                builder.setStyle(progress)
            } else if (autopilotEnabled) {
                if (target != null) {
                    // autopilot en route
                    distToTarget = ((target.pos - universe.ship.pos).mag() - target.radius).toInt()
                    if (initialDistToTarget == 0) {
                        // we have a new target!
                        initialDistToTarget = distToTarget
                        progress.progressEndIcon = getPlanetIcon(target)
                    }

                    val eta = if (speed > 0) "%1.0fs".format(distToTarget / speed) else "???"
                    builder.setContentTitle("headed to: ${target.name}")
                    builder.setContentText(
                        "autopilot is ${autopilot.strategy.lowercase()}" +
                            "\ndist: ${distToTarget}u // eta: $eta"
                    )
                    // fun fact: ProgressStyle was originally EnRouteStyle
                    builder.setShortCriticalText("en route")

                    progress
                        .setProgressSegments(
                            listOf(
                                Notification.ProgressStyle.Segment(initialDistToTarget)
                                    .setColor(Colors.Track.toArgb())
                            )
                        )
                        .setProgress(initialDistToTarget - distToTarget)
                        .setProgressIndeterminate(false)
                    builder.setStyle(progress)
                } else {
                    // no target
                    if (initialDistToTarget != 0) {
                        // just launched
                        initialDistToTarget = 0
                        progress.progressStartIcon = progress.progressEndIcon
                        progress.progressEndIcon = null
                    }

                    builder.setContentTitle("in space")
                    builder.setContentText("selecting new target...")
                    builder.setShortCriticalText("launched")

                    progress.setProgressIndeterminate(true)

                    builder.setStyle(progress)
                }
            } else {
                // under user control

                initialDistToTarget = 0

                builder.setContentTitle("in space")
                builder.setContentText("under manual control")
                builder.setShortCriticalText("adrift")

                builder.setStyle(null)
            }

            builder
                .setSubText(getSystemDesignation(universe))
                .setSmallIcon(R.drawable.ic_spacecraft_rotated)

            val notification = builder.build()

            // one of the silliest things about Android is that icon levels go from 0 to 10000
            notification.iconLevel = (((universe.ship.angle + PI2f) / PI2f) * 10_000f).toInt()

            noman?.notify(notificationId, notification)
        }
    }
}
