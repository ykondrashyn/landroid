package ru.queuejw.space.desktop.mcp

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import ru.queuejw.space.desktop.Logger
import ru.queuejw.space.game.Universe
import ru.queuejw.space.game.Vec2
import java.net.BindException
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.max

/**
 * MCP (Model-Control-Protocol) HTTP server for controlling the Space simulation.
 * Thread-safe: uses read-write locks to synchronize access to the universe.
 */
class UIMCPServer(
    private var universeProvider: () -> Universe,
    private val universeSetter: (Universe) -> Unit,
    initialSeed: Long,
    private val universeFactory: (Long) -> Universe,
    private val realtime: Boolean = false,
) {
    private val lock = ReentrantReadWriteLock()
    private var currentNanos: Long = INITIAL_NANOS
    private var seed: Long = initialSeed

    companion object {
        private const val INITIAL_NANOS = 1_000_000L
        private const val NANOS_PER_SECOND = 1_000_000_000L
        private val US = Locale.US
        private fun f2(v: Float) = String.format(US, "%.2f", v)
        private fun f3(v: Float) = String.format(US, "%.3f", v)
    }

    private fun parseQuery(query: String?): Map<String, String> {
        if (query.isNullOrEmpty()) return emptyMap()
        return query.split('&').mapNotNull { kv ->
            val idx = kv.indexOf('=')
            if (idx <= 0) null else {
                val k = URLDecoder.decode(kv.substring(0, idx), StandardCharsets.UTF_8)
                val v = URLDecoder.decode(kv.substring(idx + 1), StandardCharsets.UTF_8)
                k to v
            }
        }.toMap()
    }

    private fun HttpExchange.writeJson(status: Int, body: String) {
        responseHeaders.add("Content-Type", "application/json")
        sendResponseHeaders(status, body.toByteArray().size.toLong())
        responseBody.use { it.write(body.toByteArray()) }
    }

    private fun HttpExchange.handleError(e: Exception) {
        val message = e.message?.replace("\"", "\\\"") ?: "Unknown error"
        Logger.error("MCP endpoint error: $message")
        writeJson(500, """{"error":"$message"}""")
    }

    fun start(host: String = "127.0.0.1", port: Int = 8080): HttpServer? {
        return try {
            val server = HttpServer.create(InetSocketAddress(host, port), 0)

            server.createContext("/health") { ex ->
                try {
                    ex.writeJson(200, """{"status":"ok"}""")
                } catch (e: Exception) {
                    ex.handleError(e)
                }
            }

            server.createContext("/observe") { ex ->
                try {
                    lock.read {
                        val u = universeProvider()
                        val ship = u.ship
                        val bodies = u.planets.size + 1
                        val p = parseQuery(ex.requestURI.query)
                        val nearestN = (p["nearestN"]?.toIntOrNull() ?: 3).coerceIn(1, max(1, u.planets.size))

                        // Nearest collidable planet (exclude star)
                        val nearest = u.planets.minByOrNull { (it.pos - ship.pos).mag() }
                        val nearestIndex = if (nearest != null) u.planets.indexOf(nearest) else -1
                        val nearestJson = if (nearest != null) {
                            val v = nearest.pos - ship.pos
                            val d = v.mag()
                            val alt = d - (ship.radius + nearest.radius)
                            val nx = if (d > 1e-6f) v.x / d else 0f
                            val ny = if (d > 1e-6f) v.y / d else 0f
                            val gmag = if (d > 1e-6f) ru.queuejw.space.game.GRAVITATION * (ship.mass * nearest.mass) / (d * d) else 0f
                            val gx = gmag * nx
                            val gy = gmag * ny
                            """"nearestPlanet":{"id":$nearestIndex,"x":${f2(nearest.pos.x)},"y":${f2(nearest.pos.y)},"radius":${f2(nearest.radius)},"mass":${f3(nearest.mass)},"canLand":${nearest.collides}},"altitude":${f2(alt)},"gravity":{"gx":${f3(gx)},"gy":${f3(gy)}}"""
                        } else """"nearestPlanet":null,"altitude":null"""

                        // Top-N nearest planets (ids + useful per-planet metrics)
                        val nearestPlanetsJson = buildString {
                            append('[')
                            val list = u.planets.mapIndexed { idx, p -> idx to p }
                                .sortedBy { (_, p) -> (p.pos - ship.pos).mag() }
                                .take(nearestN)
                            list.forEachIndexed { i, (idx, p) ->
                                if (i > 0) append(',')
                                val v = p.pos - ship.pos
                                val d = v.mag()
                                val alt = d - (ship.radius + p.radius)
                                val nx = if (d > 1e-6f) v.x / d else 0f
                                val ny = if (d > 1e-6f) v.y / d else 0f
                                val gmag = if (d > 1e-6f) ru.queuejw.space.game.GRAVITATION * (ship.mass * p.mass) / (d * d) else 0f
                                val gx = gmag * nx
                                val gy = gmag * ny
                                append("{" +
                                    "\"id\":$idx," +
                                    "\"x\":${f2(p.pos.x)},\"y\":${f2(p.pos.y)}," +
                                    "\"radius\":${f2(p.radius)}," +
                                    "\"distance\":${f2(d)}," +
                                    "\"altitude\":${f2(alt)}," +
                                    "\"gx\":${f3(gx)}," +
                                    "\"gy\":${f3(gy)}," +
                                    "\"vx\":${f2(p.velocity.x)},\"vy\":${f2(p.velocity.y)}," +
                                    "\"mass\":${f3(p.mass)}" +

                                "}")
                            }
                            append(']')
                        }


                        // Landing info if currently grounded
                        val landingJson = u.ship.landing?.let { l ->
                            val pid = u.planets.indexOf(l.planet)
                            """{"planetId":$pid,"planetName":"${l.planet.name}","angle":${f3(l.angle)},"text":"${l.text.replace("\"","\\\"")}"}"""
                        } ?: "null"

                        val json = """{"now":${f3(u.now)},"ship":{"x":${f2(ship.pos.x)},"y":${f2(ship.pos.y)},"vx":${f2(ship.velocity.x)},"vy":${f2(ship.velocity.y)},"angle":${f3(ship.angle)},"fuel":${f2(ship.fuel)},"fuelCapacity":${f2(ship.fuelCapacity)},"hull":${f2(ship.hull)},"hullCapacity":${f2(ship.hullCapacity)}},${nearestJson},"nearestPlanetId":$nearestIndex,"nearestPlanets":$nearestPlanetsJson,"landing":$landingJson,"bodies":$bodies}"""
                        ex.writeJson(200, json)
                    }
                } catch (e: Exception) {
                    ex.handleError(e)
                }
            }
            // World description: star + planets with positions and metadata
            server.createContext("/world") { ex ->
                try {
                    lock.read {
                        val u = universeProvider()
                        val star = u.star

                        fun esc(s: String): String = s.replace("\"", "\\\"")

                        val starJson = """{"name":"${esc(star.name)}","x":${f2(star.pos.x)},"y":${f2(star.pos.y)},"vx":${f2(star.velocity.x)},"vy":${f2(star.velocity.y)},"radius":${f2(star.radius)},"mass":${f3(star.mass)},"class":"${star.cls}","deadly":true,"collides":false,"canLand":false}"""

                        val planetsJson = buildString {
                            append('[')
                            u.planets.forEachIndexed { idx, p ->
                                if (idx > 0) append(',')
                                append("{" +
                                    "\"id\":$idx," +
                                    "\"name\":\"${esc(p.name)}\"," +
                                    "\"x\":${f2(p.pos.x)},\"y\":${f2(p.pos.y)}," +
                                    "\"radius\":${f2(p.radius)}," +
                                    "\"mass\":${f3(p.mass)}," +
                                    "\"vx\":${f2(p.velocity.x)},\"vy\":${f2(p.velocity.y)}," +
                                    "\"collides\":${p.collides}," +
                                    "\"canLand\":${p.collides}," +
                                    "\"explored\":${p.explored}," +
                                    "\"description\":\"${esc(p.description)}\"," +
                                    "\"atmosphere\":\"${esc(p.atmosphere)}\"," +
                                    "\"flora\":\"${esc(p.flora)}\"," +
                                    "\"fauna\":\"${esc(p.fauna)}\"" +
                                "}")
                            }
                            append(']')
                        }

                        val json = """{"now":${f3(u.now)},"universeRange":${ru.queuejw.space.game.UNIVERSE_RANGE},"config":{"realtime":$realtime,"timeScale":${ru.queuejw.space.game.TIME_SCALE}},"star":$starJson,"planets":$planetsJson,"bodies":${u.planets.size + 1}}"""
                        ex.writeJson(200, json)
                    }
                } catch (e: Exception) {
                    ex.handleError(e)
                }
            }


            server.createContext("/act") { ex ->
                try {
                    val p = parseQuery(ex.requestURI.query)
                    val thrust = p["thrust"]?.toFloatOrNull()
                    val angle = p["angle"]?.toFloatOrNull()

                    // Validate thrust range
                    if (thrust != null && (thrust < 0f || thrust > 1f)) {
                        ex.writeJson(400, """{"error":"thrust must be in range [0, 1]"}""")
                        return@createContext
                    }

                    lock.write {
                        val u = universeProvider()
                        val ship = u.ship
                        angle?.let { ship.angle = it }
                        thrust?.let { t ->
                            ship.thrust = if (t > 0f) Vec2.makeWithAngleMag(ship.angle, t.coerceIn(0f, 1f)) else Vec2.Zero
                        }
                    }
                    ex.writeJson(200, """{"ok":true}""")
                } catch (e: Exception) {
                    ex.handleError(e)
                }
            }

            server.createContext("/step") { ex ->
                try {
                    if (realtime) {
                        // In realtime mode, the simulation auto-advances; /step reports current time
                        val now = lock.read { universeProvider().now }
                        ex.writeJson(200, """{"now":${f3(now)}}""")
                    } else {
                        val p = parseQuery(ex.requestURI.query)
                        val dt = p["dt"]?.toFloatOrNull() ?: (1f / 60f)

                        if (dt <= 0f || dt > 10f) {
                            ex.writeJson(400, """{"error":"dt must be in range (0, 10]"}""")
                            return@createContext
                        }

                        lock.write {
                            currentNanos = max(currentNanos + (dt * NANOS_PER_SECOND).toLong(), currentNanos + 1L)
                            universeProvider().step(currentNanos)
                        }

                        val now = lock.read { universeProvider().now }
                        ex.writeJson(200, """{"now":${f3(now)}}""")
                    }
                } catch (e: Exception) {
                    ex.handleError(e)
                }
            }

            server.createContext("/reset") { ex ->
                try {
                    val p = parseQuery(ex.requestURI.query)
                    val newSeed = p["seed"]?.toLongOrNull() ?: seed

                    lock.write {
                        seed = newSeed
                        currentNanos = INITIAL_NANOS
                        val newUniverse = universeFactory(seed)
                        universeSetter(newUniverse)
                    }
                    ex.writeJson(200, """{"ok":true,"seed":$seed}""")
                } catch (e: Exception) {
                    ex.handleError(e)
                }
            }





            server.executor = null
            server.start()
            Logger.success("MCP server listening on http://$host:$port")
            Logger.info("Endpoints: /health, /observe, /world, /act, /step, /reset")
            server
        } catch (e: BindException) {
            Logger.error("Failed to start MCP server: port $port already in use")
            null
        } catch (e: Exception) {
            Logger.error("Failed to start MCP server: ${e.message}")
            null
        }
    }
}

