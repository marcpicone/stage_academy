package fr.stageacademy.quiz.leaderboard_view

import com.sun.jna.Platform
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class RememberedAccountTest {
    @Test
    fun reloadsFromDiskAndRenewsAccessWithoutThePassword() {
        val root = createTempDirectory("devquiz-session-test").toFile()
        val userId = "c0ed9774-06f2-48fe-a126-392ac39cae65"
        val remembered = "x".repeat(43)
        val live = """{"user_id":"$userId","username":"camille","access_token":"active","expires_in":1800}"""
        val service = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        service.createContext("/auth/login") { request ->
            val response = (live.dropLast(1) + ""","refresh_token":"$remembered"}""").toByteArray()
            request.sendResponseHeaders(200, response.size.toLong())
            request.responseBody.use { it.write(response) }
        }
        service.createContext("/auth/refresh") { request ->
            val payload = request.requestBody.bufferedReader().use { it.readText() }
            if (!payload.contains(remembered) || payload.contains("a-test-password")) {
                request.sendResponseHeaders(401, -1)
                request.close()
            } else {
                val response = live.toByteArray()
                request.sendResponseHeaders(200, response.size.toLong())
                request.responseBody.use { it.write(response) }
            }
        }
        service.createContext("/collection") { request ->
            val response = """{"points":4,"floats":{}}""".toByteArray()
            request.sendResponseHeaders(200, response.size.toLong())
            request.responseBody.use { it.write(response) }
        }
        service.createContext("/auth/logout") { request ->
            request.sendResponseHeaders(204, -1)
            request.close()
        }
        service.start()
        try {
            val address = "http://127.0.0.1:${service.address.port}"
            val local = DesktopLeaderboardStore(root)
            val first = SharedLeaderboardStore(local, address, DesktopSessionVault(root))
            assertEquals(AccountResult.Connected, first.authenticate("camille", "a-test-password", null))
            val persisted = root.resolve("session/devquiz-session")
            assertFalse(persisted.readBytes().decodeToString().contains("a-test-password"))
            if (Platform.isWindows()) assertFalse(persisted.readBytes().decodeToString().contains(remembered))

            val restarted = SharedLeaderboardStore(local, address, DesktopSessionVault(root))
            restarted.restoreAccount()
            assertEquals("camille", restarted.accountName)
            assertEquals(userId, restarted.collectionAccountId)
            assertEquals(4, restarted.fetchCollection(userId).points)
            restarted.signOut()
            val afterLogout = SharedLeaderboardStore(local, address, DesktopSessionVault(root))
            afterLogout.restoreAccount()
            assertNull(afterLogout.accountName)
        } finally {
            service.stop(0)
            root.deleteRecursively()
        }
    }
}
