package ru.queuejw.space.desktop

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.queuejw.space.desktop.ui.Telescope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.zIndex
import ru.queuejw.space.desktop.ui.drawLine
import ru.queuejw.space.desktop.ui.UniverseCanvas
import ru.queuejw.space.desktop.ui.drawUniverse
import ru.queuejw.space.desktop.ui.toOffset
import ru.queuejw.space.desktop.ui.zoom
import ru.queuejw.space.game.Universe
import ru.queuejw.space.shared.NamerShared

fun main(args: Array<String>) {
    val config = AppConfig.fromArgs(args).validate()

    if (config.mcp && config.headless) {
        runHeadlessMCP(config)
    } else {
        runDesktopUI(config)
    }
}

/**
 * Run headless MCP server mode (no UI, API control only).
 */
private fun runHeadlessMCP(config: AppConfig) {
    var universe = Universe(NamerShared(), randomSeed = config.seed).apply { initRandom() }

    // Optional realtime stepper in headless mode
    val stopFlag = java.util.concurrent.atomic.AtomicBoolean(false)
    val stepThread: Thread? = if (config.realtime) {
        val frameMillis = (1000L / config.hz.coerceIn(1, 240)).coerceAtLeast(1L)
        Thread({
            while (!stopFlag.get()) {
                universe.step(System.nanoTime())
                try { Thread.sleep(frameMillis) } catch (_: InterruptedException) {}
            }
        }, "rt-stepper").apply { isDaemon = true; start() }
    } else null

    val server = ru.queuejw.space.desktop.mcp.UIMCPServer(
        universeProvider = { universe },
        universeSetter = { newU -> universe = newU },
        initialSeed = config.seed,
        universeFactory = { s -> Universe(NamerShared(), randomSeed = s).apply { initRandom() } },
        realtime = config.realtime
    ).start(port = config.mcpPort)

    if (server != null) {
        Logger.info("Press Ctrl+C to stop")
        Runtime.getRuntime().addShutdownHook(Thread {
            Logger.info("Shutting down MCP server...")
            stopFlag.set(true)
            stepThread?.join(500)
            server.stop(0)
        })
        Thread.currentThread().join()
    } else {
        Logger.error("Failed to start server. Exiting.")
        stopFlag.set(true)
        kotlin.system.exitProcess(1)
    }
}

/**
 * Run desktop UI mode (with or without MCP).
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun runDesktopUI(config: AppConfig) {

    application {
        Window(onCloseRequest = ::exitApplication, title = "Space Desktop") {
            var universe by remember { mutableStateOf(Universe(NamerShared(), randomSeed = config.seed).apply { initRandom() }) }

            // Always create autopilot like Android; enable based on CLI flag
            val ap = remember(universe) {
                ru.queuejw.space.game.Autopilot(universe.ship, universe).also {
                    universe.ship.autopilot = it
                    universe.add(it)
                    it.enabled = config.autopilot
                }
            }

            val controlsEnabled = !config.mcp
            // Keyboard controls: WASD / Arrow keys
            var thrustPressed by remember { mutableStateOf(false) }
            var leftPressed by remember { mutableStateOf(false) }
            var rightPressed by remember { mutableStateOf(false) }

            // Camera state matching Android defaults
            var cameraZoom by remember { mutableFloatStateOf(1f) }
            var cameraOffset by remember { mutableStateOf(Offset.Zero) }
            // User wheel-zoom state (enabled even in MCP mode)
            var userZoomPinned by remember { mutableStateOf(false) }
            // User pan state (click+drag to move the map)
            var userPanPinned by remember { mutableStateOf(false) }
            var isPanning by remember { mutableStateOf(false) }
            var lastPointerPos by remember { mutableStateOf<Offset?>(null) }
            var hoverPosPx by remember { mutableStateOf<Offset?>(null) }
            var boxSize by remember { mutableStateOf(IntSize.Zero) }

            val focusRequester = FocusRequester()
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            // Start MCP server when requested (UI + MCP); drives simulation via /step
            LaunchedEffect(key1 = config.mcp) {
                if (config.mcp) {
                    val server = ru.queuejw.space.desktop.mcp.UIMCPServer(
                        universeProvider = { universe },
                        universeSetter = { newU -> universe = newU },
                        initialSeed = config.seed,
                        universeFactory = { s -> Universe(NamerShared(), randomSeed = s).apply { initRandom() } },
                        realtime = config.realtime
                    ).start(port = config.mcpPort)

                    if (server == null) {
                        Logger.error("MCP server failed to start. UI will run without API control.")
                    }
                }
            }

            // Frame-tied simulation and input application (like Android), but disabled when MCP mode
            LaunchedEffect(universe, controlsEnabled) {
                var lastNanos = 0L
                while (true) {
                    withInfiniteAnimationFrameNanos { nanos ->
                        val dt = if (lastNanos == 0L) 0f else ((nanos - lastNanos) / 1_000_000_000f)
                        lastNanos = nanos
                        if (controlsEnabled) {
                            val ship = universe.ship
                            val apEnabled = ship.autopilot?.enabled == true
                            if (!apEnabled) {
                                val turnRate = kotlin.math.PI.toFloat() // rad/s
                                if (leftPressed) ship.angle -= turnRate * dt
                                if (rightPressed) ship.angle += turnRate * dt
                                ship.thrust = if (thrustPressed) ru.queuejw.space.game.Vec2.makeWithAngleMag(ship.angle, 1f) else ru.queuejw.space.game.Vec2.Zero
                            }
                            // In manual mode, step simulation on UI frames
                            universe.step(nanos)
                        } else {
                            // In MCP mode, step here only when realtime mode is enabled
                            if (config.mcp && config.realtime) {
                                universe.step(nanos)
                            }
                        }
                    }
                }

            }

            val keyHandler: (KeyEvent) -> Boolean = handler@ { event ->
                if (!controlsEnabled) return@handler false
                val isDown = event.type == KeyEventType.KeyDown
                val key = event.key
                val isControlKey = key in setOf(
                    Key.W, Key.DirectionUp,
                    Key.S, Key.DirectionDown,
                    Key.A, Key.DirectionLeft,
                    Key.D, Key.DirectionRight,
                )
                if (isDown && isControlKey) {
                    universe.ship.autopilot?.let {
                        if (it.enabled) {
                            it.enabled = false
                            universe.ship.thrust = ru.queuejw.space.game.Vec2.Zero
                        }
                    }
                }
                when (key) {
                    Key.W, Key.DirectionUp -> { thrustPressed = isDown; true }
                    Key.S, Key.DirectionDown -> { thrustPressed = false; true }
                    Key.A, Key.DirectionLeft -> { leftPressed = isDown; true }
                    Key.D, Key.DirectionRight -> { rightPressed = isDown; true }
                    else -> false
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (controlsEnabled) Modifier.onPreviewKeyEvent(keyHandler) else Modifier)
                    .onSizeChanged { boxSize = it }
                    // Mouse wheel zoom (enabled even in MCP mode)
                    .onPointerEvent(PointerEventType.Scroll) { event ->
                        val dy = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (dy != 0f) {
                            val minZ = 200f / ru.queuejw.space.game.UNIVERSE_RANGE
                            val maxZ = 5f
                            val factor = kotlin.math.exp(-dy * 0.1f)
                            cameraZoom = (cameraZoom * factor).coerceIn(minZ, maxZ)
                            userZoomPinned = true
                        }
                    }
                    // Mouse press/drag/release for panning
                    .onPointerEvent(PointerEventType.Press) { event ->
                        isPanning = true
                        lastPointerPos = event.changes.firstOrNull()?.position
                        hoverPosPx = lastPointerPos
                        userPanPinned = true
                    }
                    .onPointerEvent(PointerEventType.Release) { _ ->
                        isPanning = false
                        lastPointerPos = null
                    }
                    .onPointerEvent(PointerEventType.Move) { event ->
                        val pos = event.changes.firstOrNull()?.position
                        if (pos != null) {
                            hoverPosPx = pos
                            val last = lastPointerPos
                            if (isPanning && last != null) {
                                val d = pos - last
                                lastPointerPos = pos
                                cameraOffset += Offset(d.x / cameraZoom, d.y / cameraZoom)
                            } else {
                                lastPointerPos = pos
                            }
                        }
                    }
                    .focusRequester(focusRequester)
                    .focusTarget()
            ) {
                // Recompose this Box when the simulation steps, so overlays update in real time
                val recomposeScope = currentRecomposeScope
                Telescope(universe) { recomposeScope.invalidate() }

                UniverseCanvas(universe, Modifier.fillMaxSize()) { u ->
                    // Background
                    drawRect(ru.queuejw.space.desktop.ui.Colors.Eigengrau, Offset.Zero, size)

                    // Match Android camera/zoom logic
                    val closest = u.closestPlanet()
                    val distToNearestSurf = kotlin.math.max(0f, (u.ship.pos - closest.pos).mag() - closest.radius * 1.2f)
                    val minZoom = 200f / ru.queuejw.space.game.UNIVERSE_RANGE
                    val maxZoom = 5f
                    val dynamicZoomAllowed = (u.ship.autopilot?.enabled == true) && !userZoomPinned
                    if (dynamicZoomAllowed) {
                        val targetZoom = (500f / distToNearestSurf).coerceIn(minZoom, maxZoom)
                        cameraZoom = ru.queuejw.space.game.expSmooth(cameraZoom, targetZoom, dt = u.dt, speed = 1.5f)
                    } else if (!userZoomPinned) {
                        // Default zoom when no user override and no dynamic zoom
                        val targetZoom = 1f
                        cameraZoom = ru.queuejw.space.game.expSmooth(cameraZoom, targetZoom, dt = u.dt, speed = 1.5f)
                    }
                    // Follow target unless user panned
                    if (!userPanPinned) {
                        cameraOffset = (u.follow?.pos ?: ru.queuejw.space.game.Vec2.Zero).toOffset() * -1f
                    }

                    val visibleMeters = size / cameraZoom
                    val visibleRect = Rect(
                        -cameraOffset - Offset(visibleMeters.width * 0.5f, visibleMeters.height * 0.5f),
                        visibleMeters
                    )

                    zoom(cameraZoom) {
                        translate(
                            -visibleRect.center.x + size.width * 0.5f,
                            -visibleRect.center.y + size.height * 0.5f,
                        ) {
                            this@zoom.drawUniverse(u)

                            // Draw line from ship to hovered planet if any
                            val hp = hoverPosPx
                            if (hp != null) {
                                val visibleMeters = size / cameraZoom
                                val topLeft = -cameraOffset - Offset(visibleMeters.width * 0.5f, visibleMeters.height * 0.5f)
                                val wx = topLeft.x + hp.x / cameraZoom
                                val wy = topLeft.y + hp.y / cameraZoom
                                val hovered = u.entities.filterIsInstance<ru.queuejw.space.game.Planet>().firstOrNull { p ->
                                    val dx = p.pos.x - wx
                                    val dy = p.pos.y - wy
                                    kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat() <= p.radius
                                }
                                if (hovered != null) {
                                    val dirX = u.ship.pos.x - hovered.pos.x
                                    val dirY = u.ship.pos.y - hovered.pos.y
                                    val len = kotlin.math.hypot(dirX.toDouble(), dirY.toDouble()).toFloat().coerceAtLeast(1e-3f)
                                    val cpX = hovered.pos.x + dirX * (hovered.radius / len)
                                    val cpY = hovered.pos.y + dirY * (hovered.radius / len)
                                    val closestPoint = ru.queuejw.space.game.Vec2(cpX, cpY)
                                    drawLine(
                                        color = Color(0xFFFFD700), // gold
                                        start = u.ship.pos,
                                        end = closestPoint,
                                        strokeWidth = 2f / this@zoom.zoom
                                    )
                                }
                            }
                        }
                    }
                }
                // Overlay telemetry like Android; hide interactive controls in MCP mode
                ru.queuejw.space.desktop.ui.Telemetry(
                    universe = universe,
                    showControls = controlsEnabled,
                    mcpMode = config.mcp
                )

                // Hover panel near planet with distance (updates each sim step)
                val hp = hoverPosPx
                val hoverPanel = run {
                    if (hp != null && boxSize.width > 0 && boxSize.height > 0) {
                        val visibleMeters = Offset(boxSize.width.toFloat(), boxSize.height.toFloat()) / cameraZoom
                        val topLeft = -cameraOffset - Offset(visibleMeters.x * 0.5f, visibleMeters.y * 0.5f)
                        val wx = topLeft.x + hp.x / cameraZoom
                        val wy = topLeft.y + hp.y / cameraZoom
                        val hovered = universe.entities.filterIsInstance<ru.queuejw.space.game.Planet>().firstOrNull { p ->
                            val dx = p.pos.x - wx
                            val dy = p.pos.y - wy
                            kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat() <= p.radius
                        }
                        hovered?.let { p ->
                            val dirX = universe.ship.pos.x - p.pos.x
                            val dirY = universe.ship.pos.y - p.pos.y
                            val len = kotlin.math.hypot(dirX.toDouble(), dirY.toDouble()).toFloat().coerceAtLeast(1e-3f)
                            val cpX = p.pos.x + dirX * (p.radius / len)
                            val cpY = p.pos.y + dirY * (p.radius / len)
                            val alt = kotlin.math.max(0f, kotlin.math.hypot(dirX.toDouble(), dirY.toDouble()).toFloat() - p.radius).toInt()
                            val pxX = (cpX - topLeft.x) * cameraZoom
                            val pxY = (cpY - topLeft.y) * cameraZoom
                            Pair("Distance: ${alt} m", IntOffset(pxX.toInt() + 12, pxY.toInt() - 12))
                        }
                    } else null
                }
                hoverPanel?.let { (text, offsetInt) ->
                    BasicText(
                        text = text,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset { offsetInt }
                            .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        style = TextStyle(color = Color.White)
                    )
                }

                // Reset camera button (unpin and re-enable follow/dynamic zoom)
                BasicText(
                    text = "Reset Camera",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .zIndex(10f)
                        .background(Color(0xCC222222), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                        .clickable {
                            userZoomPinned = false
                            userPanPinned = false
                            cameraZoom = 1f
                            cameraOffset = (universe.follow?.pos ?: ru.queuejw.space.game.Vec2.Zero).toOffset() * -1f
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    style = TextStyle(color = Color.White)
                )

                // DESTROYED banner + auto-exit when hull is zero
                val isDestroyed = universe.ship.hull <= 0f
                if (isDestroyed) {
                    var blink by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        // Blink and exit after short delay
                        val start = System.currentTimeMillis()
                        while (System.currentTimeMillis() - start < 1800) {
                            blink = !blink
                            delay(180)
                        }
                        kotlin.system.exitProcess(0)
                    }
                    if (blink) {
                        BasicText(
                            text = "DESTROYED",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            style = TextStyle(color = Color(0xFFFF5252), fontSize = 48.sp, fontWeight = FontWeight.Black)
                        )
                    }
                }

            }
        }
    }
}

