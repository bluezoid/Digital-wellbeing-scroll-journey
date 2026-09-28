package com.achyutpal.scrolljourney.ui.space

import io.github.sceneview.gesture.CameraGestureDetector.CameraManipulator
import io.github.sceneview.math.Position
import io.github.sceneview.math.Transform
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.lookTowards

/**
 * Connects SceneView's touch gestures (one-finger drag = orbit, two-finger drag = pan,
 * pinch = zoom) to [OrbitCameraState]. Animation is advanced from the scene's onFrame (see
 * SpaceScreen), so [update] does nothing here: SceneView only calls it after the first touch.
 */
class FocusOrbitManipulator(private val state: OrbitCameraState) : CameraManipulator {

    private var lastX = 0
    private var lastY = 0
    private var panning = false

    override fun setViewport(width: Int, height: Int) = Unit

    override fun getTransform(): Transform = cameraTransform(state)

    override fun grabBegin(x: Int, y: Int, strafe: Boolean) {
        lastX = x
        lastY = y
        panning = strafe
    }

    override fun grabUpdate(x: Int, y: Int) {
        val dx = (x - lastX).toFloat()
        val dy = (y - lastY).toFloat()
        if (panning) state.pan(dx, dy) else state.orbit(dx, dy)
        lastX = x
        lastY = y
    }

    override fun grabEnd() = Unit

    override fun scrollBegin(x: Int, y: Int, separation: Float) = Unit

    override fun scrollUpdate(x: Int, y: Int, prevSeparation: Float, currSeparation: Float) =
        state.zoom(prevSeparation, currSeparation)

    override fun scrollEnd() = Unit

    override fun update(deltaTime: Float) = Unit

    companion object {
        /**
         * Camera model matrix looking from the eye at the target, Y up.
         * NOTE: don't use SceneView's `lookAt(eye, target)`: it passes `target - eye` into kotlin-math's
         * `lookAt`, which subtracts `eye` again. That only looks right when the target is the origin.
         */
        fun cameraTransform(state: OrbitCameraState): Transform {
            val eye = state.eye()
            val target = state.target
            return lookTowards(
                eye = Position(eye.x, eye.y, eye.z),
                forward = Float3(target.x - eye.x, target.y - eye.y, target.z - eye.z),
                up = Float3(0f, 1f, 0f),
            )
        }
    }
}
