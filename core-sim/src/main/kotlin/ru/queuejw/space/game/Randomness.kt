package ru.queuejw.space.game

import kotlin.random.Random

class Bag<T>(items: Array<T>) {
    private val remaining = items.copyOf()
    private var next = remaining.size
    fun pull(rng: Random): T {
        if (next >= remaining.size) {
            remaining.shuffle(rng)
            next = 0
        }
        return remaining[next++]
    }
}

class RandomTable<T>(private vararg val pairs: Pair<Float, T>) {
    private val total = pairs.map { it.first }.sum()
    fun roll(rng: Random): T {
        var x = rng.nextFloatInRange(0f, total)
        for ((weight, result) in pairs) {
            x -= weight
            if (x < 0f) return result
        }
        return pairs.last().second
    }
}

fun Random.nextFloatInRange(from: Float, until: Float): Float =
    from + ((until - from) * nextFloat())

fun Random.nextFloatInRange(fromUntil: ClosedFloatingPointRange<Float>): Float =
    nextFloatInRange(fromUntil.start, fromUntil.endInclusive)

fun Random.nextFloatInRange(fromUntil: Pair<Float, Float>): Float =
    nextFloatInRange(fromUntil.first, fromUntil.second)

fun <T> Random.choose(array: Array<T>) = array[nextInt(array.size)]

