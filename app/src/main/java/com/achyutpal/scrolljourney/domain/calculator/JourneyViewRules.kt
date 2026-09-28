package com.achyutpal.scrolljourney.domain.calculator

import com.achyutpal.scrolljourney.domain.model.GraphicsCapability

/** Decides whether the journey is shown as the live 3D scene or the flat 2D card. */
object JourneyViewRules {

    /** The 3D engine (Filament) needs OpenGL ES 3.0; low-memory phones get the 2D card to stay smooth. */
    fun deviceSupports3d(device: GraphicsCapability): Boolean =
        !device.isLowRamDevice && device.glEsVersion >= GraphicsCapability.GL_ES_3_0

    /** 3D only when the phone can run it and the user hasn't switched it off in Settings. */
    fun use3d(userEnabled: Boolean, device: GraphicsCapability): Boolean =
        userEnabled && deviceSupports3d(device)
}
