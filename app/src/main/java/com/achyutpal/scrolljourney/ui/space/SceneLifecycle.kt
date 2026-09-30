package com.achyutpal.scrolljourney.ui.space

import android.os.Handler
import android.os.Looper
import android.os.Message
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
 * [active] is false, and drops to CREATED when the scene leaves the composition.
 *
 * Why: SceneView draws only while its lifecycle is RESUMED (onResume posts its Choreographer
 * frame callback, onPause removes it). Capping it at STARTED pauses all 3D drawing, e.g. while
 * the dashboard hero is scrolled off screen, without throwing the loaded scene away.
 * It never goes to DESTROYED itself: the Scene composable tears the view down on dispose.
 *
 * State changes are applied from an asynchronous main-thread message, *between* frames.
 * SceneView's frame callback re-posts itself at the start of every frame, and Compose applies
 * changes inside a frame: removing the callback then is too late (it already ran this frame and
 * re-posted), so the loop would never stop, and after teardown a stray frame would run
 * ModelNode.onFrame → FilamentAsset.popRenderable() on a freed model (native SIGSEGV in
 * libgltfio). Outside a frame the pending callback really is removed. The message is
 * asynchronous so a view traversal barrier can't hold it back behind the next frame.
 */
@Composable
fun rememberSceneLifecycle(active: Boolean): Lifecycle {
    val parent = LocalLifecycleOwner.current.lifecycle
    val owner = remember { SceneLifecycleOwner() }
    DisposableEffect(parent, active) {
        fun sync() {
            val cap = if (active) Lifecycle.State.RESUMED else Lifecycle.State.STARTED
            owner.moveBetweenFrames(minOf(parent.currentState, cap).coerceAtLeast(Lifecycle.State.CREATED))
        }
        val observer = LifecycleEventObserver { _, _ -> sync() }
        parent.addObserver(observer)
        sync()
        onDispose {
            parent.removeObserver(observer)
            // Stop drawing (if only a key changed, the new effect's sync is queued right after this).
            owner.moveBetweenFrames(Lifecycle.State.CREATED)
        }
    }
    return owner.registry
}

private class SceneLifecycleOwner : LifecycleOwner {
    val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry

    private val mainHandler = Handler(Looper.getMainLooper())

    fun moveBetweenFrames(state: Lifecycle.State) {
        val message = Message.obtain(mainHandler) { registry.currentState = state }
        message.isAsynchronous = true
        mainHandler.sendMessage(message)
    }
}
