package com.earlthekaiju.coach

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-xxhdpi")
class ToolchainProbeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun rendersBrandProbe() {
        compose.setContent { ToolchainProbe() }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/00-toolchain-probe.png")
    }
}
