package fr.stageacademy.quiz.leaderboard_view

import com.sun.jna.Platform
import com.sun.jna.platform.win32.Crypt32Util
import com.sun.jna.platform.win32.Win32Exception
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.attribute.PosixFilePermissions

internal class DesktopSessionVault(root: File) : SessionVault {
    private val directory = File(root, "session").toPath()
    private val file = directory.resolve("devquiz-session")
    private val windows = Platform.isWindows()
    private val filePermissions = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))

    override fun read(): String? = withProtection {
        val bytes = try {
            Files.readAllBytes(file)
        } catch (_: NoSuchFileException) {
            return@withProtection null
        }
        (if (windows) Crypt32Util.cryptUnprotectData(bytes) else bytes).toString(Charsets.UTF_8)
    }

    override fun write(value: String) = withProtection {
        Files.createDirectories(directory)
        val bytes = value.toByteArray(Charsets.UTF_8)
        val encrypted = if (windows) Crypt32Util.cryptProtectData(bytes) else bytes
        val pending = if (windows) Files.createTempFile(directory, "session-", ".tmp")
            else Files.createTempFile(directory, "session-", ".tmp", filePermissions)
        try {
            Files.write(pending, encrypted)
            Files.move(pending, file, ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(pending)
        }
        Unit
    }

    override fun clear() { Files.deleteIfExists(file) }

    private inline fun <T> withProtection(action: () -> T): T = try {
        action()
    } catch (error: Win32Exception) {
        throw IOException("Session storage unavailable", error)
    }
}
