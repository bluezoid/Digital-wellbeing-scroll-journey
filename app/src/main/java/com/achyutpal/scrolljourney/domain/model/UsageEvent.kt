package com.achyutpal.scrolljourney.domain.model

/**
 * One raw event from Android's usage log, translated into our own type so the
 * domain layer never touches android.app.usage.UsageEvents.
 */
data class UsageEvent(
    val packageName: String,
    val className: String,
    val timestamp: Long,
    val type: UsageEventType,
)

enum class UsageEventType {
    /** An activity of the app came to the foreground. */
    RESUMED,

    /** An activity of the app left the foreground. */
    PAUSED,

    /** Screen turned off: nothing is in the foreground any more. */
    SCREEN_OFF,

    /** Phone is shutting down. */
    SHUTDOWN,
}
