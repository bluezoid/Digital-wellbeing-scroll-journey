package com.achyutpal.scrolljourney.domain.calculator

/** The two limit reminders, each sent at most once per day. */
enum class LimitReminder { NEAR, REACHED }

/** Pure rule for when a limit reminder is due. Unit-tested. */
object ReminderPolicy {

    /** "Almost there" at 80% of the limit. */
    const val NEAR_PERCENT = 80

    /**
     * The reminder to send now, or null. Once REACHED was sent, NEAR is never sent afterwards
     * (no "almost there" after "you're over").
     */
    fun due(minutes: Int, limitMinutes: Int, alreadySent: Set<LimitReminder>): LimitReminder? = when {
        limitMinutes <= 0 -> null
        minutes >= limitMinutes -> LimitReminder.REACHED.takeIf { it !in alreadySent }
        minutes * 100 >= limitMinutes * NEAR_PERCENT ->
            LimitReminder.NEAR.takeIf { it !in alreadySent && LimitReminder.REACHED !in alreadySent }
        else -> null
    }

    /** Usage up to here is still a yellow day (LimitEvaluator: ≤ 125% of the limit). */
    fun yellowUntilMinutes(limitMinutes: Int): Int = limitMinutes * 125 / 100
}
