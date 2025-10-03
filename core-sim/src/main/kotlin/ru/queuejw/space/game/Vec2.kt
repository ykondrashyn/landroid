package ru.queuejw.space.game

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

const val PIf = PI.toFloat()
const val PI2f = (2 * PI).toFloat()

data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)
    operator fun div(s: Float) = Vec2(x / s, y / s)
    operator fun unaryMinus() = Vec2(-x, -y)

    fun mag(): Float = kotlin.math.sqrt(x * x + y * y)
    fun distance(other: Vec2): Float = (this - other).mag()
    fun angle(): Float = atan2(y, x)
    fun dot(o: Vec2): Float = x * o.x + y * o.y

    fun str(fmt: String = "%+.2f"): String = "<$fmt,$fmt>".format(x, y)

    companion object {
        val Zero = Vec2(0f, 0f)
        fun makeWithAngleMag(a: Float, m: Float): Vec2 = Vec2(m * cos(a), m * sin(a))
    }
}

