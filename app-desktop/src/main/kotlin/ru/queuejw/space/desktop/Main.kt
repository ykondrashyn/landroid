package ru.queuejw.space.desktop

import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val WIDTH = 1024
private const val HEIGHT = 768

fun main(args: Array<String>) {
    val frame = JFrame("Space - Desktop Bootstrap")
    val panel = GamePanel()
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.contentPane.add(panel)
    frame.setSize(WIDTH, HEIGHT)
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
    panel.start()
}

private class GamePanel : JPanel(), KeyListener, Runnable {
    // Fixed timestep loop (60 Hz)
    private val targetFps = 60.0
    private val dt = 1.0 / targetFps

    // Simple ship state
    private var x = WIDTH / 2.0
    private var y = HEIGHT / 2.0
    private var angle = 0.0
    private var speed = 0.0

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
            repaint()

            try { Thread.sleep(2) } catch (_: InterruptedException) {}
        }
    }

    private fun update(dt: Double) {
        // Steering and thrust
        val turnRate = Math.toRadians(180.0) // deg/s
        val accel = 300.0 // px/s^2
        val drag = 0.98 // simple damping

        if (left) angle -= turnRate * dt
        if (right) angle += turnRate * dt
        if (up) speed += accel * dt
        if (down) speed -= accel * dt

        speed *= drag

        x += cos(angle) * speed * dt
        y += sin(angle) * speed * dt

        // Wrap around edges
        if (x < 0) x += width
        if (x > width) x -= width
        if (y < 0) y += height
        if (y > height) y -= height
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

        // Draw ship (triangle)
        val shipSize = 16.0
        val noseX = x + cos(angle) * shipSize
        val noseY = y + sin(angle) * shipSize
        val leftX = x + cos(angle + Math.PI * 3 / 4) * shipSize * 0.7
        val leftY = y + sin(angle + Math.PI * 3 / 4) * shipSize * 0.7
        val rightX = x + cos(angle - Math.PI * 3 / 4) * shipSize * 0.7
        val rightY = y + sin(angle - Math.PI * 3 / 4) * shipSize * 0.7

        g.color = Color(0xA0, 0xD9, 0x6D)
        g.fillPolygon(
            intArrayOf(noseX.toInt(), leftX.toInt(), rightX.toInt()),
            intArrayOf(noseY.toInt(), leftY.toInt(), rightY.toInt()),
            3
        )

        // HUD text
        g.color = Color(0xDD, 0xDD, 0xDD)
        g.drawString("Arrows: steer/thrust  |  ESC: quit", 12, 20)
        g.drawString("Speed: ${"%.1f".format(min(speed, 9999.0))}", 12, 36)
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

