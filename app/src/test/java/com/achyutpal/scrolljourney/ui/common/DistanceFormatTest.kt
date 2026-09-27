package com.achyutpal.scrolljourney.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DistanceFormatTest {

    @Test
    fun `metres below one kilometre, kilometres above`() {
        assertEquals("0 m", formatDistance(0, Locale.US))
        assertEquals("137 m", formatDistance(13_700, Locale.US))
        assertEquals("999 m", formatDistance(99_900, Locale.US))
        assertEquals("1.0 km", formatDistance(100_000, Locale.US))
        assertEquals("1.3 km", formatDistance(125_000, Locale.US))
    }

    @Test
    fun `times formatting`() {
        assertEquals("2", formatTimes(2.0, Locale.US))
        assertEquals("1.7", formatTimes(1.666, Locale.US))
        assertEquals("12", formatTimes(12.4, Locale.US))
        assertEquals("45", formatTimes(45.0, Locale.US))
    }
}
