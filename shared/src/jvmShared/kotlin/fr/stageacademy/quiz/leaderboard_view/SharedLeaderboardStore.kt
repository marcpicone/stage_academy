package fr.stageacademy.quiz.leaderboard_view

import fr.stageacademy.quiz.collection_view.CollectionProgress
import fr.stageacademy.quiz.collection_view.CollectionUpdate
import fr.stageacademy.quiz.collection_view.CrateReward
import fr.stageacademy.quiz.collection_view.PendingCrate
import fr.stageacademy.quiz.collection_view.QuizSecret
import fr.stageacademy.quiz.collection_view.SecretFloat
import fr.stageacademy.quiz.collection_view.SecretFloatScale
import fr.stageacademy.quiz.profile_view.AccountProfile
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64
import java.util.UUID

internal fun decodeRounds(games: JSONObject): Sequence<RankedRound> =
    games.keys().asSequence().mapNotNull { id ->
        val round = games.optJSONObject(id) ?: return@mapNotNull null
        RankedRound(id, round.optString("player"), round.optInt("points"), round.optString("accountId").ifBlank { null })
    }

internal fun leaderboardStore(local: LocalLeaderboardStore, vault: SessionVault, fallbackUrl: String = ""): LeaderboardStore {
    val url = LeaderboardUrl.ifBlank { fallbackUrl }
    return if (url.isBlank()) local else SharedLeaderboardStore(local, url, vault)
}

internal class SharedLeaderboardStore(
    private val local: LocalLeaderboardStore,
    private val baseUrl: String,
    private val vault: SessionVault,
) : LeaderboardStore {
    private data class Account(
        val id: String, val name: String, val refresh: String?, val token: String = "", val expiresAt: Long = 0,
    )
    private class HttpError(val status: Int) : IOException("Server HTTP $status")
    @Volatile private var session: Account? = null
    private val sessionLock = Any()
    private val account get() = session?.takeIf { it.refresh != null || System.nanoTime() < it.expiresAt }
    private val onRender = URI(baseUrl).host.endsWith(".onrender.com", ignoreCase = true)
    override val shared = true
    override val accountAvailable = URI(baseUrl).let { it.scheme == "https" || it.host in setOf("127.0.0.1", "localhost") }
    override val accountName get() = account?.name
    override val collectionAccountId get() = account?.id

    override fun restoreAccount(): Unit = synchronized(sessionLock) {
        if (session != null || !accountAvailable) return@synchronized
        parse {
            val saved = vault.read()?.let(::JSONObject) ?: return@parse
            if (saved.getString("server") == baseUrl) {
                val id = UUID.fromString(saved.getString("id")).toString()
                val refresh = saved.getString("refresh")
                if (refresh.length == 43) session = Account(id, saved.getString("name"), refresh)
            }
        }
    }

    override fun signOut() {
        val refresh = synchronized(sessionLock) {
            val remembered = session?.refresh
            clearSession()
            remembered
        }
        if (refresh != null) {
            try {
                request("/auth/logout", JSONObject().put("refresh_token", refresh), timeout = 5_000)
            } catch (_: IOException) {
                // Le jeton a déjà été effacé de cet appareil.
            }
        }
    }

    private fun clearSession() {
        session = null
        vault.clear()
    }

    private fun accountFrom(result: JSONObject, refresh: String?): Account = Account(
        UUID.fromString(result.getString("user_id")).toString(), result.getString("username"), refresh,
        result.getString("access_token"), System.nanoTime() + result.getLong("expires_in") * 1_000_000_000L,
    )

    override fun authenticate(username: String, password: String, email: String?): AccountResult {
        if (!accountAvailable) return AccountResult.Unavailable
        val body = JSONObject().put("username", username).put("password", password)
        return try {
            if (email != null) {
                request("/auth/register", body.put("email", email))
                AccountResult.Created
            } else {
                parse {
                    val result = JSONObject(request("/auth/login", body))
                    val connected = accountFrom(result, result.optString("refresh_token").ifBlank { null })
                    synchronized(sessionLock) {
                        connected.refresh?.let { refresh ->
                            vault.write(JSONObject().put("server", baseUrl).put("id", connected.id)
                                .put("name", connected.name).put("refresh", refresh).toString())
                        }
                        session = connected
                    }
                }
                AccountResult.Connected
            }
        } catch (error: HttpError) {
            when (error.status) {
                401 -> AccountResult.InvalidCredentials
                409 -> AccountResult.Conflict
                422 -> AccountResult.InvalidFields
                429 -> AccountResult.RateLimited
                else -> AccountResult.Unavailable
            }
        }
    }

    override fun rankings() = standings().players
    override fun localRankings() = local.rankings()

    override fun fetchCollection(accountId: String): CollectionProgress = parse {
        readCollection(JSONObject(request("/collection", token = collectionToken(accountId))))
    }

    override fun pushCollection(accountId: String, update: CollectionUpdate): CollectionProgress = parse {
        val floats = JSONObject()
        update.delta.floats.forEach { (secret, value) -> floats.put(secret.name, value.units) }
        val body = JSONObject().put("id", update.id).put("earned", update.delta.points).put("floats", floats)
        update.initialPoints?.let { body.put("initial_points", it) }
        readCollection(JSONObject(request("/collection/sync", body, collectionToken(accountId))))
    }

    override fun purchaseCrate(accountId: String, purchase: PendingCrate): CrateReward? = parse {
        val body = JSONObject().put("id", purchase.id).put("crate", purchase.crate.name)
        try {
            val result = JSONObject(request("/collection/crates", body, collectionToken(accountId)))
            val secret = QuizSecret.entries.firstOrNull { it.name == result.optString("reward") }
                ?: throw IOException("Unknown crate reward")
            CrateReward(secret, readCollection(result))
        } catch (error: HttpError) {
            if (error.status != 409) throw error
            null
        }
    }

    private fun collectionToken(accountId: String): String = synchronized(sessionLock) {
        val current = account?.takeIf { it.id == accountId } ?: throw IOException("Account session unavailable")
        if (current.token.isNotEmpty() && System.nanoTime() < current.expiresAt) return@synchronized current.token
        val refresh = current.refresh ?: throw IOException("Account session expired")
        try {
            val access = parse {
                val response = JSONObject(request("/auth/refresh", JSONObject().put("refresh_token", refresh)))
                accountFrom(response, refresh)
            }
            if (access.id != accountId) throw IOException("Unexpected account")
            session = access
            access.token
        } catch (error: HttpError) {
            if (error.status == 401) clearSession()
            throw error
        }
    }

    override fun loadAccountProfile(id: String): AccountProfile? {
        val response = request("/users/me/player-profile", token = collectionToken(id))
        return if (response.trim() == "null") null else parse { readProfile(JSONObject(response)) }
    }

    override fun saveAccountProfile(id: String, profile: AccountProfile, replaceAvatar: Boolean): AccountProfile = parse {
        val body = JSONObject().put("display_name", profile.name)
        if (replaceAvatar) {
            val avatar = profile.avatar?.let { Base64.getEncoder().encodeToString(it) }
            body.put("avatar", avatar ?: JSONObject.NULL)
        }
        readProfile(JSONObject(request("/users/me/player-profile", body, collectionToken(id), method = "PUT")))
    }

    private fun readProfile(data: JSONObject): AccountProfile {
        val avatar = if (data.isNull("avatar")) null else Base64.getDecoder().decode(data.getString("avatar"))
        return AccountProfile(data.getString("display_name"), avatar)
    }

    private fun readCollection(result: JSONObject): CollectionProgress {
        val floats = result.getJSONObject("floats")
        val known = buildMap {
            for (secret in QuizSecret.entries) {
                val units = floats.optLong(secret.name, -1)
                if (units in 0 until SecretFloatScale) put(secret, SecretFloat(units))
            }
        }
        return CollectionProgress(result.getInt("points"), known)
    }

    override fun record(id: String, player: String, points: Int): List<PlayerRanking> {
        val current = account
        return local.recordOwned(id, current?.name ?: player, points, current?.id)
    }

    override fun standings(): LeaderboardStandings {
        val current = account ?: return LeaderboardStandings(local.rankings(), LeaderboardSource.Local)
        return try {
            val token = collectionToken(current.id)
            val rounds = local.rounds().filter { it.accountId == current.id }
            var players = emptyList<PlayerRanking>()
            if (rounds.isEmpty()) {
                players = readPlayers(request("/leaderboard", token = token))
            } else {
                for (batch in rounds.chunked(100)) {
                    val body = JSONArray()
                    batch.forEach { round -> body.put(JSONObject().put("id", round.id).put("points", round.points)) }
                    players = readPlayers(request("/leaderboard/rounds", JSONObject().put("rounds", body), token))
                }
            }
            LeaderboardStandings(players, LeaderboardSource.Shared)
        } catch (_: IOException) {
            val source = if (account == null) LeaderboardSource.Local else LeaderboardSource.Offline
            LeaderboardStandings(local.rankings(), source)
        }
    }

    private fun readPlayers(response: String): List<PlayerRanking> = parse {
        val players = JSONArray(response)
        List(players.length()) { index ->
            val player = players.getJSONObject(index)
            PlayerRanking(player.getString("name"), player.getLong("points"))
        }
    }

    private inline fun <T> parse(read: () -> T): T = try {
        read()
    } catch (error: JSONException) {
        throw IOException("Invalid server response", error)
    } catch (error: IllegalArgumentException) {
        throw IOException("Invalid account data", error)
    }

    private fun request(
        path: String, body: JSONObject? = null, token: String? = null,
        method: String = if (body == null) "GET" else "POST", timeout: Int = if (onRender) 90_000 else 5_000,
    ): String {
        val connection = (URI("$baseUrl$path").toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 4_000
            readTimeout = timeout
            instanceFollowRedirects = false
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        try {
            if (body != null) {
                val bytes = body.toString().toByteArray(Charsets.UTF_8)
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            val status = connection.responseCode
            if (status == 401 && token != null) synchronized(sessionLock) {
                val previous = session?.takeIf { it.token == token }
                if (previous?.refresh != null) session = previous.copy(token = "", expiresAt = 0)
                else if (previous != null) clearSession()
            }
            if (status !in 200..299) throw HttpError(status)
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
