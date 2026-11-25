package fr.stageacademy.quiz.profile_view

import org.jetbrains.compose.resources.StringResource


internal interface ProfileContract<Source, Preview, Photo> : ProfileIdentity {
    val ready: Boolean
    val saving: Boolean
    val importing: Boolean
    val cropImage: Preview?
    val name: String
    val photo: Photo?
    val nameInvalid: Boolean
    val hasChanges: Boolean
    val message: StringResource?

    fun editName(value: String)
    fun removePhoto()
    fun importPhoto(uri: Source)
    fun cancelCrop()
    fun confirmCrop(crop: PhotoCrop)
    fun prepareCapture(): Photo
    fun acceptCapture(file: Photo): Boolean
    suspend fun loadPreview(file: Photo): Preview
    fun save()
}
