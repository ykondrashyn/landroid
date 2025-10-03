package ru.queuejw.space.game

/** Minimal ArraySet replacement for core (no Android dependency). */
class ArraySet<T>() : MutableSet<T> {
    private val backing = LinkedHashSet<T>()

    constructor(initialCapacity: Int) : this() // capacity hint ignored
    constructor(other: Collection<T>) : this() { addAll(other) }

    override val size: Int get() = backing.size
    override fun add(element: T): Boolean = backing.add(element)
    override fun addAll(elements: Collection<T>): Boolean = backing.addAll(elements)
    override fun clear() = backing.clear()
    override fun iterator(): MutableIterator<T> = backing.iterator()
    override fun remove(element: T): Boolean = backing.remove(element)
    override fun removeAll(elements: Collection<T>): Boolean = backing.removeAll(elements)
    override fun retainAll(elements: Collection<T>): Boolean = backing.retainAll(elements)
    override fun contains(element: T): Boolean = backing.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = backing.containsAll(elements)
    override fun isEmpty(): Boolean = backing.isEmpty()
}

/** Tiny DisposableHandle to avoid pulling kotlinx-coroutines into core. */
fun interface DisposableHandle { fun dispose() }

