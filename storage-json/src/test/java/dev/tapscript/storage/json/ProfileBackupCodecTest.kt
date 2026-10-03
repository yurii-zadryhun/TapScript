package dev.tapscript.storage.json

import dev.tapscript.engine.api.model.AutomationProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileBackupCodecTest {
    private val codec = ProfileBackupCodec()

    @Test
    fun roundTripsProfiles() {
        val source = listOf(
            AutomationProfile(
                id = "loot",
                name = "Loot",
                targetPackage = "com.example.game",
            ),
        )

        val decoded = codec.decode(codec.encode(source))

        assertEquals(source, decoded)
    }

    @Test
    fun acceptsSingleProfileJsonForConvenientImport() {
        val decoded = codec.decode(
            """{
              \"id\": \"single\",
              \"name\": \"Single profile\",
              \"targetPackage\": \"\",
              \"regions\": [],
              \"actions\": [],
              \"logic\": {\"mode\": \"RULES\", \"rules\": [], \"script\": \"\"},
              \"settings\": {\"minFrameIntervalMs\": 120, \"changeThreshold\": 0.02, \"postActionCooldownMs\": 0},
              \"schemaVersion\": 1
            }""".trimIndent(),
        )

        assertEquals("single", decoded.single().id)
    }

    @Test
    fun rejectsUnknownBackupVersion() {
        val error = runCatching {
            codec.decode(
                """{
                  \"format\": \"tapscript-profile-backup\",
                  \"version\": 999,
                  \"profiles\": []
                }""".trimIndent(),
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("Unsupported backup version"))
    }
}
