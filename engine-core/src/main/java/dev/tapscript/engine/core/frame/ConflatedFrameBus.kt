package dev.tapscript.engine.core.frame

import android.graphics.Bitmap
import dev.tapscript.engine.api.model.ScreenFrame
import dev.tapscript.engine.api.ports.ScreenFrameSink
import dev.tapscript.engine.api.ports.ScreenFrameSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout

class ConflatedFrameBus : ScreenFrameSource, ScreenFrameSink {
    private val frames = Channel<ScreenFrame>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        onUndeliveredElement = ::recycle,
    )
    private val snapshotLock = Any()
    private var pendingSnapshot: CompletableDeferred<ScreenFrame>? = null

    override fun publish(frame: ScreenFrame) {
        completePendingSnapshot(frame)
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

    suspend fun snapshotNextFrame(timeoutMs: Long = 1_500): ScreenFrame {
        require(timeoutMs > 0) { "timeoutMs must be positive" }
        val request = CompletableDeferred<ScreenFrame>()
        val replaced = synchronized(snapshotLock) {
            val previous = pendingSnapshot
            pendingSnapshot = request
            previous
        }
        replaced?.cancel()

        return try {
            withTimeout(timeoutMs) { request.await() }
        } finally {
            synchronized(snapshotLock) {
                if (pendingSnapshot === request) pendingSnapshot = null
            }
        }
    }

    private fun completePendingSnapshot(frame: ScreenFrame) {
        val request = synchronized(snapshotLock) {
            pendingSnapshot.also { pendingSnapshot = null }
        } ?: return
        if (!request.isActive) return

        val config = frame.bitmap.config ?: Bitmap.Config.ARGB_8888
        val copy = frame.bitmap.copy(config, false) ?: return
        val snapshot = ScreenFrame(
            bitmap = copy,
            capturedAtNanos = frame.capturedAtNanos,
            interactionSize = frame.interactionSize,
        )
        if (!request.complete(snapshot)) recycle(snapshot)
    }

    private fun recycle(frame: ScreenFrame) {
        if (!frame.bitmap.isRecycled) frame.bitmap.recycle()
    }
}
