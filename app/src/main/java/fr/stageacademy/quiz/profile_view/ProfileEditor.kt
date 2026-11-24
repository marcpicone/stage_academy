package fr.stageacademy.quiz.profile_view

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.stageacademy.quiz.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

internal class ProfileEditor(context: Context, private val scope: CoroutineScope) {
    val photos = PhotoFiles(context)
    private val store = ProfileStore(context)
    private var saved by mutableStateOf<SavedProfile?>(null)
    private var loading = false

    var ready by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var name by mutableStateOf("")
        private set
    var photo by mutableStateOf<File?>(null)
        private set
    var nameInvalid by mutableStateOf(false)
        private set
    var message by mutableStateOf<Int?>(null)
        private set

    val hasChanges: Boolean
        get() = name.trim() != saved?.name.orEmpty() || photo != saved?.photo

    fun load() {
        if (ready || loading) return
        loading = true
        scope.launch {
            try {
                val (profile, draftPhoto) = withContext(Dispatchers.IO) {
                    val profile = store.load()
                    profile to (if (profile == null) photos.previousDraft() else profile.photo)
                }
                saved = profile
                name = profile?.name.orEmpty()
                photo = draftPhoto
            } catch (_: IOException) {
                message = R.string.profile_load_failed
            } finally {
                ready = true
                loading = false
            }
        }
    }

    fun editName(value: String) {
        if (saving) return
        name = value
        nameInvalid = false
        message = null
    }

    fun selectPhoto(value: File?) {
        if (saving || photo == value) return
        photos.discardDraft(photo)
        photo = value
        message = null
    }

    fun save() {
        if (!ready || saving) return
        val trimmedName = name.trim()
        message = null
        if (trimmedName.isBlank()) {
            nameInvalid = true
            return
        }

        val draftPhoto = photo
        saving = true
        scope.launch {
            try {
                val profile = withContext(Dispatchers.IO) { store.save(trimmedName, draftPhoto) }
                saved = profile
                name = profile.name
                photo = profile.photo
                photos.discardDraft(draftPhoto)
                message = R.string.profile_saved
            } catch (_: IOException) {
                message = R.string.profile_save_failed
            } finally {
                saving = false
            }
        }
    }
}
