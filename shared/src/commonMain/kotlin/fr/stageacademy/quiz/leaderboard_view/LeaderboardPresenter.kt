package fr.stageacademy.quiz.leaderboard_view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import fr.stageacademy.quiz.StorageException
import fr.stageacademy.quiz.platformIo
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class LeaderboardPresenter(internal val store: LeaderboardStore, internal val scope: CoroutineScope) {
    private var refreshAfterLoad = false
    val shared: Boolean get() = store.shared
    val accountAvailable: Boolean get() = store.accountAvailable
    var accountBusy by mutableStateOf(false)
        private set
    var accountResult by mutableStateOf<AccountResult?>(null)
        private set
    var accountName by mutableStateOf(store.accountName)
        private set
    var accountId by mutableStateOf(store.collectionAccountId)
        private set
    var source by mutableStateOf(LeaderboardSource.Local)
        private set
    var rankings by mutableStateOf(emptyList<PlayerRanking>())
        private set
    var deviceRankings by mutableStateOf(emptyList<PlayerRanking>())
        private set
    var loading by mutableStateOf(false)
        private set
    var recording by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set

    fun authenticate(
        username: String, password: String, email: String?, onSuccess: (AccountResult) -> Unit,
    ) {
        if (!accountAvailable || accountBusy) return
        accountBusy = true
        accountResult = null
        scope.launch {
            try {
                val result = withContext(platformIo) { store.authenticate(username, password, email) }
                accountResult = result
                if (result == AccountResult.Connected) {
                    accountName = store.accountName
                    accountId = store.collectionAccountId
                    refresh()
                }
                if (result == AccountResult.Connected || result == AccountResult.Created) onSuccess(result)
            } catch (_: StorageException) {
                accountResult = AccountResult.Unavailable
            } finally {
                accountBusy = false
            }
        }
    }

    fun clearAccountResult() {
        if (!accountBusy) accountResult = null
    }

    suspend fun restore() {
        if (accountBusy || accountId != null || !accountAvailable) return
        accountBusy = true
        try {
            withContext(platformIo) { store.restoreAccount() }
            accountName = store.accountName
            accountId = store.collectionAccountId
            if (accountId != null) refresh()
        } catch (_: StorageException) {
            accountResult = AccountResult.Unavailable
        } finally {
            accountBusy = false
        }
    }

    fun signOut() {
        if (accountBusy) return
        accountBusy = true
        scope.launch {
            try {
                withContext(platformIo) { store.signOut() }
                accountResult = null
            } catch (_: StorageException) {
                accountResult = AccountResult.Unavailable
            } finally {
                accountName = store.accountName
                accountId = store.collectionAccountId
                accountBusy = false
                if (accountId == null) {
                    rankings = deviceRankings
                    source = LeaderboardSource.Local
                }
                refresh()
            }
        }
    }

    private fun refresh() {
        if (loading || recording) refreshAfterLoad = true else load()
    }

    fun load() {
        if (loading || recording) return
        loading = true
        failed = false
        scope.launch {
            try {
                val (standings, device) = withContext(platformIo) {
                    val standings = store.standings()
                    val device = if (standings.source == LeaderboardSource.Shared) store.localRankings() else standings.players
                    standings to device
                }
                deviceRankings = device
                val signedOut = shared && store.accountName == null
                rankings = if (signedOut) device else standings.players
                source = if (signedOut) LeaderboardSource.Local else standings.source
            } catch (_: StorageException) {
                failed = true
            } finally {
                accountName = store.accountName
                accountId = store.collectionAccountId
                loading = false
                if (refreshAfterLoad) {
                    refreshAfterLoad = false
                    load()
                }
            }
        }
    }

    fun completeGame(id: String, name: String, points: Int, onRecorded: () -> Unit, onFailure: () -> Unit) {
        if (recording) return
        recording = true
        scope.launch {
            var recorded = false
            try {
                val localRankings = withContext(platformIo) { store.record(id, name, points) }
                deviceRankings = localRankings
                if (source != LeaderboardSource.Shared) rankings = localRankings
                recorded = true
                onRecorded()
            } catch (_: StorageException) {
                onFailure()
            } finally {
                recording = false
            }
            if (recorded && store.shared) {
                refresh()
            }
        }
    }
}
