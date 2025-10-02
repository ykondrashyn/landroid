package ru.queuejw.space.game

import kotlin.random.Random

/**
 * Platform-agnostic naming interface used by the simulation core.
 * Implementations must be pure Kotlin without side effects beyond RNG usage
 * to preserve determinism across platforms.
 */
interface INamer {
    fun describePlanet(rng: Random): String
    fun describeLife(rng: Random): String
    fun nameSystem(rng: Random): String
    fun describeAtmo(rng: Random): String
    fun describeActivity(rng: Random, target: Planet?): String
}

