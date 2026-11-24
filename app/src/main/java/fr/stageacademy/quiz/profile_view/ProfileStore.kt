package fr.stageacademy.quiz.profile_view

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

internal data class SavedProfile(val name: String, val photo: File?)

internal class ProfileStore(context: Context) {
    private val directory = File(context.noBackupFilesDir, "player_profile")
    private val record = File(directory, "profile.json")

    fun load(): SavedProfile? = synchronized(lock) { readProfile() }

    fun save(name: String, draftPhoto: File?): SavedProfile = synchronized(lock) {
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank())
        if (draftPhoto != null && (!draftPhoto.isFile || draftPhoto.length() == 0L)) {
            throw IOException("Profile photo is missing or empty")
        }
        val previous = readProfile()
        Files.createDirectories(directory.toPath())
        val newPhoto = if (draftPhoto != null && draftPhoto != previous?.photo) copyPhoto(draftPhoto) else null
        val updated = SavedProfile(trimmedName, newPhoto ?: draftPhoto)

        try {
            writeProfile(updated)
        } catch (error: IOException) {
            newPhoto?.delete()
            throw error
        }

        // The old photo is still needed until the new record has replaced the old one.
        previous?.photo?.takeIf { it != updated.photo }?.delete()
        updated
    }

    private fun readProfile(): SavedProfile? {
        if (!record.exists()) return null
        try {
            val data = JSONObject(record.readText())
            val name = data.getString("name").trim()
            if (name.isBlank()) throw IOException("Profile name is missing")
            val photoName = if (data.has("photo")) data.getString("photo") else null
            if (photoName != null && !photoNamePattern.matches(photoName)) {
                throw IOException("Invalid profile photo path")
            }
            return SavedProfile(name, photoName?.let { File(directory, it) })
        } catch (error: JSONException) {
            throw IOException("Invalid profile record", error)
        }
    }

    private fun copyPhoto(source: File): File {
        val destination = File.createTempFile("photo-", ".jpg", directory)
        try {
            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            return destination
        } catch (error: IOException) {
            destination.delete()
            throw error
        }
    }

    private fun writeProfile(profile: SavedProfile) {
        val data = JSONObject().put("name", profile.name).put("photo", profile.photo?.name)
        val pending = File.createTempFile("profile-", ".tmp", directory)
        try {
            pending.outputStream().use { output ->
                output.write(data.toString().toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            Files.move(pending.toPath(), record.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            pending.delete()
        }
    }

    private companion object {
        val lock = Any()
        val photoNamePattern = Regex("photo-[0-9]+\\.jpg")
    }
}
