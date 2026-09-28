package com.achyutpal.scrolljourney.ui.space

import io.github.sceneview.gesture.CameraGestureDetector.CameraManipulator
import io.github.sceneview.math.Position
import io.github.sceneview.math.Transform
import kotlin.math.sqrt

/**
 * Wraps SceneView's orbit camera and limits zoom: pinching can't push the camera inside the Sun
 * (closer than [minDistance] to [target]) or drift so far away that everything becomes a dot.
 * Everything else (360° rotation, pan) is passed straight through.
 */
class ClampedOrbitManipulator(
    private val delegate: CameraManipulator,
    private val target: Position,
    private val minDistance: Float,
    private val maxDistance: Float,
) : CameraManipulator by delegate {

    override fun scrollUpdate(x: Int, y: Int, prevSeparation: Float, currSeparation: Float) {
        val zoomingIn = currSeparation > prevSeparation // fingers moving apart
        val distance = distanceToTarget(delegate.getTransform())
        if (zoomingIn && distance <= minDistance) return
        if (!zoomingIn && distance >= maxDistance) return
        delegate.scrollUpdate(x, y, prevSeparation, currSeparation)
    }

    private fun distanceToTarget(transform: Transform): Float {
        val p = transform.w // the camera position is the translation column
        val dx = p.x - target.x
        val dy = p.y - target.y
        val dz = p.z - target.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
