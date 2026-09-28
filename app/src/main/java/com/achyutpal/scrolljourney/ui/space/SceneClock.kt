package com.achyutpal.scrolljourney.ui.space

/** Seconds since the first frame, and the time since the previous frame, from the frame clock. */
internal class SceneClock {
    private var start = 0L
    private var last = 0L

    /** The scene time of the most recent frame (used when handling taps between frames). */
    var lastSeconds = 0f
        private set

    fun seconds(frameTimeNanos: Long): Float {
        if (start == 0L) start = frameTimeNanos
        lastSeconds = (frameTimeNanos - start) / 1_000_000_000f
        return lastSeconds
    }

    fun delta(frameTimeNanos: Long): Float {
        val d = if (last == 0L) 0f else (frameTimeNanos - last) / 1_000_000_000f
        last = frameTimeNanos
        return d
    }
}
