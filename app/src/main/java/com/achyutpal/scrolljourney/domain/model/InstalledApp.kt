package com.achyutpal.scrolljourney.domain.model

/** An app the user can launch from their home screen (candidate for tracking). */
data class InstalledApp(
    val packageName: String,
    val label: String,
)
