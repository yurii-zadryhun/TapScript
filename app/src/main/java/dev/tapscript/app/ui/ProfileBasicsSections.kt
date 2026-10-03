package dev.tapscript.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.platform.android.app.LaunchableAppInfo

@Composable
internal fun ProfileIdentitySection(
    profile: AutomationProfile,
    installedApps: List<LaunchableAppInfo>,
    onChange: (AutomationProfile) -> Unit,
) {
    var appPickerOpen by remember { mutableStateOf(false) }

    EditorSection("Identity") {
        OutlinedTextField(
            value = profile.name,
            onValueChange = { onChange(profile.copy(name = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Profile name") },
            singleLine = true,
        )

        Text("Automation scope", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = profile.targetPackage.isBlank(),
                onClick = { onChange(profile.copy(targetPackage = "")) },
                label = { Text("Whole screen") },
            )
            FilterChip(
                selected = profile.targetPackage.isNotBlank(),
                onClick = { appPickerOpen = true },
                label = { Text("Specific app") },
            )
        }

        if (profile.targetPackage.isNotBlank()) {
            val selected = installedApps.firstOrNull { it.packageName == profile.targetPackage }
            OutlinedButton(
                onClick = { appPickerOpen = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(selected?.label ?: "Selected app")
                    Text(
                        profile.targetPackage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                "The profile pauses while another app is in the foreground and resumes automatically when this app returns.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (appPickerOpen) {
        AppPickerDialog(
            apps = installedApps,
            onDismiss = { appPickerOpen = false },
            onSelected = { app ->
                onChange(profile.copy(targetPackage = app.packageName))
                appPickerOpen = false
            },
        )
    }
}

@Composable
private fun AppPickerDialog(
    apps: List<LaunchableAppInfo>,
    onDismiss: () -> Unit,
    onSelected: (LaunchableAppInfo) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val needle = query.trim()
        if (needle.isBlank()) apps else apps.filter {
            it.label.contains(needle, ignoreCase = true) ||
                it.packageName.contains(needle, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Choose target app") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search") },
                    singleLine = true,
                )
                if (filtered.isEmpty()) {
                    Text(
                        "No launchable apps match this search.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            TextButton(
                                onClick = { onSelected(app) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(app.label)
                                    Text(
                                        app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
internal fun ProfileRuntimeSection(profile: AutomationProfile, onChange: (AutomationProfile) -> Unit) {
    EditorSection("Runtime") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                label = "Min frame ms",
                value = profile.settings.minFrameIntervalMs.toString(),
                modifier = Modifier.weight(1f),
            ) { number ->
                onChange(profile.copy(settings = profile.settings.copy(minFrameIntervalMs = number.toLong().coerceIn(40, 5_000))))
            }
            NumberField(
                label = "Post-action ms",
                value = profile.settings.postActionCooldownMs.toString(),
                modifier = Modifier.weight(1f),
            ) { number ->
                onChange(profile.copy(settings = profile.settings.copy(postActionCooldownMs = number.toLong().coerceIn(0, 10_000))))
            }
        }
        NumberField(
            label = "Change threshold (0 disables gate)",
            value = profile.settings.changeThreshold.toString(),
            modifier = Modifier.fillMaxWidth(),
        ) { number ->
            onChange(profile.copy(settings = profile.settings.copy(changeThreshold = number.coerceIn(0.0, 1.0))))
        }
        Text(
            "Lower frame interval reacts faster. Change detection skips OCR while configured regions stay stable; post-action cooldown helps avoid repeat taps during UI transitions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
