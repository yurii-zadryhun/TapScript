package dev.tapscript.platform.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import dev.tapscript.engine.api.model.PixelPoint
import dev.tapscript.engine.api.ports.GestureDispatcher
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidGestureDispatcher : GestureDispatcher {
    private val mainHandler = Handler(Looper.getMainLooper())

    override suspend fun tap(point: PixelPoint): Result<Unit> = dispatch(
        path = Path().apply { moveTo(point.x.toFloat(), point.y.toFloat()) },
        durationMs = 45,
    )

    override suspend fun swipe(
        start: PixelPoint,
        end: PixelPoint,
        durationMs: Long,
    ): Result<Unit> = dispatch(
        path = Path().apply {
            moveTo(start.x.toFloat(), start.y.toFloat())
            lineTo(end.x.toFloat(), end.y.toFloat())
        },
        durationMs = durationMs.coerceIn(50, 10_000),
    )

    private suspend fun dispatch(path: Path, durationMs: Long): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            val service = AccessibilityServiceRegistry.service
            if (service == null) {
                continuation.resume(Result.failure(IllegalStateException("TapScript accessibility service is not enabled")))
                return@suspendCancellableCoroutine
            }

            mainHandler.post {
                if (!continuation.isActive) return@post
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
                    .build()

                val accepted = service.dispatchGesture(
                    gesture,
                    object : AccessibilityService.GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            if (continuation.isActive) continuation.resume(Result.success(Unit))
                        }

                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(IllegalStateException("Gesture was cancelled")))
                            }
                        }
                    },
                    mainHandler,
                )

                if (!accepted && continuation.isActive) {
                    continuation.resume(Result.failure(IllegalStateException("Android rejected the gesture")))
                }
            }
        }
}
