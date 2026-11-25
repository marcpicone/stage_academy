package fr.stageacademy.quiz.leaderboard_view

import fr.stageacademy.quiz.collection_view.CollectionGateway
import fr.stageacademy.quiz.profile_view.AccountProfileGateway

internal data class PlayerRanking(val name: String, val points: Long)
internal data class RankedRound(val id: String, val player: String, val points: Int, val accountId: String? = null)
internal enum class AccountResult {
    Connected, Created, InvalidCredentials, InvalidFields, Conflict, RateLimited, Unavailable,
}

internal enum class LeaderboardSource { Local, Shared, Offline }

internal data class LeaderboardStandings(val players: List<PlayerRanking>, val source: LeaderboardSource)

internal interface LeaderboardStore : CollectionGateway, AccountProfileGateway {
    val shared: Boolean get() = false
    val accountAvailable: Boolean get() = false
    val accountName: String? get() = null
    fun restoreAccount() = Unit
    fun authenticate(username: String, password: String, email: String?): AccountResult =
        AccountResult.Unavailable
    fun signOut() = Unit
    fun rankings(): List<PlayerRanking>
    fun localRankings(): List<PlayerRanking> = rankings()
    fun record(id: String, player: String, points: Int): List<PlayerRanking>
    fun standings() = LeaderboardStandings(rankings(), LeaderboardSource.Local)
}

internal interface LocalLeaderboardStore : LeaderboardStore {
    fun rounds(): List<RankedRound>
    fun recordOwned(id: String, player: String, points: Int, accountId: String?): List<PlayerRanking>
    override fun record(id: String, player: String, points: Int) = recordOwned(id, player, points, null)
}

internal fun rankPlayers(scores: Sequence<Pair<String, Int>>): List<PlayerRanking> {
    val players = mutableMapOf<String, PlayerRanking>()
    for ((player, points) in scores) {
        val name = player.trim()
        if (name.isEmpty()) continue
        val key = name.lowercase()
        val previous = players[key] ?: PlayerRanking(name, 0)
        players[key] = previous.copy(points = previous.points + points.coerceAtLeast(0))
    }
    return players.values.sortedWith(compareByDescending<PlayerRanking> { it.points }.thenBy { it.name.lowercase() })
}
