package com.achyutpal.scrolljourney.domain.model

/**
 * Whether Android currently lets us read app usage (Settings > Usage access).
 *
 * - [UNKNOWN]: not checked yet (initial UI state).
 * - [GRANTED]: the user has switched Usage Access on.
 * - [REVOKED]: Usage Access is currently off, whether never granted or switched off later.
 */
enum class UsageAccessState {
    GRANTED,
    REVOKED,
    UNKNOWN,
}
