package com.achyutpal.scrolljourney.domain.calculator

/**
 * The one place the scroll-speed estimate lives. Tune it here only.
 *
 * Default 100 cm (1 m) of feed per minute of app use. For scale: the test phone's screen is
 * ~15 cm tall, so that's ~6–7 screen-heights a minute, roughly one Reels swipe every 15 s or a
 * slower read of a normal feed. It's an estimate, which is why the UI always says "estimated".
 */
data class DistanceConfig(
    val centimetresPerMinute: Long = DEFAULT_CENTIMETRES_PER_MINUTE,
) {
    init {
        require(centimetresPerMinute > 0) { "centimetresPerMinute must be positive" }
    }

    companion object {
        const val DEFAULT_CENTIMETRES_PER_MINUTE = 100L
    }
}
