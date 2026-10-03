package dev.tapscript.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import dev.tapscript.app.ui.DashboardViewModel
import dev.tapscript.app.ui.TapScriptApp
import dev.tapscript.app.ui.theme.TapScriptTheme
import dev.tapscript.platform.android.capture.CaptureState

class MainActivity : ComponentActivity() {
    private val graph: AppGraph
        get() = (application as TapScriptApplication).graph

    private lateinit var dashboardViewModel: DashboardViewModel
    private var closeHandled = false

    private val capturePermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        val granted = graph.captureController.isPermissionResultValid(result.resultCode, data)
        if (granted) {
            graph.captureStatusStore.update(CaptureState.Starting)
            graph.captureController.start(result.resultCode, requireNotNull(data))
        }
        if (::dashboardViewModel.isInitialized) {
            dashboardViewModel.onCapturePermissionResult(granted)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (handleCloseIntent(intent)) return

        enableEdgeToEdge()
        dashboardViewModel = ViewModelProvider(
            this,
            DashboardViewModel.Factory(graph),
        )[DashboardViewModel::class.java]

        setContent {
            TapScriptTheme {
                TapScriptApp(
                    viewModel = dashboardViewModel,
                    onRequestCapture = ::requestScreenCapture,
                    onOpenAccessibilitySettings = ::openAccessibilitySettings,
                    onRequestOverlayPermission = ::openOverlayPermissionSettings,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCloseIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (::dashboardViewModel.isInitialized) dashboardViewModel.refresh()
    }

    override fun onDestroy() {
        if (isFinishing && !isChangingConfigurations && !closeHandled) {
            graph.shutdownRuntime()
        }
        super.onDestroy()
    }

    private fun handleCloseIntent(intent: Intent?): Boolean {
        if (intent?.action != ACTION_CLOSE) return false
        closeHandled = true
        graph.shutdownRuntime()
        finishAndRemoveTask()
        return true
    }

    private fun requestScreenCapture() {
        capturePermission.launch(graph.captureController.createPermissionIntent())
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openOverlayPermissionSettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    companion object {
        const val ACTION_CLOSE = "dev.tapscript.app.action.CLOSE"
    }
}
