package dev.tapscript.storage.json

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.ports.ProfileRepository
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class JsonProfileRepository(
    context: Context,
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create(),
) : ProfileRepository {
    private val directory = File(context.filesDir, "profiles")
    private val mutex = Mutex()
    private val backupCodec = ProfileBackupCodec(gson)

    override suspend fun list(): List<AutomationProfile> = withContext(Dispatchers.IO) {
        mutex.withLock { listUnlocked() }
    }

    override suspend fun get(id: String): AutomationProfile? = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureDirectory()
            readProfile(profileFile(id))
        }
    }

    override suspend fun save(profile: AutomationProfile) = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureDirectory()
            writeProfile(profile)
        }
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = profileFile(id)
            if (file.exists() && !file.delete()) error("Could not delete ${file.name}")
        }
    }

    suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        mutex.withLock { backupCodec.encode(listUnlocked()) }
    }

    /**
     * Imports profiles by id. Existing matching ids are replaced; unrelated local profiles stay intact.
     */
    suspend fun importBackup(json: String): Int = withContext(Dispatchers.IO) {
        val imported = backupCodec.decode(json)
        mutex.withLock {
            ensureDirectory()
            imported.forEach(::writeProfile)
        }
        imported.size
    }

    private fun listUnlocked(): List<AutomationProfile> {
        ensureDirectory()
        return directory.listFiles { file -> file.extension == "json" }
            .orEmpty()
            .mapNotNull(::readProfile)
            .sortedBy { it.name.lowercase() }
    }

    private fun writeProfile(profile: AutomationProfile) {
        val destination = profileFile(profile.id)
        val temporary = File(directory, ".${profile.id}.${System.nanoTime()}.tmp")
        try {
            temporary.writeText(gson.toJson(profile), Charsets.UTF_8)
            replaceFile(temporary, destination)
        } finally {
            temporary.delete()
        }
    }

    private fun replaceFile(source: File, destination: File) {
        val sourcePath = source.toPath()
        val destinationPath = destination.toPath()
        try {
            Files.move(
                sourcePath,
                destinationPath,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun ensureDirectory() {
        if (!directory.exists()) check(directory.mkdirs()) { "Could not create profile directory" }
    }

    private fun profileFile(id: String): File = File(directory, "$id.json")

    private fun readProfile(file: File): AutomationProfile? = runCatching {
        gson.fromJson(file.readText(Charsets.UTF_8), AutomationProfile::class.java)
    }.getOrNull()
}
