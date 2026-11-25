package fr.stageacademy.quiz.leaderboard_view

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class AndroidSessionVault(context: Context) : SessionVault {
    private val file = AtomicFile(File(context.noBackupFilesDir, "devquiz-session"))

    override fun read(): String? = protect {
        val payload = try {
            file.readFully()
        } catch (_: FileNotFoundException) {
            return@protect null
        }
        if (payload.size < 28) throw IOException("Invalid session storage")
        Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, payload.copyOfRange(0, 12)))
            doFinal(payload.copyOfRange(12, payload.size)).toString(Charsets.UTF_8)
        }
    }

    override fun write(value: String) = protect {
        val bytes = Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, key())
            iv + doFinal(value.toByteArray(Charsets.UTF_8))
        }
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (error: IOException) {
            file.failWrite(output)
            throw error
        }
    }

    override fun clear() { file.delete() }

    private fun key(): SecretKey {
        val alias = "devquiz-session-key"
        val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keys.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private inline fun <T> protect(action: () -> T): T = try {
        action()
    } catch (error: GeneralSecurityException) {
        throw IOException("Session storage unavailable", error)
    }
}
