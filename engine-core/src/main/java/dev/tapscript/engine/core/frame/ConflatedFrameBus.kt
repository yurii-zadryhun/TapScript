package dev.tapscript.engine.core.frame

import dev.tapscript.engine.api.model.ScreenFrame
import dev.tapscript.engine.api.ports.ScreenFrameSink
import dev.tapscript.engine.api.ports.ScreenFrameSource
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

class ConflatedFrameBus : ScreenFrameSource, ScreenFrameSink {
    private val frames = Channel<ScreenFrame>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        onUndeliveredElement = ::recycle,
    )

    override fun publish(frame: ScreenFrame) {
        val result = frames.trySend(frame)
        if (result.isFailure) recycle(frame)
    }

    override suspend fun awaitFrame(afterTimestampNanos: Long): ScreenFrame {
        while (true) {
            val frame = frames.receive()
            if (frame.capturedAtNanos > afterTimestampNanos) return frame
            recycle(frame)
        }
    }

    private fun recycle(frame: ScreenFrame) {
        if (!frame.bitmap.isRecycled) frame.bitmap.recycle()
    }
}
