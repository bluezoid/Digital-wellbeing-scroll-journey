package com.achyutpal.scrolljourney.ui.space

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * A Lifecycle for a 3D Scene that follows the screen's lifecycle but stops at STARTED while
 * [active] is false.
 *
 * Why: SceneView draws only while its lifecycle is RESUMED (onResume posts the per-frame
 * callback, onPause removes it). Capping it at STARTED pauses all 3D drawing, e.g. while the
 * dashboard hero is scrolled off screen, without throwing the loaded scene away.
 * It never goes to DESTROYED itself: the Scene composable tears the view down on dispose.
 */
@Composable
fun rememberSceneLifecycle(active: Boolean): Lifecycle {
    val parent = LocalLifecycleOwner.current.lifecycle
    val owner = remember { SceneLifecycleOwner() }
    DisposableEffect(parent, active) {
        fun sync() {
            val cap = if (active) Lifecycle.State.RESUMED else Lifecycle.State.STARTED
            owner.registry.currentState = minOf(parent.currentState, cap).coerceAtLeast(Lifecycle.State.CREATED)
        }
        val observer = LifecycleEventObserver { _, _ -> sync() }
        parent.addObserver(observer)
        sync()
        onDispose { parent.removeObserver(observer) }
    }
    return owner.registry
}

private class SceneLifecycleOwner : LifecycleOwner {
    val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry
}
