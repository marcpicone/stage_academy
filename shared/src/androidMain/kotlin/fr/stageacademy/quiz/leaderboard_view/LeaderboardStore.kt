package fr.stageacademy.quiz.leaderboard_view

import android.content.Context
import android.util.AtomicFile
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
internal class AndroidLeaderboardStore(context: Context) : LocalLeaderboardStore {
    private val file = AtomicFile(File(context.noBackupFilesDir, "quiz_leaderboard.json"))

    override fun rankings(): List<PlayerRanking> = synchronized(lock) { rankings(readGames()) }
    override fun rounds(): List<RankedRound> = synchronized(lock) { decodeRounds(readGames()).toList() }

    override fun recordOwned(id: String, player: String, points: Int, accountId: String?): List<PlayerRanking> = synchronized(lock) {
        val games = readGames()
        if (!games.has(id)) {
            games.put(id, JSONObject().put("player", player).put("points", points).put("accountId", accountId))
            val output = file.startWrite()
            try {
                output.write(games.toString().toByteArray(Charsets.UTF_8))
                output.fd.sync()
            } catch (error: IOException) {
                file.failWrite(output)
                throw error
            }
            file.finishWrite(output)
            if (!readGames().has(id)) throw IOException("The game was not saved")
        }
        rankings(games)
    }

    private fun readGames(): JSONObject {
        return try {
            JSONObject(file.readFully().toString(Charsets.UTF_8))
        } catch (error: FileNotFoundException) {
            if (file.baseFile.exists()) throw error
            JSONObject()
        } catch (error: JSONException) {
            throw IOException("Invalid leaderboard", error)
        }
    }

    private fun rankings(games: JSONObject) = rankPlayers(decodeRounds(games).map { it.player to it.points })

    private companion object {
        val lock = Any()
    }
}
