package com.achyutpal.scrolljourney.data.apps

import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Loads app icons for display. An interface so ViewModel tests can skip real icons. */
interface AppIconSource {
    suspend fun loadIcon(packageName: String): ImageBitmap?
}

class AndroidAppIconSource(
    private val packageManager: PackageManager,
) : AppIconSource {

    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    override suspend fun loadIcon(packageName: String): ImageBitmap? {
        cache[packageName]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                packageManager.getApplicationIcon(packageName)
                    .toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX)
                    .asImageBitmap()
                    .also { cache[packageName] = it }
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    private companion object {
        const val ICON_SIZE_PX = 96
    }
}
