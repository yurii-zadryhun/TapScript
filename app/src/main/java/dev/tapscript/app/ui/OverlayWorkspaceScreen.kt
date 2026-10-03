package dev.tapscript.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.tapscript.app.overlay.OverlayWorkspaceState
import dev.tapscript.app.overlay.ScreenPickMode
import dev.tapscript.app.overlay.ScreenPickResult
import dev.tapscript.app.overlay.ScreenPickerState
import dev.tapscript.engine.api.model.ActionKind
import dev.tapscript.engine.api.model.ActionTarget
import dev.tapscript.engine.api.model.AutomationLogEntry
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.AutomationSessionStatus
import dev.tapscript.engine.api.model.DecisionRule
import dev.tapscript.engine.api.model.NormalizedPoint
import dev.tapscript.engine.api.model.RecognitionRegion
import dev.tapscript.engine.api.model.SessionPhase

/**
 * Overlay-safe workspace. It intentionally avoids AlertDialog, Dialog and DropdownMenu because those
 * create child Android windows that do not have an Activity token when hosted in TYPE_APPLICATION_OVERLAY.
 */
@Composable
fun OverlayWorkspaceScreen(
    state: OverlayWorkspaceState,
    runtimeStatus: AutomationSessionStatus,
    liveLogs: List<AutomationLogEntry>,
    screenPickerState: ScreenPickerState,
    onClose: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onSaveProfile: (AutomationProfile) -> Unit,
    onBeginLivePick: (ScreenPickMode, AutomationProfile?, String?) -> Unit,
    onConsumeLivePick: (Long) -> Unit,
    onFreezeFrame: () -> Unit,
    onClearFrozenFrame: () -> Unit,
    onResumeToApp: () -> Unit,
    onStopSession: () -> Unit,
    onDismissError: () -> Unit,
) {
    var tab by remember { mutableStateOf(WorkspaceTab.LIVE) }
    var draft by remember(state.selectedProfile?.id) { mutableStateOf(state.selectedProfile) }

    var editingRegion by remember { mutableStateOf<RecognitionRegion?>(null) }
    var regionEditorOpen by remember { mutableStateOf(false) }
    var editingAction by remember { mutableStateOf<ActionTarget?>(null) }
    var actionEditorOpen by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<DecisionRule?>(null) }
    var ruleEditorOpen by remember { mutableStateOf(false) }
    var redrawRegionId by remember { mutableStateOf<String?>(null) }
    var redrawActionId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(screenPickerState.resultSequence, screenPickerState.result) {
        val result = screenPickerState.result ?: return@LaunchedEffect
        val current = draft
        when (result) {
            is ScreenPickResult.Point -> {
                val redrawId = redrawActionId
                val existing = current?.actions?.firstOrNull { it.id == redrawId }
                if (current != null && redrawId != null && existing != null) {
                    draft = current.copy(
                        actions = current.actions.upsertBy(existing.copy(start = result.point, end = null)) { it.id },
                    )
                } else {
                    editingAction = ProfileDraftFactory.tapTarget(result.point)
                    actionEditorOpen = true
                }
                redrawActionId = null
            }
            is ScreenPickResult.Swipe -> {
                val redrawId = redrawActionId
                val existing = current?.actions?.firstOrNull { it.id == redrawId }
                if (current != null && redrawId != null && existing != null) {
                    draft = current.copy(
                        actions = current.actions.upsertBy(existing.copy(start = result.start, end = result.end)) { it.id },
                    )
                } else {
                    editingAction = ProfileDraftFactory.swipeTarget(result.start, result.end)
                    actionEditorOpen = true
                }
                redrawActionId = null
            }
            is ScreenPickResult.Region -> {
                val redrawId = redrawRegionId
                val existing = current?.regions?.firstOrNull { it.id == redrawId }
                if (current != null && redrawId != null && existing != null) {
                    draft = current.copy(
                        regions = current.regions.upsertBy(existing.copy(bounds = result.bounds)) { it.id },
                    )
                } else {
                    editingRegion = ProfileDraftFactory.textRegion(result.bounds)
                    regionEditorOpen = true
                }
                redrawRegionId = null
            }
        }
        tab = WorkspaceTab.PROFILE
        onConsumeLivePick(screenPickerState.resultSequence)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.34f)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.94f)
                .widthIn(max = 760.dp),
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.985f),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                WorkspaceHeader(
                    runtimeStatus = runtimeStatus,
                    selectedProfileName = draft?.name ?: state.selectedProfile?.name,
                    onClose = onClose,
                )
                WorkspaceTabs(tab = tab, onTabChange = { tab = it })
                HorizontalDivider()

                state.errorMessage?.let { message ->
                    InlineError(message = message, onDismiss = onDismissError)
                }

                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading workspace…")
                    }
                } else {
                    when (tab) {
                        WorkspaceTab.LIVE -> LiveWorkspaceTab(
                            runtimeStatus = runtimeStatus,
                            profile = draft ?: state.selectedProfile,
                            frozenFrame = state.frozenFrame,
                            isFreezingFrame = state.isFreezingFrame,
                            onFreezeFrame = onFreezeFrame,
                            onClearFrozenFrame = onClearFrozenFrame,
                            onResumeToApp = onResumeToApp,
                            onStopSession = onStopSession,
                        )
                        WorkspaceTab.PROFILE -> ProfileWorkspaceTab(
                            profiles = state.profiles,
                            draft = draft,
                            installedApps = state.installedApps,
                            editingRegion = editingRegion,
                            regionEditorOpen = regionEditorOpen,
                            editingAction = editingAction,
                            actionEditorOpen = actionEditorOpen,
                            editingRule = editingRule,
                            ruleEditorOpen = ruleEditorOpen,
                            onSelectProfile = { id ->
                                onSelectProfile(id)
                                draft = state.profiles.firstOrNull { it.id == id }
                                editingRegion = null
                                regionEditorOpen = false
                                editingAction = null
                                actionEditorOpen = false
                                editingRule = null
                                ruleEditorOpen = false
                            },
                            onDraftChange = { draft = it },
                            onSave = { profile ->
                                onSaveProfile(profile)
                                draft = profile
                            },
                            onPreview = {
                                onBeginLivePick(ScreenPickMode.PREVIEW, draft, null)
                            },
                            onPickRegion = {
                                redrawRegionId = null
                                redrawActionId = null
                                onBeginLivePick(ScreenPickMode.REGION, draft, null)
                            },
                            onPickTap = {
                                redrawRegionId = null
                                redrawActionId = null
                                onBeginLivePick(ScreenPickMode.POINT, draft, null)
                            },
                            onPickSwipe = {
                                redrawRegionId = null
                                redrawActionId = null
                                onBeginLivePick(ScreenPickMode.SWIPE, draft, null)
                            },
                            onEditRegion = {
                                editingRegion = it
                                regionEditorOpen = true
                                actionEditorOpen = false
                                ruleEditorOpen = false
                            },
                            onRedrawRegion = { region ->
                                redrawActionId = null
                                redrawRegionId = region.id
                                onBeginLivePick(ScreenPickMode.REGION, draft, region.id)
                            },
                            onCloseRegionEditor = { regionEditorOpen = false },
                            onApplyRegion = { region ->
                                val current = draft ?: return@ProfileWorkspaceTab
                                draft = current.copy(regions = current.regions.upsertBy(region) { it.id })
                                regionEditorOpen = false
                            },
                            onEditAction = {
                                editingAction = it
                                actionEditorOpen = true
                                regionEditorOpen = false
                                ruleEditorOpen = false
                            },
                            onRedrawAction = { action ->
                                redrawRegionId = null
                                redrawActionId = action.id
                                onBeginLivePick(
                                    if (action.kind == ActionKind.TAP) ScreenPickMode.POINT else ScreenPickMode.SWIPE,
                                    draft,
                                    action.id,
                                )
                            },
                            onCloseActionEditor = { actionEditorOpen = false },
                            onApplyAction = { action ->
                                val current = draft ?: return@ProfileWorkspaceTab
                                draft = current.copy(actions = current.actions.upsertBy(action) { it.id })
                                actionEditorOpen = false
                            },
                            onAddRule = {
                                editingRule = null
                                ruleEditorOpen = true
                                regionEditorOpen = false
                                actionEditorOpen = false
                            },
                            onEditRule = {
                                editingRule = it
                                ruleEditorOpen = true
                                regionEditorOpen = false
                                actionEditorOpen = false
                            },
                            onCloseRuleEditor = { ruleEditorOpen = false },
                            onApplyRule = { rule ->
                                val current = draft ?: return@ProfileWorkspaceTab
                                draft = current.copy(
                                    logic = current.logic.copy(rules = current.logic.rules.upsertBy(rule) { it.id }),
                                )
                                ruleEditorOpen = false
                            },
                        )
                        WorkspaceTab.LOGS -> LiveLogViewer(
                            logs = liveLogs,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceHeader(
    runtimeStatus: AutomationSessionStatus,
    selectedProfileName: String?,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("TapScript", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                selectedProfileName ?: runtimeStatus.profileName.ifBlank { "Workspace" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusPill(runtimeStatus.phase)
        TextButton(onClick = onClose) { Text("×") }
    }
}

@Composable
private fun StatusPill(phase: SessionPhase) {
    val label = phase.name.lowercase().replaceFirstChar { it.titlecase() }
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(label, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun WorkspaceTabs(tab: WorkspaceTab, onTabChange: (WorkspaceTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        WorkspaceTab.entries.forEach { item ->
            FilterChip(
                selected = tab == item,
                onClick = { onTabChange(item) },
                label = { Text(item.label) },
            )
        }
    }
}

@Composable
private fun InlineError(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDismiss) { Text("×") }
        }
    }
}

@Composable
private fun LiveWorkspaceTab(
    runtimeStatus: AutomationSessionStatus,
    profile: AutomationProfile?,
    frozenFrame: Bitmap?,
    isFreezingFrame: Boolean,
    onFreezeFrame: () -> Unit,
    onClearFrozenFrame: () -> Unit,
    onResumeToApp: () -> Unit,
    onStopSession: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RuntimeSummaryCard(runtimeStatus, onResumeToApp, onStopSession)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onFreezeFrame, enabled = !isFreezingFrame, modifier = Modifier.weight(1f)) {
                Text(if (isFreezingFrame) "Freezing…" else "Freeze frame")
            }
            if (frozenFrame != null) {
                OutlinedButton(onClick = onClearFrozenFrame) { Text("Clear") }
            }
        }
        frozenFrame?.let { FrozenFramePreview(it, profile?.regions.orEmpty()) }
        LiveVariablesCard(runtimeStatus)
        OcrPreviewCard(runtimeStatus)
    }
}

@Composable
private fun RuntimeSummaryCard(
    status: AutomationSessionStatus,
    onResumeToApp: () -> Unit,
    onStopSession: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(status.profileName.ifBlank { "Runtime" }, style = MaterialTheme.typography.titleMedium)
            Text(status.message.ifBlank { "Ready" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "frame ${status.metrics.frameAgeMs} ms • OCR ${status.metrics.recognitionMs} ms • decision ${status.metrics.decisionMs} ms",
                style = MaterialTheme.typography.bodySmall,
            )
            if (status.metrics.lastCommand.isNotBlank()) {
                Text("Last: ${status.metrics.lastCommand}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
            val active = status.phase !in setOf(SessionPhase.IDLE, SessionPhase.STOPPED, SessionPhase.ERROR)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onResumeToApp, enabled = active) { Text("▶") }
                Button(onClick = onStopSession, enabled = active) { Text("■") }
            }
        }
    }
}

@Composable
private fun LiveVariablesCard(status: AutomationSessionStatus) {
    val values = status.snapshot?.values.orEmpty().toSortedMap()
    EditorSection("Live variables") {
        if (values.isEmpty()) {
            Hint("No extracted variables yet.")
        } else {
            values.forEach { (name, value) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                ) {
                    Column(modifier = Modifier.padding(9.dp)) {
                        Text(name, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
                        Text(formatRuntimeValue(value), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun OcrPreviewCard(status: AutomationSessionStatus) {
    val observations = status.snapshot?.observations.orEmpty()
    EditorSection("OCR") {
        if (observations.isEmpty()) {
            Hint("No region observations yet.")
        } else {
            observations.forEach { observation ->
                Text(
                    "${observation.regionId} • ${observation.recognitionMs} ms",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    observation.errorMessage ?: observation.rawText.ifBlank { "(empty)" },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = if (observation.errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun FrozenFramePreview(bitmap: Bitmap, regions: List<RecognitionRegion>) {
    EditorSection("Frozen frame") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 430.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Frozen screen capture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val transform = ImageFitTransform.create(
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height,
                    canvasSize = IntSize(size.width.toInt(), size.height.toInt()),
                )
                regions.forEach { region ->
                    val topLeft = transform.toCanvas(NormalizedPoint(region.bounds.left, region.bounds.top))
                    val bottomRight = transform.toCanvas(NormalizedPoint(region.bounds.right, region.bounds.bottom))
                    drawRect(
                        color = Color(0xFF55DCB4),
                        topLeft = topLeft,
                        size = Size(
                            width = (bottomRight.x - topLeft.x).coerceAtLeast(1f),
                            height = (bottomRight.y - topLeft.y).coerceAtLeast(1f),
                        ),
                        style = Stroke(width = 3f),
                    )
                }
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ProfileWorkspaceTab(
    profiles: List<AutomationProfile>,
    draft: AutomationProfile?,
    installedApps: List<dev.tapscript.platform.android.app.LaunchableAppInfo>,
    editingRegion: RecognitionRegion?,
    regionEditorOpen: Boolean,
    editingAction: ActionTarget?,
    actionEditorOpen: Boolean,
    editingRule: DecisionRule?,
    ruleEditorOpen: Boolean,
    onSelectProfile: (String) -> Unit,
    onDraftChange: (AutomationProfile) -> Unit,
    onSave: (AutomationProfile) -> Unit,
    onPreview: () -> Unit,
    onPickRegion: () -> Unit,
    onPickTap: () -> Unit,
    onPickSwipe: () -> Unit,
    onEditRegion: (RecognitionRegion) -> Unit,
    onRedrawRegion: (RecognitionRegion) -> Unit,
    onCloseRegionEditor: () -> Unit,
    onApplyRegion: (RecognitionRegion) -> Unit,
    onEditAction: (ActionTarget) -> Unit,
    onRedrawAction: (ActionTarget) -> Unit,
    onCloseActionEditor: () -> Unit,
    onApplyAction: (ActionTarget) -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (DecisionRule) -> Unit,
    onCloseRuleEditor: () -> Unit,
    onApplyRule: (DecisionRule) -> Unit,
) {
    if (draft == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No profiles available. Create or import one in TapScript first.")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InlineProfileSelector(profiles, draft.id, onSelectProfile)
        OverlayProfileIdentitySection(draft, installedApps, onDraftChange)
        ProfileRuntimeSection(draft, onDraftChange)

        EditorSection("Visual layout") {
            Hint("The workspace hides while you draw on the real target screen. Existing geometry stays visible on the dimmed overlay.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPreview, modifier = Modifier.weight(1f)) { Text("Preview") }
                OutlinedButton(onClick = onPickRegion, modifier = Modifier.weight(1f)) { Text("OCR") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPickTap, modifier = Modifier.weight(1f)) { Text("Tap") }
                OutlinedButton(onClick = onPickSwipe, modifier = Modifier.weight(1f)) { Text("Swipe") }
            }
        }

        if (regionEditorOpen && editingRegion != null) {
            OverlayRegionEditor(editingRegion, onApplyRegion, onCloseRegionEditor)
        }
        ProfileRegionsSection(
            regions = draft.regions,
            onAdd = onPickRegion,
            onEdit = onEditRegion,
            onDelete = { region -> onDraftChange(draft.copy(regions = draft.regions.filterNot { it.id == region.id })) },
            onRedraw = onRedrawRegion,
        )

        if (actionEditorOpen && editingAction != null) {
            OverlayActionEditor(editingAction, onApplyAction, onCloseActionEditor)
        }
        ProfileActionsSection(
            actions = draft.actions,
            onAdd = onPickTap,
            onEdit = onEditAction,
            onDelete = { action -> onDraftChange(draft.copy(actions = draft.actions.filterNot { it.id == action.id })) },
            onRedraw = onRedrawAction,
        )

        if (ruleEditorOpen) {
            OverlayRuleEditor(editingRule, draft.actions, onApplyRule, onCloseRuleEditor)
        }
        ProfileLogicSection(
            logic = draft.logic,
            onLogicChange = { onDraftChange(draft.copy(logic = it)) },
            onAddRule = onAddRule,
            onEditRule = onEditRule,
            onDeleteRule = { rule ->
                onDraftChange(draft.copy(logic = draft.logic.copy(rules = draft.logic.rules.filterNot { it.id == rule.id })))
            },
        )

        HorizontalDivider()
        Button(onClick = { onSave(draft) }, modifier = Modifier.fillMaxWidth(), enabled = draft.name.isNotBlank()) {
            Text("Save profile")
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun InlineProfileSelector(
    profiles: List<AutomationProfile>,
    selectedId: String,
    onSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        profiles.forEach { profile ->
            FilterChip(
                selected = profile.id == selectedId,
                onClick = { onSelected(profile.id) },
                label = { Text(profile.name) },
            )
        }
    }
}

private fun formatRuntimeValue(value: Any?): String = when (value) {
    null -> "null"
    is String -> value
    is Number, is Boolean -> value.toString()
    is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}") { "${it.key}: ${it.value}" }
    is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]")
    else -> value.toString()
}

private enum class WorkspaceTab(val label: String) {
    LIVE("Live"),
    PROFILE("Profile"),
    LOGS("Logs"),
}
