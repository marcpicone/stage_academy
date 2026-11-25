package fr.stageacademy.quiz.profile_view

import androidx.compose.runtime.Composable

internal interface ProfileIdentity {
    val savedName: String
    fun load()
}

internal interface ProfilePanel : ProfileIdentity {
    @Composable
    fun Content(isProfileSelected: () -> Boolean, accountContent: @Composable () -> Unit)
}
