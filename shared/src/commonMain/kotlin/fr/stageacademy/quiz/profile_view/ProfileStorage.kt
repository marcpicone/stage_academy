package fr.stageacademy.quiz.profile_view

internal data class SavedProfile<Photo>(val name: String, val photo: Photo?)

internal data class AccountProfile(val name: String, val avatar: ByteArray?)

internal interface AccountProfileGateway {
    fun loadAccountProfile(id: String): AccountProfile? = null
    fun saveAccountProfile(id: String, profile: AccountProfile, replaceAvatar: Boolean): AccountProfile = error("No profile server")
}

internal interface ProfileRepository<Photo> {
    fun load(): SavedProfile<Photo>?
    fun save(name: String, draftPhoto: Photo?): SavedProfile<Photo>
}

internal interface ProfilePhotos<Source, Preview, Photo> {
    fun prepareCapture(): Photo
    fun isValidCapture(pendingPhoto: Photo): Boolean
    fun previousDraft(): Photo?
    fun discardDraft(photo: Photo?)
    fun loadPreview(photo: Photo): Preview
    fun importPhoto(source: Source): Preview
    fun saveCrop(image: Preview, crop: PhotoCrop): Photo
}
