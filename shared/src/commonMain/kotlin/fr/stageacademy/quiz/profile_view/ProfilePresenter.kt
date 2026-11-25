package fr.stageacademy.quiz.profile_view

import org.jetbrains.compose.resources.StringResource

import fr.stageacademy.quiz.StorageException
import fr.stageacademy.quiz.platformIo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ProfilePresenter<Source, Preview, Photo>(
    private val photos: ProfilePhotos<Source, Preview, Photo>,
    private val store: ProfileRepository<Photo>,
    private val scope: CoroutineScope,
) : ProfileContract<Source, Preview, Photo> {
    private var saved by mutableStateOf<SavedProfile<Photo>?>(null)
    override val savedName: String get() = saved?.name.orEmpty()
    private var loading = false
    private var saveRevision = 0

    override var ready by mutableStateOf(false)
        private set
    override var saving by mutableStateOf(false)
        private set
    override var importing by mutableStateOf(false)
        private set
    override var cropImage by mutableStateOf<Preview?>(null)
        private set
    override var name by mutableStateOf("")
        private set
    override var photo by mutableStateOf<Photo?>(null)
        private set
    override var nameInvalid by mutableStateOf(false)
        private set
    override var message by mutableStateOf<StringResource?>(null)
        private set

    override val hasChanges: Boolean
        get() = name.trim() != saved?.name.orEmpty() || photo != saved?.photo

    override fun load() {
        if (loading || saving || importing || (ready && hasChanges)) return
        loading = true
        val revision = saveRevision
        scope.launch {
            try {
                val (profile, draftPhoto) = withContext(platformIo) {
                    val profile = store.load()
                    profile to (if (profile == null) photos.previousDraft() else profile.photo)
                }
                if (revision == saveRevision && (!ready || !hasChanges)) {
                    saved = profile
                    name = profile?.name.orEmpty()
                    photo = draftPhoto
                }
            } catch (_: StorageException) {
                message = Res.string.profile_load_failed
            } finally {
                ready = true
                loading = false
            }
        }
    }

    override fun editName(value: String) {
        if (saving) return
        name = value
        nameInvalid = false
        message = null
    }

    private fun selectPhoto(value: Photo?) {
        if (photo == value) return
        photos.discardDraft(photo)
        photo = value
        message = null
    }

    override fun prepareCapture(): Photo = photos.prepareCapture()

    override fun acceptCapture(file: Photo): Boolean {
        if (saving || importing || !photos.isValidCapture(file)) return false
        selectPhoto(file)
        return true
    }

    override suspend fun loadPreview(file: Photo) = withContext(platformIo) {
        photos.loadPreview(file)
    }

    override fun removePhoto() {
        if (!saving && !importing) selectPhoto(null)
    }

    override fun importPhoto(uri: Source) {
        if (!ready || saving || importing) return
        importing = true
        message = null
        scope.launch {
            try {
                cropImage = withContext(platformIo) { photos.importPhoto(uri) }
            } catch (_: StorageException) {
                message = Res.string.photo_import_failed
            } catch (_: SecurityException) {
                message = Res.string.photo_import_failed
            } finally {
                importing = false
            }
        }
    }

    override fun cancelCrop() {
        if (importing) return
        cropImage = null
        message = null
    }

    override fun confirmCrop(crop: PhotoCrop) {
        val image = cropImage ?: return
        if (importing) return
        importing = true
        message = null
        scope.launch {
            try {
                val draft = withContext(platformIo) { photos.saveCrop(image, crop) }
                selectPhoto(draft)
                cropImage = null
            } catch (_: StorageException) {
                message = Res.string.photo_import_failed
            } finally {
                importing = false
            }
        }
    }

    override fun save() {
        if (!ready || saving || importing || cropImage != null) return
        val trimmedName = name.trim()
        message = null
        if (trimmedName.isBlank() || trimmedName.length > 64) {
            nameInvalid = true
            return
        }

        val draftPhoto = photo
        saveRevision++
        saving = true
        scope.launch {
            try {
                val profile = withContext(platformIo) { store.save(trimmedName, draftPhoto) }
                saved = profile
                name = profile.name
                photo = profile.photo
                photos.discardDraft(draftPhoto)
                message = Res.string.profile_saved
            } catch (_: StorageException) {
                message = Res.string.profile_save_failed
            } finally {
                saving = false
            }
        }
    }
}
