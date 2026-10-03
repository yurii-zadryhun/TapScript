package dev.tapscript.engine.api.ports

import dev.tapscript.engine.api.model.ScreenFrame

interface ScreenFrameSource {
    suspend fun awaitFrame(afterTimestampNanos: Long): ScreenFrame
}

interface ScreenFrameSink {
    fun publish(frame: ScreenFrame)
}
