package dev.tapscript.storage.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import dev.tapscript.engine.api.model.ActionKind
import dev.tapscript.engine.api.model.AutomationProfile
import java.util.regex.Pattern

class ProfileBackupCodec(
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create(),
) {
    fun encode(profiles: List<AutomationProfile>): String {
        validateProfiles(profiles)
        return gson.toJson(
            ProfileBackupEnvelope(
                format = FORMAT,
                version = CURRENT_VERSION,
                profiles = profiles,
            ),
        )
    }

    fun decode(json: String): List<AutomationProfile> = try {
        require(json.length <= MAX_JSON_CHARS) { "Backup is too large" }
        val root = JsonParser.parseString(json)
        val profiles = when {
            root.isJsonArray -> root.asJsonArray.map { gson.fromJson(it, AutomationProfile::class.java) }
            root.isJsonObject && root.asJsonObject.has("profiles") -> {
                val envelope = gson.fromJson(root, ProfileBackupEnvelope::class.java)
                require(envelope.format == FORMAT) { "Unsupported backup format" }
                require(envelope.version in 1..CURRENT_VERSION) {
                    "Unsupported backup version ${envelope.version}"
                }
                envelope.profiles
            }
            root.isJsonObject -> listOf(gson.fromJson(root, AutomationProfile::class.java))
            else -> error("Backup must contain a profile or profile list")
        }
        validateProfiles(profiles)
        profiles
    } catch (throwable: Throwable) {
        throw IllegalArgumentException(
            "Invalid TapScript profile backup: ${throwable.message ?: throwable::class.java.simpleName}",
            throwable,
        )
    }

    private fun validateProfiles(profiles: List<AutomationProfile>) {
        require(profiles.size <= MAX_PROFILES) { "Backup contains too many profiles" }
        require(profiles.map { it.id }.distinct().size == profiles.size) { "Duplicate profile ids" }
        profiles.forEach(::validateProfile)
    }

    private fun validateProfile(profile: AutomationProfile) {
        require(profile.id.isNotBlank() && profile.id.length <= MAX_ID_LENGTH) { "Invalid profile id" }
        require(profile.name.isNotBlank() && profile.name.length <= MAX_NAME_LENGTH) {
            "Profile '${profile.id}' has an invalid name"
        }
        require(profile.schemaVersion in 1..AutomationProfile.CURRENT_SCHEMA_VERSION) {
            "Profile '${profile.name}' uses unsupported schema ${profile.schemaVersion}"
        }
        require(profile.targetPackage.length <= MAX_PACKAGE_LENGTH) { "Target package is too long" }
        require(profile.regions.size <= MAX_REGIONS) { "Profile '${profile.name}' has too many regions" }
        require(profile.actions.size <= MAX_ACTIONS) { "Profile '${profile.name}' has too many actions" }
        require(profile.logic.script.length <= MAX_SCRIPT_CHARS) { "Profile script is too large" }
        require(profile.regions.map { it.id }.distinct().size == profile.regions.size) { "Duplicate region ids" }
        require(profile.actions.map { it.id }.distinct().size == profile.actions.size) { "Duplicate action ids" }

        profile.regions.forEach { region ->
            require(region.id.isNotBlank()) { "Region id is required" }
            val bounds = region.bounds
            require(bounds.left in 0f..1f && bounds.top in 0f..1f && bounds.right in 0f..1f && bounds.bottom in 0f..1f)
            require(bounds.right > bounds.left && bounds.bottom > bounds.top) { "Invalid region bounds" }
            region.textConfig.extractors.forEach { extractor ->
                require(extractor.variable.length <= MAX_VARIABLE_LENGTH) { "Variable name is too long" }
                require(extractor.pattern.length <= MAX_REGEX_CHARS) { "Regex is too long" }
                Pattern.compile(extractor.pattern)
            }
        }

        profile.actions.forEach { action ->
            require(action.id.isNotBlank()) { "Action id is required" }
            require(action.start.x in 0f..1f && action.start.y in 0f..1f) { "Invalid action coordinate" }
            if (action.kind == ActionKind.SWIPE) {
                val end = requireNotNull(action.end) { "Swipe '${action.id}' has no end point" }
                require(end.x in 0f..1f && end.y in 0f..1f) { "Invalid swipe coordinate" }
            }
            require(action.durationMs in 0..60_000) { "Invalid action duration" }
        }

        require(profile.settings.minFrameIntervalMs in 1..60_000) { "Invalid frame interval" }
        require(profile.settings.changeThreshold in 0.0..1.0) { "Invalid change threshold" }
        require(profile.settings.postActionCooldownMs in 0..60_000) { "Invalid post-action cooldown" }
    }

    private data class ProfileBackupEnvelope(
        val format: String = FORMAT,
        val version: Int = CURRENT_VERSION,
        val profiles: List<AutomationProfile> = emptyList(),
    )

    private companion object {
        const val FORMAT = "tapscript-profile-backup"
        const val CURRENT_VERSION = 1
        const val MAX_JSON_CHARS = 5_000_000
        const val MAX_PROFILES = 500
        const val MAX_REGIONS = 500
        const val MAX_ACTIONS = 500
        const val MAX_ID_LENGTH = 200
        const val MAX_NAME_LENGTH = 500
        const val MAX_PACKAGE_LENGTH = 500
        const val MAX_VARIABLE_LENGTH = 500
        const val MAX_REGEX_CHARS = 50_000
        const val MAX_SCRIPT_CHARS = 500_000
    }
}
