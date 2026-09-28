package com.achyutpal.scrolljourney.ui.space

import kotlin.math.hypot

/** A body as it appears on screen right now (pixels). */
data class ScreenBody(val body: SpaceBody, val x: Float, val y: Float, val radiusPx: Float, val inFront: Boolean)

/**
 * Which body did the user mean to tap? 3D hit-testing misses small, far planets, so pick in screen
 * space instead: every visible body gets a touch area of its drawn size plus a generous margin
 * ([minTouchPx] at least), and the closest match (relative to its touch area) wins.
 */
object SpacePicking {

    fun pick(tapX: Float, tapY: Float, bodies: List<ScreenBody>, minTouchPx: Float, marginPx: Float): SpaceBody? =
        bodies
            .filter { it.inFront }
            .mapNotNull { b ->
                val touchRadius = maxOf(minTouchPx, b.radiusPx + marginPx)
                val d = hypot(tapX - b.x, tapY - b.y)
                if (d <= touchRadius) b to d / touchRadius else null
            }
            .minByOrNull { it.second }
            ?.first?.body
}
