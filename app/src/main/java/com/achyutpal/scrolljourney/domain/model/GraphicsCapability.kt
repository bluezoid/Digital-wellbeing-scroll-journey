package com.achyutpal.scrolljourney.domain.model

/** What the phone's graphics can handle, read once from the system. */
data class GraphicsCapability(
    /** Android's own "this is a low-memory phone" flag (ActivityManager.isLowRamDevice). */
    val isLowRamDevice: Boolean = false,
    /** OpenGL ES version the phone supports, packed like 0x00030002 for 3.2. */
    val glEsVersion: Int = GL_ES_3_0,
) {
    companion object {
        const val GL_ES_3_0 = 0x00030000
    }
}
