package dev.tapscript.app.profile

import android.content.Context
import android.net.Uri
import dev.tapscript.storage.json.JsonProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileTransferService(
    context: Context,
    private val repository: JsonProfileRepository,
) {
    private val resolver = context.applicationContext.contentResolver

    suspend fun importFrom(uri: Uri): Int = withContext(Dispatchers.IO) {
        val json = resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("Could not open profile backup")
        repository.importBackup(json)
    }

    suspend fun exportTo(uri: Uri): Int = withContext(Dispatchers.IO) {
        val profiles = repository.list()
        val json = repository.exportBackup()
        resolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
            writer.write(json)
        } ?: error("Could not create profile backup")
        profiles.size
    }
}
