package fr.stageacademy.quiz.profile_view

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.UUID

internal class AccountProfileStore(
    root: File,
    private val accountId: String,
    private val gateway: AccountProfileGateway,
    private val compress: (File) -> ByteArray,
) : ProfileRepository<File> {
    private val guest = ProfileStore(root)
    private val directory = File(File(root, "account_profiles"), UUID.fromString(accountId).toString())
    private val cache = ProfileStore(directory)

    override fun load(): SavedProfile<File>? {
        val remote = try {
            gateway.loadAccountProfile(accountId)
        } catch (error: IOException) {
            return cache.load() ?: throw error
        }
        // L'ancien profil local peut être publié par un clic explicite sur Enregistrer.
        return remote?.let(::saveCache) ?: guest.load()
    }

    override fun save(name: String, draftPhoto: File?): SavedProfile<File> {
        val found = cache.load()
        val replaceAvatar = draftPhoto != found?.photo
        val avatar = if (replaceAvatar) draftPhoto?.let(compress) else null
        return saveCache(gateway.saveAccountProfile(accountId, AccountProfile(name, avatar), replaceAvatar))
    }

    private fun saveCache(profile: AccountProfile): SavedProfile<File> {
        val previous = cache.load()
        val avatar = profile.avatar
        val existing = previous?.photo?.takeIf(File::isFile)
        if (avatar == null) {
            return if (previous?.name == profile.name && previous.photo == null) previous else cache.save(profile.name, null)
        }
        if (previous != null && existing != null && avatar.contentEquals(existing.readBytes())) {
            return if (previous.name == profile.name) previous else cache.save(profile.name, existing)
        }

        Files.createDirectories(directory.toPath())
        val temporary = File.createTempFile("avatar-", ".jpg", directory)
        try {
            temporary.writeBytes(avatar)
            return cache.save(profile.name, temporary)
        } finally {
            temporary.delete()
        }
    }
}
