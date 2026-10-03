package dev.tapscript.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tapscript.engine.api.model.*
import dev.tapscript.platform.android.app.LaunchableAppInfo

@Composable
fun ProfileEditorScreen(
    initialProfile: AutomationProfile,
    installedApps: List<LaunchableAppInfo>,
    onCancel: () -> Unit,
    onSave: (AutomationProfile) -> Unit,
) {
    var profile by remember(initialProfile.id) { mutableStateOf(initialProfile) }
    var referenceBitmap by remember(initialProfile.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var visualPickMode by remember { mutableStateOf<VisualPickMode?>(null) }
    val context = LocalContext.current
    val screenshotPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selected ->
            ReferenceBitmapLoader.load(context, selected)?.let { loaded ->
                referenceBitmap?.recycle()
                referenceBitmap = loaded
            }
        }
    }
    var editingRegion by remember { mutableStateOf<RecognitionRegion?>(null) }
    var regionDialogOpen by remember { mutableStateOf(false) }
    var editingAction by remember { mutableStateOf<ActionTarget?>(null) }
    var actionDialogOpen by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<DecisionRule?>(null) }
    var ruleDialogOpen by remember { mutableStateOf(false) }

    DisposableEffect(initialProfile.id) {
        onDispose {
            referenceBitmap?.takeUnless { it.isRecycled }?.recycle()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            EditorHeader(onCancel)
            ProfileIdentitySection(profile, installedApps) { profile = it }
            ProfileRuntimeSection(profile) { profile = it }
            AuthoringReferenceSection(
                bitmap = referenceBitmap,
                onImportScreenshot = { screenshotPicker.launch("image/*") },
                onDrawRegion = { visualPickMode = VisualPickMode.REGION },
                onPlaceTap = { visualPickMode = VisualPickMode.POINT },
                onDrawSwipe = { visualPickMode = VisualPickMode.SWIPE },
            )
            ProfileRegionsSection(
                regions = profile.regions,
                onAdd = {
                    editingRegion = null
                    regionDialogOpen = true
                },
                onEdit = {
                    editingRegion = it
                    regionDialogOpen = true
                },
                onDelete = { region ->
                    profile = profile.copy(regions = profile.regions.filterNot { it.id == region.id })
                },
            )
            ProfileActionsSection(
                actions = profile.actions,
                onAdd = {
                    editingAction = null
                    actionDialogOpen = true
                },
                onEdit = {
                    editingAction = it
                    actionDialogOpen = true
                },
                onDelete = { action ->
                    profile = profile.copy(actions = profile.actions.filterNot { it.id == action.id })
                },
            )
            ProfileLogicSection(
                logic = profile.logic,
                onLogicChange = { profile = profile.copy(logic = it) },
                onAddRule = {
                    editingRule = null
                    ruleDialogOpen = true
                },
                onEditRule = {
                    editingRule = it
                    ruleDialogOpen = true
                },
                onDeleteRule = { rule ->
                    profile = profile.copy(
                        logic = profile.logic.copy(rules = profile.logic.rules.filterNot { it.id == rule.id }),
                    )
                },
            )
            EditorSaveRow(
                canSave = profile.name.isNotBlank(),
                onCancel = onCancel,
                onSave = { onSave(profile) },
            )
        }
    }

    referenceBitmap?.let { bitmap ->
        when (visualPickMode) {
            VisualPickMode.REGION -> VisualCoordinatePickerDialog(
                bitmap = bitmap,
                mode = VisualPickMode.REGION,
                onDismiss = { visualPickMode = null },
                onRegionPicked = { bounds ->
                    editingRegion = ProfileDraftFactory.textRegion(bounds)
                    visualPickMode = null
                    regionDialogOpen = true
                },
            )
            VisualPickMode.POINT -> VisualCoordinatePickerDialog(
                bitmap = bitmap,
                mode = VisualPickMode.POINT,
                onDismiss = { visualPickMode = null },
                onPointPicked = { point ->
                    editingAction = ProfileDraftFactory.tapTarget(point)
                    visualPickMode = null
                    actionDialogOpen = true
                },
            )
            VisualPickMode.SWIPE -> VisualCoordinatePickerDialog(
                bitmap = bitmap,
                mode = VisualPickMode.SWIPE,
                onDismiss = { visualPickMode = null },
                onSwipePicked = { start, end ->
                    editingAction = ProfileDraftFactory.swipeTarget(start, end)
                    visualPickMode = null
                    actionDialogOpen = true
                },
            )
            null -> Unit
        }
    }

    if (regionDialogOpen) {
        TextRegionDialog(editingRegion, { regionDialogOpen = false }) { region ->
            profile = profile.copy(regions = profile.regions.upsertBy(region) { it.id })
            regionDialogOpen = false
        }
    }
    if (actionDialogOpen) {
        ActionTargetDialog(editingAction, { actionDialogOpen = false }) { action ->
            profile = profile.copy(actions = profile.actions.upsertBy(action) { it.id })
            actionDialogOpen = false
        }
    }
    if (ruleDialogOpen) {
        DecisionRuleDialog(editingRule, profile.actions, { ruleDialogOpen = false }) { rule ->
            profile = profile.copy(logic = profile.logic.copy(rules = profile.logic.rules.upsertBy(rule) { it.id }))
            ruleDialogOpen = false
        }
    }
}

@Composable
private fun EditorHeader(onCancel: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Profile editor", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Configure regions and actions as data; reserve JavaScript for real logic.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onCancel) { Text("Close") }
    }
}

@Composable
private fun EditorSaveRow(canSave: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    HorizontalDivider()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onCancel) { Text("Cancel") }
        Button(onClick = onSave, enabled = canSave) { Text("Save profile") }
    }
}
