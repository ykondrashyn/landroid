package ru.queuejw.space.game

import kotlin.random.Random

interface INamer {
    fun describePlanet(rng: Random): String
    fun describeLife(rng: Random): String
    fun nameSystem(rng: Random): String
    fun describeAtmo(rng: Random): String
    fun describeActivity(rng: Random, target: PlanetInfo?): String
}

