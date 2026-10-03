package dev.tapscript.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tapscript.engine.api.model.AutomationProfile

@Composable
internal fun ProfileIdentitySection(profile: AutomationProfile, onChange: (AutomationProfile) -> Unit) {
    EditorSection("Identity") {
        OutlinedTextField(
            value = profile.name,
            onValueChange = { onChange(profile.copy(name = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Profile name") },
            singleLine = true,
        )
        OutlinedTextField(
            value = profile.targetPackage,
            onValueChange = { onChange(profile.copy(targetPackage = it.trim())) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Target package (optional)") },
            placeholder = { Text("com.example.game") },
            singleLine = true,
        )
    }
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
