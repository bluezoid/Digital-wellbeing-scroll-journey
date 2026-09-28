package com.achyutpal.scrolljourney.ui

import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry

/** Reads app strings the same way the screens do, so tests don't hard-code copy. */
fun str(@StringRes id: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

/** Records callback calls from a screen so a test can assert on them. */
class Calls {
    val log = mutableListOf<String>()
    fun record(name: String): () -> Unit = { log += name }
}
