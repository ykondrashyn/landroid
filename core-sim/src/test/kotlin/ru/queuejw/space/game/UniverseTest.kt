package ru.queuejw.space.game

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

private class TestNamer : INamer {
    override fun describePlanet(rng: kotlin.random.Random) = "planet"
    override fun describeLife(rng: kotlin.random.Random) = "life"
    override fun nameSystem(rng: kotlin.random.Random) = "SYS"
    override fun describeAtmo(rng: kotlin.random.Random) = "atmo"
    override fun describeActivity(rng: kotlin.random.Random, target: PlanetInfo?) = "activity"
}

class UniverseTest {
    @Test
    fun `stepping increases sim time`() {
        val u = Universe(TestNamer(), randomSeed = 1L).apply { initRandom() }
        val start = u.now
        var t = 1_000_000L
        repeat(10) {
            t += 16_666_667L
            u.step(t)
        }
        assertTrue(u.now > start, "expected now to increase after steps")
    }

    @Test
    fun `initRandom creates ship and planets`() {
        val u = Universe(TestNamer(), randomSeed = 42L).apply { initRandom() }
        assertNotNull(u.ship)
        assertTrue(u.planets.isNotEmpty(), "expected some planets")
    }
}

