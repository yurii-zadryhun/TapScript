package dev.tapscript.app

import android.content.Context
import dev.tapscript.app.logging.CompositeAutomationLogger
import dev.tapscript.app.logging.SessionHistoryRecorder
import dev.tapscript.app.overlay.FloatingOverlayController
import dev.tapscript.app.overlay.ScreenPickerController
import dev.tapscript.engine.core.action.ActionResolver
import dev.tapscript.engine.core.action.CommandExecutor
import dev.tapscript.engine.core.decision.DefaultDecisionEngine
import dev.tapscript.engine.core.decision.RuleEvaluator
import dev.tapscript.engine.core.frame.ConflatedFrameBus
import dev.tapscript.engine.core.frame.SampledFrameChangeDetector
import dev.tapscript.engine.core.recognition.BitmapRegionCropper
import dev.tapscript.engine.core.recognition.RecognitionPipeline
import dev.tapscript.engine.core.recognition.RegexValueExtractor
import dev.tapscript.engine.core.runtime.AutomationRunner
import dev.tapscript.platform.android.accessibility.AccessibilityForegroundAppReader
import dev.tapscript.platform.android.accessibility.AccessibilityStatusReader
import dev.tapscript.platform.android.accessibility.AndroidGestureDispatcher
import dev.tapscript.platform.android.app.AndroidInstalledAppProvider
import dev.tapscript.platform.android.app.AndroidPackageLauncher
import dev.tapscript.platform.android.capture.CaptureServiceController
import dev.tapscript.platform.android.capture.CaptureStatusStore
import dev.tapscript.platform.android.logging.AndroidAutomationLogger
import dev.tapscript.recognition.mlkit.MlKitTextRecognizer
import dev.tapscript.scripting.rhino.RhinoScriptEngine
import dev.tapscript.storage.json.JsonProfileRepository
import dev.tapscript.storage.json.JsonSessionHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppGraph(context: Context) {
    private val applicationContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val logcatLogger = AndroidAutomationLogger()
    val sessionHistoryRepository = JsonSessionHistoryRepository(applicationContext)
    val historyRecorder = SessionHistoryRecorder(sessionHistoryRepository)
    val logger = CompositeAutomationLogger(logcatLogger, historyRecorder)

    val frameBus = ConflatedFrameBus()
    val captureStatusStore = CaptureStatusStore()
    val captureController = CaptureServiceController(applicationContext)
    val accessibilityStatusReader = AccessibilityStatusReader(applicationContext)
    val foregroundAppReader = AccessibilityForegroundAppReader()
    val packageLauncher = AndroidPackageLauncher(applicationContext)
    val installedAppProvider = AndroidInstalledAppProvider(applicationContext)
    val profileRepository = JsonProfileRepository(applicationContext)
    val screenPickerController = ScreenPickerController(applicationContext)

    private val textRecognizer = MlKitTextRecognizer()
    private val recognitionPipeline = RecognitionPipeline(
        textRecognizer = textRecognizer,
        cropper = BitmapRegionCropper(),
        extractor = RegexValueExtractor(),
    )
    private val decisionEngine = DefaultDecisionEngine(
        ruleEvaluator = RuleEvaluator(),
        scriptEngine = RhinoScriptEngine(),
        logger = logger,
    )
    private val commandExecutor = CommandExecutor(
        gestureDispatcher = AndroidGestureDispatcher(),
        actionResolver = ActionResolver(),
        logger = logger,
    )

    val automationRunner = AutomationRunner(
        frameSource = frameBus,
        changeDetector = SampledFrameChangeDetector(),
        recognitionPipeline = recognitionPipeline,
        decisionEngine = decisionEngine,
        commandExecutor = commandExecutor,
        foregroundAppReader = foregroundAppReader,
        logger = logger,
    )

    val sessionManager = AutomationSessionManager(
        scope = applicationScope,
        profileRepository = profileRepository,
        runner = automationRunner,
        logger = logger,
        historyRecorder = historyRecorder,
    )

    val overlayController = FloatingOverlayController(
        context = applicationContext,
        sessionStatus = sessionManager.status,
        onStopSession = sessionManager::stop,
    )

    fun seedDefaults() {
        applicationScope.launch {
            if (profileRepository.list().isEmpty()) {
                profileRepository.save(SampleProfiles.lootEvaluator())
            }
        }
    }
}
