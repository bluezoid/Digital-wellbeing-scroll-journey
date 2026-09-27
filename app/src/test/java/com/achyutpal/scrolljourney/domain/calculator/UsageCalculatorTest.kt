package com.achyutpal.scrolljourney.domain.calculator

import com.achyutpal.scrolljourney.domain.model.UsageEvent
import com.achyutpal.scrolljourney.domain.model.UsageEventType
import com.achyutpal.scrolljourney.domain.model.UsageEventType.PAUSED
import com.achyutpal.scrolljourney.domain.model.UsageEventType.RESUMED
import com.achyutpal.scrolljourney.domain.model.UsageEventType.SCREEN_OFF
import com.achyutpal.scrolljourney.domain.model.UsageEventType.SHUTDOWN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageCalculatorTest {

    private val insta = "com.instagram.android"
    private val yt = "com.google.android.youtube"
    private val chrome = "com.android.chrome"
    private val tracked = setOf(insta, yt)

    private val min = 60_000L
    private val start = 1_000_000L
    private val end = start + 24 * 60 * min

    private fun ev(pkg: String, minute: Long, type: UsageEventType, cls: String = "Main") =
        UsageEvent(pkg, cls, start + minute * min, type)

    private fun calc(vararg events: UsageEvent, windowStart: Long = start, windowEnd: Long = end) =
        UsageCalculator.foregroundMillis(events.toList(), windowStart, windowEnd, tracked)

    @Test
    fun `single session is counted`() {
        val result = calc(ev(insta, 10, RESUMED), ev(insta, 30, PAUSED))
        assertEquals(20 * min, result[insta])
    }

    @Test
    fun `multiple sessions add up`() {
        val result = calc(
            ev(insta, 10, RESUMED), ev(insta, 15, PAUSED),
            ev(insta, 60, RESUMED), ev(insta, 70, PAUSED),
        )
        assertEquals(15 * min, result[insta])
    }

    @Test
    fun `untracked apps are ignored`() {
        val result = calc(ev(chrome, 0, RESUMED), ev(chrome, 50, PAUSED), ev(yt, 0, RESUMED), ev(yt, 5, PAUSED))
        assertEquals(null, result[chrome])
        assertEquals(5 * min, result[yt])
    }

    @Test
    fun `switching activities inside one app is not double counted`() {
        val result = calc(
            ev(insta, 0, RESUMED, "Feed"),
            ev(insta, 5, RESUMED, "Reels"), // Reels opens before Feed's pause arrives
            ev(insta, 5, PAUSED, "Feed"),
            ev(insta, 12, PAUSED, "Reels"),
        )
        assertEquals(12 * min, result[insta])
    }

    @Test
    fun `app still open at window end counts until the end`() {
        val result = calc(ev(insta, 10, RESUMED), windowEnd = start + 25 * min)
        assertEquals(15 * min, result[insta])
    }

    @Test
    fun `app already open at window start counts from the start`() {
        // Scrolling across midnight: the RESUMED happened yesterday, only the PAUSED is in today's window.
        val result = calc(ev(insta, 7, PAUSED))
        assertEquals(7 * min, result[insta])
    }

    @Test
    fun `session across midnight is split between the two days`() {
        val midnight = start + 60 * min
        val events = listOf(ev(insta, 50, RESUMED), ev(insta, 65, PAUSED))
        val yesterday = UsageCalculator.foregroundMillis(events, start, midnight, tracked)
        val today = UsageCalculator.foregroundMillis(events, midnight, end, tracked)
        assertEquals(10 * min, yesterday[insta])
        assertEquals(5 * min, today[insta])
    }

    @Test
    fun `screen off ends open sessions`() {
        val result = calc(ev(insta, 0, RESUMED), ev(insta, 10, SCREEN_OFF), ev(insta, 40, PAUSED))
        assertEquals(10 * min, result[insta])
    }

    @Test
    fun `shutdown ends open sessions`() {
        val result = calc(ev(yt, 0, RESUMED), UsageEvent("android", "", start + 3 * min, SHUTDOWN))
        assertEquals(3 * min, result[yt])
    }

    @Test
    fun `events outside the window are ignored`() {
        val result = calc(
            UsageEvent(insta, "Main", start - 30 * min, RESUMED),
            UsageEvent(insta, "Main", start - 20 * min, PAUSED),
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `events are sorted before processing`() {
        val result = calc(ev(insta, 30, PAUSED), ev(insta, 10, RESUMED))
        assertEquals(20 * min, result[insta])
    }

    @Test
    fun `stray pause after a completed session is ignored`() {
        val result = calc(ev(insta, 0, RESUMED), ev(insta, 5, PAUSED), ev(insta, 9, PAUSED))
        assertEquals(5 * min, result[insta])
    }
}
