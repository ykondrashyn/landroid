package ru.queuejw.space.game

/** Simple platform-neutral RGBA color for core. */
data class KColor(val r: Float, val g: Float, val b: Float, val a: Float = 1f) {
    fun withAlpha(alpha: Float) = copy(a = alpha)

    companion object {
        fun fromArgb(argb: Int): KColor {
            val a = ((argb ushr 24) and 0xFF) / 255f
            val r = ((argb ushr 16) and 0xFF) / 255f
            val g = ((argb ushr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            return KColor(r, g, b, a)
        }

        val White = KColor(1f, 1f, 1f, 1f)
        val Gray = KColor(0.5f, 0.5f, 0.5f, 1f)
        val Yellow = KColor(1f, 1f, 0f, 1f)
    }
}

