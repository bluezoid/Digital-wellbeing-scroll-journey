package com.achyutpal.scrolljourney.domain.calculator

import com.achyutpal.scrolljourney.domain.calculator.LimitReminder.NEAR
import com.achyutpal.scrolljourney.domain.calculator.LimitReminder.REACHED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderPolicyTest {

    @Test
    fun `nothing below 80 percent`() {
        assertNull(ReminderPolicy.due(95, 120, emptySet())) // 79%
    }

    @Test
    fun `near at 80 percent, once`() {
        assertEquals(NEAR, ReminderPolicy.due(96, 120, emptySet())) // exactly 80%
        assertNull(ReminderPolicy.due(110, 120, setOf(NEAR)))
    }

    @Test
    fun `reached at the limit, once, even if near was never sent`() {
        assertEquals(REACHED, ReminderPolicy.due(120, 120, setOf(NEAR)))
        assertEquals(REACHED, ReminderPolicy.due(130, 120, emptySet())) // jumped straight past 80%
        assertNull(ReminderPolicy.due(200, 120, setOf(NEAR, REACHED)))
    }

    @Test
    fun `no almost-there after you're-over`() {
        assertNull(ReminderPolicy.due(100, 120, setOf(REACHED))) // limit raised later the same day
    }

    @Test
    fun `yellow lasts until 125 percent`() {
        assertEquals(150, ReminderPolicy.yellowUntilMinutes(120))
        assertEquals(75, ReminderPolicy.yellowUntilMinutes(60))
    }
}
