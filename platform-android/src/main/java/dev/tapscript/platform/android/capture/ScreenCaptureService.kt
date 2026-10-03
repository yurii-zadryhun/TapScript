package dev.tapscript.platform.android.capture

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.view.WindowManager
import dev.tapscript.engine.api.model.PixelSize
import dev.tapscript.engine.api.model.ScreenFrame

class ScreenCaptureService : Service() {
    private lateinit var dependencies: ScreenCaptureServiceDependencies
    private lateinit var workerThread: HandlerThread
    private lateinit var workerHandler: Handler
    private lateinit var notificationFactory: CaptureNotificationFactory

    private val sizePolicy = CaptureSizePolicy()
    private val imageConverter = ScreenImageConverter()

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var densityDpi: Int = 1
    private var interactionSize = PixelSize(1, 1)
    private var captureSize = PixelSize(1, 1)
    private var lastPublishedAtMs: Long = 0

    override fun onCreate() {
        super.onCreate()
        dependencies = application as? ScreenCaptureServiceDependencies
            ?: error("Application must implement ScreenCaptureServiceDependencies")
        workerThread = HandlerThread("TapScript-Capture").also { it.start() }
        workerHandler = Handler(workerThread.looper)
        notificationFactory = CaptureNotificationFactory(this).also { it.ensureChannel() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> workerHandler.post(::stopCapture)
            ACTION_START -> {
                val safeIntent = Intent(intent)
                workerHandler.post { startCapture(safeIntent) }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (::workerHandler.isInitialized) {
            workerHandler.removeCallbacksAndMessages(null)
        }
        releaseCapture(stopProjection = true)
        if (::workerThread.isInitialized) {
            workerThread.quitSafely()
        }
        if (::dependencies.isInitialized) {
            dependencies.captureStatusStore.update(CaptureState.Idle)
        }
        super.onDestroy()
    }

    private fun startCapture(intent: Intent) {
        if (mediaProjection != null) return
        dependencies.captureStatusStore.update(CaptureState.Starting)
        startForeground(NOTIFICATION_ID, notificationFactory.create())

        val resultData = extractProjectionData(intent) ?: run {
            fail("Missing MediaProjection permission data")
            return
        }
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)

        runCatching {
            val manager = getSystemService(MediaProjectionManager::class.java)
            val projection = requireNotNull(manager.getMediaProjection(resultCode, resultData)) {
                "MediaProjection permission was not granted"
            }
            projection.registerCallback(projectionCallback, workerHandler)
            mediaProjection = projection

            densityDpi = resources.displayMetrics.densityDpi.coerceAtLeast(1)
            val size = initialInteractionSize()
            createOrResizeCapture(size.width, size.height)
        }.onFailure { throwable ->
            fail(throwable.message ?: "Unable to start screen capture")
        }
    }

    private fun extractProjectionData(intent: Intent): Intent? = if (Build.VERSION.SDK_INT >= 33) {
        intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
    } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(EXTRA_RESULT_DATA)
    }

    private fun initialInteractionSize(): PixelSize {
        val metrics = resources.displayMetrics
        if (Build.VERSION.SDK_INT < 30) {
            @Suppress("DEPRECATION")
            return PixelSize(metrics.widthPixels, metrics.heightPixels)
        }
        val bounds = getSystemService(WindowManager::class.java).currentWindowMetrics.bounds
        return PixelSize(bounds.width(), bounds.height())
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            workerHandler.post(::handleProjectionStopped)
        }

        override fun onCapturedContentResize(width: Int, height: Int) {
            workerHandler.post {
                runCatching { createOrResizeCapture(width, height) }
                    .onFailure { fail(it.message ?: "Unable to resize screen capture") }
            }
        }
    }

    private fun createOrResizeCapture(sourceWidth: Int, sourceHeight: Int) {
        if (sourceWidth <= 0 || sourceHeight <= 0) return
        val projection = mediaProjection ?: return
        interactionSize = PixelSize(sourceWidth, sourceHeight)
        val resolvedSize = sizePolicy.resolve(sourceWidth, sourceHeight)
        val newReader = createImageReader(resolvedSize)

        try {
            val display = virtualDisplay
            if (display == null) {
                virtualDisplay = createVirtualDisplay(projection, newReader, resolvedSize)
            } else {
                display.setSurface(newReader.surface)
                display.resize(resolvedSize.width, resolvedSize.height, densityDpi)
            }
        } catch (throwable: Throwable) {
            newReader.setOnImageAvailableListener(null, null)
            newReader.close()
            throw throwable
        }

        val oldReader = imageReader
        imageReader = newReader
        captureSize = resolvedSize
        oldReader?.setOnImageAvailableListener(null, null)
        oldReader?.close()
        dependencies.captureStatusStore.update(CaptureState.Active(resolvedSize.width, resolvedSize.height))
    }

    private fun createImageReader(size: PixelSize): ImageReader =
        ImageReader.newInstance(size.width, size.height, PixelFormat.RGBA_8888, 2).apply {
            setOnImageAvailableListener(::onImageAvailable, workerHandler)
        }

    private fun createVirtualDisplay(
        projection: MediaProjection,
        reader: ImageReader,
        size: PixelSize,
    ): VirtualDisplay = requireNotNull(
        projection.createVirtualDisplay(
            "TapScriptCapture",
            size.width,
            size.height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            workerHandler,
        ),
    ) { "Unable to create virtual display" }

    private fun onImageAvailable(reader: ImageReader) {
        val image = acquireLatestImage(reader) ?: return
        try {
            if (reader !== imageReader) return
            val now = SystemClock.elapsedRealtime()
            if (now - lastPublishedAtMs < MIN_PUBLISH_INTERVAL_MS) return
            lastPublishedAtMs = now
            dependencies.screenFrameSink.publish(
                ScreenFrame(
                    bitmap = imageConverter.toBitmap(image),
                    capturedAtNanos = System.nanoTime(),
                    interactionSize = interactionSize,
                ),
            )
            dependencies.captureStatusStore.update(CaptureState.Active(captureSize.width, captureSize.height))
        } catch (throwable: Throwable) {
            dependencies.captureStatusStore.update(
                CaptureState.Error(throwable.message ?: "Frame conversion failed"),
            )
        } finally {
            image.close()
        }
    }

    private fun acquireLatestImage(reader: ImageReader): Image? = try {
        reader.acquireLatestImage()
    } catch (_: IllegalStateException) {
        // A queued callback can arrive after a resize closed the previous ImageReader.
        null
    }

    private fun handleProjectionStopped() {
        releaseCapture(stopProjection = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        dependencies.captureStatusStore.update(CaptureState.Idle)
        stopSelf()
    }

    private fun stopCapture() {
        releaseCapture(stopProjection = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        dependencies.captureStatusStore.update(CaptureState.Idle)
        stopSelf()
    }

    private fun releaseCapture(stopProjection: Boolean) {
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
        virtualDisplay?.release()
        virtualDisplay = null

        val projection = mediaProjection
        mediaProjection = null
        if (projection != null) {
            runCatching { projection.unregisterCallback(projectionCallback) }
            if (stopProjection) runCatching { projection.stop() }
        }
    }

    private fun fail(message: String) {
        releaseCapture(stopProjection = true)
        dependencies.captureStatusStore.update(CaptureState.Error(message))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val ACTION_START = "dev.tapscript.capture.START"
        const val ACTION_STOP = "dev.tapscript.capture.STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        private const val NOTIFICATION_ID = 1401
        private const val MIN_PUBLISH_INTERVAL_MS = 90L
    }
}
