package ru.queuejw.space.mcp

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import ru.queuejw.space.game.*
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.PI

private class MinimalNamer : INamer {
    override fun describePlanet(rng: kotlin.random.Random) = listOf("lush", "barren", "rocky").random(rng)
    override fun describeLife(rng: kotlin.random.Random) = listOf("microbial", "flora", "fauna").random(rng)
    override fun nameSystem(rng: kotlin.random.Random) = "SYS-" + ('A' + rng.nextInt(26)) + (rng.nextInt(999).toString())
    override fun describeAtmo(rng: kotlin.random.Random) = listOf("thin", "thick", "toxic").random(rng)
    override fun describeActivity(rng: kotlin.random.Random, target: PlanetInfo?) = "scanning"
}

private class MCPService(seed: Long) {
    private var currentNanos: Long = 1_000_000L
    var universe: Universe = Universe(MinimalNamer(), seed).apply { initRandom() }
        private set

    var autopilotEnabled: Boolean = false

    private fun ensureAutopilotState() {
        val ship = universe.ship
        if (autopilotEnabled) {
            if (ship.autopilot == null) {
                val ap = Autopilot(ship, universe)
                ship.autopilot = ap
                universe.add(ap)
            }
            ship.autopilot?.enabled = true
        } else {
            ship.autopilot?.let { ap ->
                ap.enabled = false
                universe.remove(ap)
                ship.autopilot = null
            }
        }
    }

    fun setAutopilot(enabled: Boolean) {
        autopilotEnabled = enabled
        ensureAutopilotState()
    }

    fun reset(seed: Long) {
        currentNanos = 1_000_000L
        universe = Universe(MinimalNamer(), seed).apply { initRandom() }
        ensureAutopilotState()
    }

    fun step(dtSeconds: Float): Float {
        if (dtSeconds <= 0f) return universe.now
        currentNanos += (dtSeconds * 1_000_000_000L).toLong()
        universe.step(currentNanos)
        return universe.now
    }

    fun act(thrust: Float?, angle: Float?) {
        val ship = universe.ship
        angle?.let { ship.angle = it }
        thrust?.let { t ->
            ship.thrust = if (t > 0f) Vec2.makeWithAngleMag(ship.angle, t.coerceIn(0f, 1f)) else Vec2.Zero
        }
    }

    private fun fmt2(v: Float) = String.format(Locale.US, "%.2f", v)
    private fun fmt3(v: Float) = String.format(Locale.US, "%.3f", v)
    private fun jsonString(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    fun observe(): String {
        val ship = universe.ship
        val bodies = universe.planets.size + 1
        val landed = ship.landing != null
        val landingJson = if (landed) {
            val planetName = ship.landing!!.planet.name
            "{\"landed\":true,\"planet\":${jsonString(planetName)}}"
        } else {
            "{\"landed\":false}"
        }
        val nearest = universe.closestPlanet()
        val nearestJson = "{\"name\":${jsonString(nearest.name)},\"dist\":${fmt2((nearest.pos - ship.pos).mag())}}"
        return """{"now":${fmt3(universe.now)},"ship":{"x":${fmt2(ship.pos.x)},"y":${fmt2(ship.pos.y)},"vx":${fmt2(ship.velocity.x)},"vy":${fmt2(ship.velocity.y)},"angle":${fmt3(ship.angle)}},"bodies":$bodies,"autopilot":$autopilotEnabled,"landing":$landingJson,"nearest":$nearestJson}"""
    }
}

private fun parseQuery(query: String?): Map<String, String> {
    if (query.isNullOrEmpty()) return emptyMap()
    return query.split('&').mapNotNull { kv ->
        val idx = kv.indexOf('='); if (idx <= 0) null else {
            val k = URLDecoder.decode(kv.substring(0, idx), StandardCharsets.UTF_8)
            val v = URLDecoder.decode(kv.substring(idx + 1), StandardCharsets.UTF_8)
            k to v
        }
    }.toMap()
}

private fun readBody(ex: HttpExchange): String =
    ex.requestBody.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

private fun parseJsonNumbers(body: String): Map<String, String> {
    // Very small, permissive parser for flat JSON objects with numeric values
    // e.g., {"dt":0.016,"thrust":1,"angle":0.0}
    val s = body.trim().removePrefix("{").removeSuffix("}")
    if (s.isBlank()) return emptyMap()
    val result = mutableMapOf<String, String>()
    s.split(',').forEach { pair ->
        val idx = pair.indexOf(':')
        if (idx > 0) {
            val k = pair.substring(0, idx).trim().trim('"', '\'', ' ')
            val v = pair.substring(idx + 1).trim()
            // strip quotes if present
            val vv = if (v.startsWith('"') && v.endsWith('"')) v.substring(1, v.length - 1) else v
            result[k] = vv
        }
    }
    return result
}

private fun parseArgs(args: Array<String>): Map<String, String> = buildMap {
    args.forEach { a ->
        if (a.startsWith("--")) {
            val s = a.removePrefix("--")
            val i = s.indexOf('=')
            if (i > 0) put(s.substring(0, i), s.substring(i + 1)) else put(s, "true")
        }
    }
}

private fun getParams(ex: HttpExchange): Map<String, String> {
    val q = parseQuery(ex.requestURI.query).toMutableMap()
    if (ex.requestMethod.equals("POST", ignoreCase = true)) {
        val ct = ex.requestHeaders.getFirst("Content-Type") ?: ""
        if (ct.contains("application/json", ignoreCase = true)) {
            val json = parseJsonNumbers(readBody(ex))
            q.putAll(json)
        }
    }
    return q
}


private fun HttpExchange.writeJson(status: Int, body: String) {

    sendResponseHeaders(status, body.toByteArray().size.toLong())
    responseHeaders.add("Content-Type", "application/json")
    responseBody.use { it.write(body.toByteArray()) }
}

fun main(args: Array<String>) {
    val argv = parseArgs(args)
    val service = MCPService(seed = 42L)
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 8080), 0)
    val ap = (argv["autopilot"] ?: "false").lowercase(Locale.US)
    if (ap == "1" || ap == "true" || ap == "on" || ap == "yes") service.setAutopilot(true)

    server.createContext("/health") { ex ->
        ex.writeJson(200, "{\"status\":\"ok\"}")
    }

    server.createContext("/observe") { ex ->
        ex.writeJson(200, service.observe())
    }

    server.createContext("/step") { ex ->
        val p = getParams(ex)
        val dt = p["dt"]?.toFloatOrNull() ?: 1f / 60f
        val now = service.step(dt)
        ex.writeJson(200, String.format(Locale.US, "{\"now\":%.3f}", now))
    }

    server.createContext("/act") { ex ->
        val p = getParams(ex)
        val thrust = p["thrust"]?.toFloatOrNull()
        val angle = p["angle"]?.toFloatOrNull()
        service.act(thrust, angle)
        ex.writeJson(200, "{\"ok\":true}")
    }

    server.createContext("/reset") { ex ->
        val p = parseQuery(ex.requestURI.query)
        val seed = p["seed"]?.toLongOrNull() ?: 42L
        service.reset(seed)
        ex.writeJson(200, "{\"ok\":true}")
    }
    server.executor = null
    server.start()
    println("MCP server listening on http://127.0.0.1:8080  (health, observe, act, step, reset)")
    // Keep running
}

