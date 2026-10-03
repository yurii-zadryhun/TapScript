package dev.tapscript.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
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
    onEditProfile: (AutomationProfile) -> Unit,
    onDeleteProfile: (AutomationProfile) -> Unit,
    onStartProfile: (AutomationProfile) -> Unit,
    onStopSession: () -> Unit,
    onLaunchTarget: (AutomationProfile) -> Unit,
    onDeleteRun: (AutomationRunRecord) -> Unit,
    onClearHistory: () -> Unit,
    onDismissError: () -> Unit,
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
            item { ProfilesHeader(onCreateProfile) }
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
private fun ProfilesHeader(onCreateProfile: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("Profiles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Regions + actions + rules/script",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onCreateProfile) { Text("New") }
    }
}

@Composable
private fun EmptyProfilesCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "No profiles yet. Create one to define what to read and where to tap.",
            modifier = Modifier.padding(18.dp),
        )
    }
}
