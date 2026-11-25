package fr.stageacademy.quiz.profile_view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.shared_view.MenuCard
import fr.stageacademy.quiz.leaderboard_view.AccountResult
import fr.stageacademy.quiz.leaderboard_view.LeaderboardPresenter

@Composable
internal fun ProfileAccountCard(presenter: LeaderboardPresenter) {
    var creatingAccount by remember { mutableStateOf<Boolean?>(null) }
    MenuCard(title = stringResource(Res.string.profile_account)) {
        if (presenter.shared && presenter.accountAvailable) {
            val accountName = presenter.accountName
            if (accountName != null) {
                Text(stringResource(Res.string.leaderboard_account_connected, accountName))
                Text(stringResource(Res.string.profile_synchronized))
                TextButton(onClick = presenter::signOut, enabled = !presenter.accountBusy) { Text(stringResource(Res.string.leaderboard_account_logout)) }
            } else {
                if (presenter.accountResult == AccountResult.Created && creatingAccount == null) {
                    Text(stringResource(Res.string.leaderboard_account_created))
                }
                if (presenter.accountResult == AccountResult.Unavailable && creatingAccount == null) {
                    Text(stringResource(Res.string.leaderboard_account_unavailable))
                }
                Button(enabled = !presenter.accountBusy, onClick = {
                    presenter.clearAccountResult()
                    creatingAccount = false
                }) {
                    Text(stringResource(Res.string.leaderboard_account_login))
                }
                TextButton(enabled = !presenter.accountBusy, onClick = {
                    presenter.clearAccountResult()
                    creatingAccount = true
                }) { Text(stringResource(Res.string.leaderboard_account_create)) }
            }
        } else {
            Button(onClick = {}, enabled = false) { Text(stringResource(Res.string.leaderboard_account_login)) }
            Text(stringResource(if (presenter.shared) Res.string.leaderboard_https_required else Res.string.leaderboard_server_required))
        }
    }
    creatingAccount?.let { create ->
        ProfileAccountDialog(create, presenter) { creatingAccount = null }
    }
}

@Composable
private fun ProfileAccountDialog(create: Boolean, presenter: LeaderboardPresenter, onClose: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val valid = username.trim().matches(Regex("[a-zA-Z0-9_.-]{3,32}")) &&
        password.isNotEmpty() && password.length <= 128 && (!create || (password.length >= 12 && email.isNotBlank()))
    val action = if (create) Res.string.leaderboard_account_create else Res.string.leaderboard_account_login
    val error = when (presenter.accountResult) {
        AccountResult.InvalidCredentials -> Res.string.leaderboard_account_invalid
        AccountResult.InvalidFields -> Res.string.leaderboard_account_fields
        AccountResult.Conflict -> Res.string.leaderboard_account_conflict
        AccountResult.RateLimited -> Res.string.leaderboard_account_rate_limit
        AccountResult.Unavailable -> Res.string.leaderboard_account_unavailable
        else -> null
    }
    AlertDialog(
        onDismissRequest = { if (!presenter.accountBusy) onClose() },
        title = { Text(stringResource(action)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(Res.string.leaderboard_account_username)) },
                    singleLine = true,
                    enabled = !presenter.accountBusy,
                )
                if (create) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(Res.string.leaderboard_account_email)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        enabled = !presenter.accountBusy,
                    )
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(Res.string.leaderboard_account_password)) },
                    supportingText = if (create) {
                        { Text(stringResource(Res.string.leaderboard_account_password_length)) }
                    } else null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !presenter.accountBusy,
                )
                if (error != null) Text(stringResource(error), color = MaterialTheme.colorScheme.error)
                if (presenter.accountBusy) Text(stringResource(Res.string.leaderboard_account_waiting))
            }
        },
        confirmButton = {
            Button(enabled = valid && !presenter.accountBusy, onClick = {
                presenter.authenticate(username.trim(), password, email.trim().takeIf { create }) {
                    password = ""
                    onClose()
                }
            }) { Text(stringResource(action)) }
        },
        dismissButton = {
            TextButton(enabled = !presenter.accountBusy, onClick = onClose) { Text(stringResource(Res.string.close)) }
        },
    )
}
