package ru.queuejw.space.game

import kotlin.random.Random

// artificially speed up or slow down the simulation
const val TIME_SCALE = 1f // simulation seconds per wall clock second

// if it's been over 1 real second since our last timestep, don't simulate that elapsed time.
// this allows the simulation to "pause" when, for example, the activity pauses
const val MAX_VALID_DT = 1f

interface Entity {
    // Integrate.
    // Compute accelerations from forces, add accelerations to velocity, save old position,
    // add velocity to position.
    fun update(sim: Simulator, dt: Float)

    // Post-integration step, after constraints are satisfied.
    fun postUpdate(sim: Simulator, dt: Float)
}

interface Removable {
    fun canBeRemoved(): Boolean
}

class Fuse(var lifetime: Float) : Removable {
    fun update(dt: Float) { lifetime -= dt }
    override fun canBeRemoved(): Boolean = lifetime < 0
}

open class Body(var name: String = "Unknown") : Entity {
    var pos = Vec2.Zero
    var opos = Vec2.Zero
    var velocity = Vec2.Zero

    var mass = 0f
    var angle = 0f
    var radius = 0f

    var collides = true

    var omega: Float
        get() = angle - oangle
        set(value) { oangle = angle - value }

    var oangle = 0f

    override fun update(sim: Simulator, dt: Float) {
        if (dt <= 0) return
        val vscaled = velocity * dt
        opos = pos
        pos += vscaled
    }

    override fun postUpdate(sim: Simulator, dt: Float) {
        if (dt <= 0) return
        velocity = (pos - opos) / dt
    }
}

interface Constraint {
    // Solve constraints. Pick up objects and put them where they are "supposed" to be.
    fun solve(sim: Simulator, dt: Float)
}

open class Container(val radius: Float) : Constraint {
    private val list = ArraySet<Body>()
    private val softness = 0.0f

    override fun toString(): String = "Container($radius)"

    fun add(p: Body) { list.add(p) }
    fun remove(p: Body) { list.remove(p) }

    override fun solve(sim: Simulator, dt: Float) {
        for (p in list) {
            if ((p.pos.mag() + p.radius) > radius) {
                p.pos =
                    p.pos * (softness) +
                        Vec2.makeWithAngleMag(p.pos.angle(), radius - p.radius) * (1f - softness)
            }
        }
    }
}

open class Simulator(val randomSeed: Long) {
    private var wallClockNanos: Long = 0L
    var now: Float = 0f
    var dt: Float = 0f
    val rng = Random(randomSeed)
    val entities = ArraySet<Entity>(1000)
    val constraints = ArraySet<Constraint>(100)
    private val simStepListeners = mutableListOf<() -> Unit>()

    fun add(e: Entity) = entities.add(e)
    fun remove(e: Entity) = entities.remove(e)
    fun add(c: Constraint) = constraints.add(c)
    fun remove(c: Constraint) = constraints.remove(c)

    open fun updateAll(dt: Float, entities: ArraySet<Entity>) {
        entities.forEach { it.update(this, dt) }
    }

    open fun solveAll(dt: Float, constraints: ArraySet<Constraint>) {
        constraints.forEach { it.solve(this, dt) }
    }

    open fun postUpdateAll(dt: Float, entities: ArraySet<Entity>) {
        entities.forEach { it.postUpdate(this, dt) }
    }

    fun step(nanos: Long) {
        val firstFrame = (wallClockNanos == 0L)
        dt = (nanos - wallClockNanos) / 1_000_000_000f * TIME_SCALE
        this.wallClockNanos = nanos
        if (firstFrame || dt > MAX_VALID_DT) return
        this.now += dt

        val localEntities = ArraySet(entities)
        val localConstraints = ArraySet(constraints)

        updateAll(dt, localEntities)
        solveAll(dt, localConstraints)
        postUpdateAll(dt, localEntities)

        simStepListeners.forEach { it.invoke() }
    }

    /** Register [listener] to be invoked every time the [Simulator] completes one [step]. */
    fun addSimulationStepListener(listener: () -> Unit): DisposableHandle {
        simStepListeners += listener
        return DisposableHandle { simStepListeners -= listener }
    }
}

