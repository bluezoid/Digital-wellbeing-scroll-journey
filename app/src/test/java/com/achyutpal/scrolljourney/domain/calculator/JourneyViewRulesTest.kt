package com.achyutpal.scrolljourney.domain.calculator

import com.achyutpal.scrolljourney.domain.model.GraphicsCapability
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyViewRulesTest {

    private val capable = GraphicsCapability(isLowRamDevice = false, glEsVersion = 0x00030002) // GLES 3.2

    @Test
    fun `capable phone with the switch on shows 3D`() {
        assertTrue(JourneyViewRules.use3d(userEnabled = true, device = capable))
    }

    @Test
    fun `switch off shows the 2D card even on a capable phone`() {
        assertFalse(JourneyViewRules.use3d(userEnabled = false, device = capable))
    }

    @Test
    fun `low-RAM phones get the 2D card`() {
        val lowRam = capable.copy(isLowRamDevice = true)
        assertFalse(JourneyViewRules.deviceSupports3d(lowRam))
        assertFalse(JourneyViewRules.use3d(userEnabled = true, device = lowRam))
    }

    @Test
    fun `3D needs OpenGL ES 3_0 or newer`() {
        assertFalse(JourneyViewRules.deviceSupports3d(capable.copy(glEsVersion = 0x00020000)))
        assertTrue(JourneyViewRules.deviceSupports3d(capable.copy(glEsVersion = GraphicsCapability.GL_ES_3_0)))
    }
}
