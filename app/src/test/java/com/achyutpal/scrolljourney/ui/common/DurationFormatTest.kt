package com.achyutpal.scrolljourney.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {
    @Test
    fun `formats minutes and hours`() {
        assertEquals("0m", formatMinutes(0))
        assertEquals("59m", formatMinutes(59))
        assertEquals("1h 00m", formatMinutes(60))
        assertEquals("1h 35m", formatMinutes(95))
        assertEquals("4h 00m", formatMinutes(240))
    }
}
