package dev.tapscript.platform.android.capture

import dev.tapscript.engine.api.ports.ScreenFrameSink

interface ScreenCaptureServiceDependencies {
    val screenFrameSink: ScreenFrameSink
    val captureStatusStore: CaptureStatusStore
}
