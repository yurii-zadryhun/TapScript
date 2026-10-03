package dev.tapscript.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.AutomationRunRecord
import dev.tapscript.engine.api.model.SessionPhase

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onCreateProfile: () -> Unit,
    onImportProfiles: () -> Unit,
    onExportProfiles: () -> Unit,
    onEditProfile: (AutomationProfile) -> Unit,
    onDeleteProfile: (AutomationProfile) -> Unit,
    onStartProfile: (AutomationProfile) -> Unit,
    onStopSession: () -> Unit,
    onLaunchTarget: (AutomationProfile) -> Unit,
    onDeleteRun: (AutomationRunRecord) -> Unit,
    onClearHistory: () -> Unit,
    onDismissError: () -> Unit,
    onDismissInfo: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { DashboardHeader() }
            item {
                ReadinessCard(
                    accessibilityEnabled = state.accessibilityEnabled,
                    captureState = state.captureState,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                )
            }
            item {
                FloatingControlsCard(
                    state = state.overlayState,
                    onRequestPermission = onRequestOverlayPermission,
                )
            }
            if (state.sessionStatus.phase !in setOf(SessionPhase.IDLE, SessionPhase.STOPPED)) {
                item { RuntimeCard(state = state, onStopSession = onStopSession) }
            }
            item {
                ProfilesHeader(
                    onCreateProfile = onCreateProfile,
                    onImportProfiles = onImportProfiles,
                    onExportProfiles = onExportProfiles,
                )
            }
            if (state.profiles.isEmpty()) {
                item { EmptyProfilesCard() }
            } else {
                items(state.profiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        canRun = state.accessibilityEnabled &&
                            state.sessionStatus.phase in setOf(SessionPhase.IDLE, SessionPhase.STOPPED, SessionPhase.ERROR),
                        onEdit = { onEditProfile(profile) },
                        onDelete = { onDeleteProfile(profile) },
                        onStart = { onStartProfile(profile) },
                        onLaunch = { onLaunchTarget(profile) },
                    )
                }
            }
            item {
                RunHistorySection(
                    records = state.runHistory,
                    onDelete = onDeleteRun,
                    onClear = onClearHistory,
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    state.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            confirmButton = { TextButton(onClick = onDismissError) { Text("OK") } },
            title = { Text("TapScript") },
            text = { Text(message) },
        )
    }
    state.infoMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissInfo,
            confirmButton = { TextButton(onClick = onDismissInfo) { Text("OK") } },
            title = { Text("Profiles") },
            text = { Text(message) },
        )
    }
}

@Composable
private fun DashboardHeader() {
    Column(modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)) {
        Text(
            text = "TapScript",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Visual automation, without hard-coded pixels.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfilesHeader(
    onCreateProfile: () -> Unit,
    onImportProfiles: () -> Unit,
    onExportProfiles: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Profiles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Regions + actions + rules/script",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onCreateProfile) { Text("+ New") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onImportProfiles, modifier = Modifier.weight(1f)) {
                Text("↓ Import")
            }
            OutlinedButton(onClick = onExportProfiles, modifier = Modifier.weight(1f)) {
                Text("↑ Export")
            }
        }
    }
}

@Composable
private fun EmptyProfilesCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "No profiles yet. Import a backup or create one to define what to read and where to tap.",
            modifier = Modifier.padding(18.dp),
        )
    }
}
