package ru.queuejw.space.desktop

import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import ru.queuejw.space.game.Universe
import ru.queuejw.space.game.Vec2


private const val WIDTH = 1024
private const val HEIGHT = 768

fun main(args: Array<String>) {
    // Parse seed from args
    var seed = 42L
    args.forEach { arg ->
        if (arg.startsWith("--seed=")) {
            seed = arg.removePrefix("--seed=").toLongOrNull() ?: 42L
        }
    }

    val frame = JFrame("Space - Desktop Bootstrap")
    val panel = GamePanel(seed)
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.contentPane.add(panel)
    frame.setSize(WIDTH, HEIGHT)
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
    panel.start()
}

private class GamePanel(seed: Long) : JPanel(), KeyListener, Runnable {
    // Fixed timestep loop (60 Hz)
    private val targetFps = 60.0
    private val dt = 1.0 / targetFps

    // Core simulation
    private val universe = Universe(DesktopNamer(), randomSeed = seed).apply { initRandom() }

    private var up = false
    private var down = false
    private var left = false
    private var right = false

    private var running = false
    private var loopThread: Thread? = null

    init {
        background = Color(0x10, 0x12, 0x16)
        isFocusable = true
        addKeyListener(this)
    }

    fun start() {
        if (running) return
        running = true
        requestFocusInWindow()
        loopThread = Thread(this, "game-loop").also { it.start() }
    }

    override fun run() {
        var previous = System.nanoTime()
        var lag = 0.0
        while (running) {
            val current = System.nanoTime()
            val elapsed = (current - previous) / 1_000_000_000.0
            previous = current
            lag += elapsed

            while (lag >= dt) {
                update(dt)
                lag -= dt
            }
            // Step the core simulation with wall-clock nanoseconds
            universe.step(current)

            repaint()

            try { Thread.sleep(2) } catch (_: InterruptedException) {}
        }
    }

    private fun update(dt: Double) {
        // Map keys to the real spacecraft
        val turnRate = Math.toRadians(180.0) // deg/s
        val ship = universe.ship
        if (left) ship.angle -= (turnRate * dt).toFloat()
        if (right) ship.angle += (turnRate * dt).toFloat()
        ship.thrust = if (up) Vec2.makeWithAngleMag(ship.angle, 1f) else Vec2.Zero
        if (down) ship.thrust = Vec2.Zero
    }

    override fun paintComponent(g0: Graphics) {
        super.paintComponent(g0)
        val g = g0 as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Starfield background dots
        g.color = Color(0x1E, 0x21, 0x29)
        val w = width
        val h = height
        val step = 32
        for (ix in 0 until w step step) {
            for (iy in 0 until h step step) {
                g.fillRect(ix, iy, 1, 1)
            }
        }

        // Draw actual universe: center on ship
        val ship = universe.ship
        val centerX = width / 2.0
        val centerY = height / 2.0
        val scale = 0.1 // world px to screen px

        // Star
        universe.star.let { star ->
            val sx = centerX + (star.pos.x - ship.pos.x) * scale
            val sy = centerY + (star.pos.y - ship.pos.y) * scale
            val r = (star.radius * scale).toInt().coerceAtLeast(1)
            g.color = Color(255, 235, 130)
            g.fillOval((sx - r).toInt(), (sy - r).toInt(), r * 2, r * 2)
        }
        // Planets
        universe.planets.forEach { p ->
            val px = centerX + (p.pos.x - ship.pos.x) * scale
            val py = centerY + (p.pos.y - ship.pos.y) * scale
            val pr = (p.radius * scale).toInt().coerceAtLeast(1)
            g.color = Color(160, 217, 109)
            g.drawOval((px - pr).toInt(), (py - pr).toInt(), pr * 2, pr * 2)
        }
        // Ship (triangle)
        val shipSize = 10.0
        val noseX = centerX + cos(ship.angle.toDouble()) * shipSize
        val noseY = centerY + sin(ship.angle.toDouble()) * shipSize
        val leftX = centerX + cos(ship.angle + Math.PI.toFloat() * 3 / 4) * shipSize * 0.7
        val leftY = centerY + sin(ship.angle + Math.PI.toFloat() * 3 / 4) * shipSize * 0.7
        val rightX = centerX + cos(ship.angle - Math.PI.toFloat() * 3 / 4) * shipSize * 0.7
        val rightY = centerY + sin(ship.angle - Math.PI.toFloat() * 3 / 4) * shipSize * 0.7
        g.color = Color(0xA0, 0xD9, 0x6D)
        g.fillPolygon(
            intArrayOf(noseX.toInt(), leftX.toInt(), rightX.toInt()),
            intArrayOf(noseY.toInt(), leftY.toInt(), rightY.toInt()),
            3
        )

        // HUD text
        g.color = Color(0xDD, 0xDD, 0xDD)
        g.drawString("Arrows: steer/thrust  |  ESC: quit", 12, 20)
        g.drawString("Seeded core sim: ${universe.now.toInt()}s", 12, 36)
    }

    override fun keyTyped(e: KeyEvent) {}

    override fun keyPressed(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_LEFT, KeyEvent.VK_A -> left = true
            KeyEvent.VK_RIGHT, KeyEvent.VK_D -> right = true
            KeyEvent.VK_UP, KeyEvent.VK_W -> up = true
            KeyEvent.VK_DOWN, KeyEvent.VK_S -> down = true
            KeyEvent.VK_ESCAPE -> System.exit(0)
        }
    }

    override fun keyReleased(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_LEFT, KeyEvent.VK_A -> left = false
            KeyEvent.VK_RIGHT, KeyEvent.VK_D -> right = false
            KeyEvent.VK_UP, KeyEvent.VK_W -> up = false
            KeyEvent.VK_DOWN, KeyEvent.VK_S -> down = false
        }
    }
}

