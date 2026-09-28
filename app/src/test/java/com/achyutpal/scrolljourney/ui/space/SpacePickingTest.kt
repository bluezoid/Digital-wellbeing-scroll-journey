package com.achyutpal.scrolljourney.ui.space

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpacePickingTest {

    private fun pick(x: Float, y: Float, vararg b: ScreenBody) =
        SpacePicking.pick(x, y, b.toList(), minTouchPx = 90f, marginPx = 40f)

    @Test
    fun `tiny far planet is still tappable within the minimum touch area`() {
        assertEquals(SpaceBody.PLUTO, pick(560f, 300f, ScreenBody(SpaceBody.PLUTO, 500f, 300f, 3f, true)))
    }

    @Test
    fun `tap far from everything picks nothing`() {
        assertNull(pick(100f, 100f, ScreenBody(SpaceBody.MARS, 900f, 900f, 10f, true)))
    }

    @Test
    fun `bodies behind the camera are ignored`() {
        assertNull(pick(500f, 500f, ScreenBody(SpaceBody.EARTH, 500f, 500f, 20f, inFront = false)))
    }

    @Test
    fun `nearest body relative to its touch area wins`() {
        val result = pick(
            520f, 500f,
            ScreenBody(SpaceBody.SUN, 400f, 500f, 200f, true), // tap is inside the big Sun disc…
            ScreenBody(SpaceBody.EARTH, 530f, 500f, 8f, true), // …but right on top of Earth
        )
        assertEquals(SpaceBody.EARTH, result)
    }

    @Test
    fun `big body is picked anywhere on its disc`() {
        assertEquals(SpaceBody.JUPITER, pick(700f, 500f, ScreenBody(SpaceBody.JUPITER, 500f, 500f, 220f, true)))
    }
}
