package dev.tapscript.storage.json

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dev.tapscript.engine.api.model.AutomationRunRecord
import dev.tapscript.engine.api.ports.SessionHistoryRepository
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class JsonSessionHistoryRepository(
    context: Context,
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create(),
) : SessionHistoryRepository {
    private val directory = File(context.filesDir, "run-history")
    private val mutex = Mutex()

    override suspend fun list(limit: Int): List<AutomationRunRecord> = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureDirectory()
            directory.listFiles { file -> file.extension == "json" }
                .orEmpty()
                .asSequence()
                .mapNotNull(::readRecord)
                .sortedByDescending { it.startedAtEpochMs }
                .take(limit.coerceAtLeast(0))
                .toList()
        }
    }

    override suspend fun save(record: AutomationRunRecord) = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureDirectory()
            val destination = recordFile(record.id)
            val temporary = File(directory, ".${record.id}.${System.nanoTime()}.tmp")
            try {
                temporary.writeText(gson.toJson(record), Charsets.UTF_8)
                replaceFile(temporary, destination)
            } finally {
                temporary.delete()
            }
        }
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = recordFile(id)
            if (file.exists() && !file.delete()) error("Could not delete ${file.name}")
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!directory.exists()) return@withLock
            directory.listFiles().orEmpty().forEach { file ->
                if (file.isFile && !file.delete()) error("Could not delete ${file.name}")
            }
        }
    }

    private fun readRecord(file: File): AutomationRunRecord? = runCatching {
        gson.fromJson(file.readText(Charsets.UTF_8), AutomationRunRecord::class.java)
    }.getOrNull()

    private fun ensureDirectory() {
        if (!directory.exists()) check(directory.mkdirs()) { "Could not create run history directory" }
    }

    private fun recordFile(id: String): File = File(directory, "$id.json")

    private fun replaceFile(source: File, destination: File) {
        try {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
