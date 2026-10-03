package dev.tapscript.app

import android.app.Application
import dev.tapscript.app.diagnostics.CrashReportStore
import dev.tapscript.engine.api.ports.ScreenFrameSink
import dev.tapscript.platform.android.capture.CaptureStatusStore
import dev.tapscript.platform.android.capture.ScreenCaptureServiceDependencies

class TapScriptApplication : Application(), ScreenCaptureServiceDependencies {
    lateinit var graph: AppGraph
        private set

    override val screenFrameSink: ScreenFrameSink
        get() = graph.frameBus

    override val captureStatusStore: CaptureStatusStore
        get() = graph.captureStatusStore

    override fun onCreate() {
        super.onCreate()
        val crashReportStore = CrashReportStore(this).also { it.install() }
        graph = AppGraph(this, crashReportStore)
        graph.seedDefaults()
    }
}
