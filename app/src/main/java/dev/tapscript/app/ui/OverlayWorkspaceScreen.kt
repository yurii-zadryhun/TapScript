package dev.tapscript.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.geometry.Offset
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
import dev.tapscript.engine.api.model.ActionTarget
import dev.tapscript.engine.api.model.AutomationLogEntry
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.AutomationSessionStatus
import dev.tapscript.engine.api.model.DecisionRule
import dev.tapscript.engine.api.model.NormalizedPoint
import dev.tapscript.engine.api.model.RecognitionRegion
import dev.tapscript.engine.api.model.SessionPhase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OverlayWorkspaceScreen(
    state: OverlayWorkspaceState,
    runtimeStatus: AutomationSessionStatus,
    liveLogs: List<AutomationLogEntry>,
    screenPickerState: ScreenPickerState,
    onClose: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onSaveProfile: (AutomationProfile) -> Unit,
    onBeginLivePick: (ScreenPickMode) -> Unit,
    onConsumeLivePick: (Long) -> Unit,
    onFreezeFrame: () -> Unit,
    onClearFrozenFrame: () -> Unit,
    onTogglePause: () -> Unit,
    onStopSession: () -> Unit,
    onDismissError: () -> Unit,
) {
    var tab by remember { mutableStateOf(WorkspaceTab.LIVE) }
    var draft by remember(state.selectedProfile?.id) {
        mutableStateOf(state.selectedProfile)
    }
    var editingRegion by remember { mutableStateOf<RecognitionRegion?>(null) }
    var regionDialogOpen by remember { mutableStateOf(false) }
    var editingAction by remember { mutableStateOf<ActionTarget?>(null) }
    var actionDialogOpen by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<DecisionRule?>(null) }
    var ruleDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.selectedProfile) {
        if (draft?.id == state.selectedProfile?.id && draft == null) {
            draft = state.selectedProfile
        }
    }

    LaunchedEffect(screenPickerState.resultSequence, screenPickerState.result) {
        val result = screenPickerState.result ?: return@LaunchedEffect
        when (result) {
            is ScreenPickResult.Point -> {
                editingAction = ProfileDraftFactory.tapTarget(result.point)
                actionDialogOpen = true
                tab = WorkspaceTab.PROFILE
            }
            is ScreenPickResult.Swipe -> {
                editingAction = ProfileDraftFactory.swipeTarget(result.start, result.end)
                actionDialogOpen = true
                tab = WorkspaceTab.PROFILE
            }
            is ScreenPickResult.Region -> {
                editingRegion = ProfileDraftFactory.textRegion(result.bounds)
                regionDialogOpen = true
                tab = WorkspaceTab.PROFILE
            }
        }
        onConsumeLivePick(screenPickerState.resultSequence)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.30f)),
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
                            onTogglePause = onTogglePause,
                            onStopSession = onStopSession,
                        )
                        WorkspaceTab.PROFILE -> ProfileWorkspaceTab(
                            profiles = state.profiles,
                            draft = draft,
                            installedApps = state.installedApps,
                            onSelectProfile = { id ->
                                onSelectProfile(id)
                                draft = state.profiles.firstOrNull { it.id == id }
                            },
                            onDraftChange = { draft = it },
                            onSave = { profile ->
                                onSaveProfile(profile)
                                draft = profile
                            },
                            onPickRegion = { onBeginLivePick(ScreenPickMode.REGION) },
                            onPickTap = { onBeginLivePick(ScreenPickMode.POINT) },
                            onPickSwipe = { onBeginLivePick(ScreenPickMode.SWIPE) },
                            onAddRegion = {
                                editingRegion = null
                                regionDialogOpen = true
                            },
                            onEditRegion = {
                                editingRegion = it
                                regionDialogOpen = true
                            },
                            onAddAction = {
                                editingAction = null
                                actionDialogOpen = true
                            },
                            onEditAction = {
                                editingAction = it
                                actionDialogOpen = true
                            },
                            onAddRule = {
                                editingRule = null
                                ruleDialogOpen = true
                            },
                            onEditRule = {
                                editingRule = it
                                ruleDialogOpen = true
                            },
                        )
                        WorkspaceTab.LOGS -> LogsWorkspaceTab(liveLogs)
                    }
                }
            }
        }
    }

    state.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("TapScript") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = onDismissError) { Text("OK") } },
        )
    }

    if (regionDialogOpen) {
        TextRegionDialog(editingRegion, { regionDialogOpen = false }) { region ->
            val current = draft ?: return@TextRegionDialog
            draft = current.copy(regions = current.regions.upsertBy(region) { it.id })
            regionDialogOpen = false
        }
    }
    if (actionDialogOpen) {
        ActionTargetDialog(editingAction, { actionDialogOpen = false }) { action ->
            val current = draft ?: return@ActionTargetDialog
            draft = current.copy(actions = current.actions.upsertBy(action) { it.id })
            actionDialogOpen = false
        }
    }
    if (ruleDialogOpen) {
        val current = draft
        if (current != null) {
            DecisionRuleDialog(editingRule, current.actions, { ruleDialogOpen = false }) { rule ->
                draft = current.copy(
                    logic = current.logic.copy(rules = current.logic.rules.upsertBy(rule) { it.id }),
                )
                ruleDialogOpen = false
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
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("TapScript Workspace", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                selectedProfileName ?: runtimeStatus.profileName.ifBlank { "No profile selected" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusPill(runtimeStatus.phase)
        TextButton(onClick = onClose) { Text("Close") }
    }
}

@Composable
private fun StatusPill(phase: SessionPhase) {
    val label = phase.name.lowercase().replaceFirstChar { it.titlecase() }
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun WorkspaceTabs(tab: WorkspaceTab, onTabChange: (WorkspaceTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
private fun LiveWorkspaceTab(
    runtimeStatus: AutomationSessionStatus,
    profile: AutomationProfile?,
    frozenFrame: Bitmap?,
    isFreezingFrame: Boolean,
    onFreezeFrame: () -> Unit,
    onClearFrozenFrame: () -> Unit,
    onTogglePause: () -> Unit,
    onStopSession: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RuntimeSummaryCard(runtimeStatus, onTogglePause, onStopSession)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onFreezeFrame, enabled = !isFreezingFrame, modifier = Modifier.weight(1f)) {
                Text(if (isFreezingFrame) "Freezing…" else "Freeze current frame")
            }
            if (frozenFrame != null) {
                OutlinedButton(onClick = onClearFrozenFrame) { Text("Clear") }
            }
        }

        frozenFrame?.let { bitmap ->
            FrozenFramePreview(bitmap, profile?.regions.orEmpty())
        }

        LiveVariablesCard(runtimeStatus)
        OcrPreviewCard(runtimeStatus)
    }
}

@Composable
private fun RuntimeSummaryCard(
    status: AutomationSessionStatus,
    onTogglePause: () -> Unit,
    onStopSession: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(status.profileName.ifBlank { "Runtime" }, style = MaterialTheme.typography.titleMedium)
            Text(status.message.ifBlank { "Ready" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val metrics = status.metrics
            Text(
                "frame ${metrics.frameAgeMs} ms • OCR ${metrics.recognitionMs} ms • decision ${metrics.decisionMs} ms",
                style = MaterialTheme.typography.bodySmall,
            )
            if (metrics.lastCommand.isNotBlank()) {
                Text("Last: ${metrics.lastCommand}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
            val active = status.phase !in setOf(SessionPhase.IDLE, SessionPhase.STOPPED, SessionPhase.ERROR)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onTogglePause, enabled = active) {
                    Text(if (status.phase == SessionPhase.PAUSED) "Resume" else "Pause")
                }
                Button(onClick = onStopSession, enabled = active) { Text("Stop") }
            }
        }
    }
}

@Composable
private fun LiveVariablesCard(status: AutomationSessionStatus) {
    val values = status.snapshot?.values.orEmpty().toSortedMap()
    EditorSection("Live variables") {
        if (values.isEmpty()) {
            Hint("No extracted variables yet. They appear here after the next recognized frame.")
        } else {
            values.forEach { (name, value) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(name, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
                        Text(formatRuntimeValue(value), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun OcrPreviewCard(status: AutomationSessionStatus) {
    val observations = status.snapshot?.observations.orEmpty()
    EditorSection("OCR preview") {
        if (observations.isEmpty()) {
            Hint("No region observations yet.")
        } else {
            observations.forEach { observation ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "${observation.regionId} • ${observation.recognitionMs} ms",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (observation.errorMessage != null) {
                            Text(observation.errorMessage, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(
                                observation.rawText.ifBlank { "(empty OCR result)" },
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrozenFramePreview(bitmap: Bitmap, regions: List<RecognitionRegion>) {
    EditorSection("Frozen frame") {
        Hint("The workspace hides itself for one capture frame. Configured OCR regions are outlined below.")
        BoxWithConstraints(
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
                        color = Color(0xFF7AA2FF),
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

@Composable
private fun ProfileWorkspaceTab(
    profiles: List<AutomationProfile>,
    draft: AutomationProfile?,
    installedApps: List<dev.tapscript.platform.android.app.LaunchableAppInfo>,
    onSelectProfile: (String) -> Unit,
    onDraftChange: (AutomationProfile) -> Unit,
    onSave: (AutomationProfile) -> Unit,
    onPickRegion: () -> Unit,
    onPickTap: () -> Unit,
    onPickSwipe: () -> Unit,
    onAddRegion: () -> Unit,
    onEditRegion: (RecognitionRegion) -> Unit,
    onAddAction: () -> Unit,
    onEditAction: (ActionTarget) -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (DecisionRule) -> Unit,
) {
    if (draft == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No profiles available. Create one in the main TapScript app first.")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ProfileSelector(profiles, draft.id, onSelectProfile)
        ProfileIdentitySection(draft, installedApps, onDraftChange)
        ProfileRuntimeSection(draft, onDraftChange)

        EditorSection("Live visual authoring") {
            Hint("TapScript stays over the target app. Pick coordinates directly on the live screen; no manual X/Y entry is needed.")
            Button(onClick = onPickRegion, modifier = Modifier.fillMaxWidth()) { Text("Pick OCR region on screen") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPickTap, modifier = Modifier.weight(1f)) { Text("Pick tap") }
                OutlinedButton(onClick = onPickSwipe, modifier = Modifier.weight(1f)) { Text("Pick swipe") }
            }
        }

        ProfileRegionsSection(
            regions = draft.regions,
            onAdd = onAddRegion,
            onEdit = onEditRegion,
            onDelete = { region ->
                onDraftChange(draft.copy(regions = draft.regions.filterNot { it.id == region.id }))
            },
        )
        ProfileActionsSection(
            actions = draft.actions,
            onAdd = onAddAction,
            onEdit = onEditAction,
            onDelete = { action ->
                onDraftChange(draft.copy(actions = draft.actions.filterNot { it.id == action.id }))
            },
        )
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
private fun ProfileSelector(
    profiles: List<AutomationProfile>,
    selectedId: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = profiles.firstOrNull { it.id == selectedId }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(selected?.name ?: "Choose profile")
                Text(selected?.id.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = { Text(profile.name) },
                    onClick = {
                        expanded = false
                        onSelected(profile.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun LogsWorkspaceTab(logs: List<AutomationLogEntry>) {
    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No live logs for the current run.")
        }
        return
    }

    val formatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        items(logs.takeLast(500), key = { "${it.timestampEpochMs}:${it.message.hashCode()}" }) { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "${formatter.format(Date(entry.timestampEpochMs))}  ${entry.level.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(entry.message, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
            }
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
