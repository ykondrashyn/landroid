package ru.queuejw.space.game

import kotlin.math.exp
import kotlin.math.pow

fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** smoothstep. Ken Perlin's version */
fun smooth(x: Float): Float {
    return x * x * x * (x * (x * 6 - 15) + 10)
}

/** Kind of like an inverted smoothstep, but */
fun invsmoothish(x: Float): Float {
    return 0.25f * ((2f * x - 1f).pow(5f) + 1f) + 0.5f * x
}

/** Compute the fraction that progress represents between start and end (inverse of lerp). */
fun lexp(start: Float, end: Float, progress: Float): Float {
    return (progress - start) / (end - start)
}

/** Exponentially smooth current toward target by a factor of speed. */
fun expSmooth(current: Float, target: Float, dt: Float = 1f / 60, speed: Float = 5f): Float {
    return current + (target - current) * (1 - exp(-dt * speed))
}

