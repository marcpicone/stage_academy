package fr.stageacademy.quiz.collection_view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.stageacademy.quiz.QuizPreferences
import fr.stageacademy.quiz.StorageException
import fr.stageacademy.quiz.platformIo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal interface CollectionGateway {
    val collectionAccountId: String? get() = null
    fun fetchCollection(accountId: String): CollectionProgress = error("No collection server")
    fun pushCollection(accountId: String, update: CollectionUpdate): CollectionProgress = error("No collection server")
    fun purchaseCrate(accountId: String, purchase: PendingCrate): CrateReward? = error("No collection server")
}

internal data class CollectionUpdate(val id: String, val delta: CollectionProgress, val initialPoints: Int?)
internal data class PendingCrate(val id: String, val crate: SecretCrate)

private data class CollectionJournal(
    val server: CollectionProgress = CollectionProgress(),
    val pending: CollectionProgress = CollectionProgress(),
    val initialPoints: Int? = null,
    val sending: CollectionUpdate? = null,
    val purchase: PendingCrate? = null,
) {
    fun display(): CollectionProgress {
        val outstanding = sending?.delta ?: CollectionProgress()
        val startingPoints = sending?.initialPoints ?: initialPoints ?: server.points
        val points = startingPoints.toLong() + outstanding.points + pending.points
        return CollectionProgress(points.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), server.floats + outstanding.floats + pending.floats)
    }

    fun encode() = listOf(
        encodeCollection(server),
        encodeCollection(pending),
        initialPoints?.toString().orEmpty(),
        sending?.id.orEmpty(),
        sending?.delta?.let(::encodeCollection).orEmpty(),
        sending?.initialPoints?.toString().orEmpty(),
        purchase?.id.orEmpty(),
        purchase?.crate?.name.orEmpty(),
    ).joinToString("\n")
}

private fun decodeJournal(value: String?): CollectionJournal {
    if (value == null) return CollectionJournal()
    val parts = value.split('\n')
    fun progress(index: Int) = loadCollection(parts.getOrNull(index), "", emptySet()) {}
    val sending = parts.getOrNull(3)?.takeIf(String::isNotEmpty)?.let {
        CollectionUpdate(it, progress(4), parts.getOrNull(5)?.toIntOrNull())
    }
    val purchase = SecretCrate.entries.find { it.name == parts.getOrNull(7) }?.let { crate ->
        parts.getOrNull(6)?.takeIf(String::isNotEmpty)?.let { PendingCrate(it, crate) }
    }
    return CollectionJournal(progress(0), progress(1), parts.getOrNull(2)?.toIntOrNull(), sending, purchase)
}

@OptIn(ExperimentalUuidApi::class)
private fun operationId() = Uuid.random().toString()

internal class CollectionPresenter(
    private val preferences: QuizPreferences,
    private val gateway: CollectionGateway,
    private val scope: CoroutineScope,
) {
    private var journal = CollectionJournal()
    private var account: String? = null
    private var sessionVersion = 0
    private var refreshRequested = false
    var progress by mutableStateOf(preferences.collection())
        private set
    var syncing by mutableStateOf(false)
        private set
    var unavailable by mutableStateOf(false)
        private set

    fun useAccount(id: String?) {
        if (account == id) return
        sessionVersion++
        account = id
        syncing = false
        unavailable = false
        refreshRequested = false
        val guest = preferences.collection()
        if (id == null) {
            progress = guest
            return
        }

        val stored = preferences.loadAccountCollection(id)
        journal = decodeJournal(stored)
        if (preferences.collectionImportOwner() == null) {
            if (stored == null && guest != CollectionProgress()) {
                // L'ancien solde ne sert qu'à créer une collection serveur vide.
                publish(journal.copy(pending = CollectionProgress(floats = guest.floats), initialPoints = guest.points))
            }
            preferences.saveCollectionImportOwner(id)
        }
        progress = journal.display()
        refresh()
    }

    fun earnPoint() {
        if (account == null) saveGuest(progress.earnPoint())
        else enqueue(journal.pending.earnPoint())
    }

    fun discover(floats: Map<QuizSecret, SecretFloat>) {
        if (floats.isEmpty()) return
        if (account == null) saveGuest(progress.copy(floats = progress.floats + floats))
        else enqueue(journal.pending.copy(floats = journal.pending.floats + floats))
    }

    fun refresh() {
        if (syncing) {
            refreshRequested = true
            return
        }
        runSync()
    }

    fun buy(crate: SecretCrate, onReward: (QuizSecret) -> Unit) {
        if (account == null) {
            val reward = crate.open(progress) ?: return
            saveGuest(reward.progress)
            onReward(reward.secret)
        } else if (!syncing && journal.purchase == null) {
            publish(journal.copy(purchase = PendingCrate(operationId(), crate)))
            runSync(onReward)
        } else if (!syncing) {
            runSync(onReward)
        }
    }

    private fun saveGuest(value: CollectionProgress) {
        if (progress == value) return
        preferences.saveCollection(value)
        progress = value
    }

    private fun enqueue(delta: CollectionProgress) {
        publish(journal.copy(pending = delta))
        if (syncing) refreshRequested = true else runSync(debounce = true)
    }

    private fun publish(value: CollectionJournal) {
        val id = account ?: return
        preferences.saveAccountCollection(id, value.encode())
        journal = value
        progress = value.display()
    }

    private fun runSync(onReward: ((QuizSecret) -> Unit)? = null, debounce: Boolean = false) {
        val id = account ?: return
        val version = sessionVersion
        syncing = true
        scope.launch {
            try {
                if (debounce) delay(450)
                if (version != sessionVersion) return@launch
                uploadChanges(id, version)
                if (version != sessionVersion) return@launch
                val purchase = journal.purchase
                if (purchase != null) {
                    val reward = withContext(platformIo) { gateway.purchaseCrate(id, purchase) }
                    if (version != sessionVersion) return@launch
                    publish(journal.copy(purchase = null, server = reward?.progress ?: journal.server))
                    if (reward != null) onReward?.invoke(reward.secret)
                    else refreshRequested = true
                }
                unavailable = false
            } catch (_: StorageException) {
                if (version == sessionVersion) unavailable = true
            } finally {
                if (version == sessionVersion) {
                    syncing = false
                    val retry = refreshRequested && !unavailable
                    refreshRequested = false
                    if (retry) runSync()
                }
            }
        }
    }

    private suspend fun uploadChanges(id: String, version: Int) {
        if (journal.sending == null && (journal.pending != CollectionProgress() || journal.initialPoints != null)) {
            val update = CollectionUpdate(operationId(), journal.pending, journal.initialPoints)
            publish(journal.copy(sending = update, pending = CollectionProgress(), initialPoints = null))
        }
        val sending = journal.sending
        val server = withContext(platformIo) {
            if (sending == null) gateway.fetchCollection(id) else gateway.pushCollection(id, sending)
        }
        if (version == sessionVersion) {
            publish(journal.copy(server = server, sending = null))
        }
    }
}
