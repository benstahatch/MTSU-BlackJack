package com.mtsu.table21.server

import com.mtsu.table21.client.CasinoClient
import com.mtsu.table21.database.DatabaseConnection
import com.mtsu.table21.database.WalletRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Uses the same configured MySQL database as AccountServiceTest. */
class AuthenticationTest {
    @Test fun bankruptcyClosesAuthenticatedSocketAndClearsClientSession() {
        val accounts = com.mtsu.table21.service.AccountService()
        val username = "broke_" + UUID.randomUUID().toString().take(12)
        val user = accounts.register(username, "TestPassword123!")
        val wallets = WalletRepository()
        wallets.setBalance(user.id, BigDecimal("10.00"))
        val cards = mutableListOf(com.mtsu.table21.model.Card.Rank.TEN,
            com.mtsu.table21.model.Card.Rank.TEN, com.mtsu.table21.model.Card.Rank.SIX,
            com.mtsu.table21.model.Card.Rank.NINE)
        val shoe = object : com.mtsu.table21.game.Shoe(6) {
            override fun size() = 52
            override fun dealCard() = com.mtsu.table21.model.Card(com.mtsu.table21.model.Card.Suit.HEARTS, cards.removeAt(0))
        }
        val house = object : com.mtsu.table21.database.HouseWalletRepository() {
            override fun addChips(amount: BigDecimal) = true
            override fun removeChips(amount: BigDecimal) = true
        }
        val manager = TableSessionManager(house, wallets, shoe)
        try {
            ServerSocket(0).use { server ->
                val worker = thread { ClientHandler(server.accept(), manager).run() }
                val connectedClient = CasinoClient("127.0.0.1", server.localPort)
                val messages = LinkedBlockingQueue<String>()
                val bankruptcy = java.util.concurrent.CountDownLatch(1)
                connectedClient.setMessageListener { messages.add(it) }
                connectedClient.setBankruptcyListener { bankruptcy.countDown() }
                fun expect(prefix: String): String {
                    while (true) {
                        val line = messages.poll(10, TimeUnit.SECONDS) ?: error("No server response")
                        if (line.startsWith(prefix)) return line
                        assertFalse(line.startsWith("ERROR|"), line)
                    }
                }
                try {
                    assertTrue(connectedClient.connect())
                    connectedClient.login(username, "TestPassword123!")
                    expect("AUTH_OK|")
                    connectedClient.sendCommand("JOIN_TABLE")
                    expect("JOINED|")
                    connectedClient.sendCommand("OPT_IN")
                    expect("OK|")
                    connectedClient.sendCommand("START_ROUND|10")
                    expect("OK|")
                    assertTrue(accounts.findUser(user.id).isPresent)
                    connectedClient.sendCommand("STAND")
                    assertTrue(expect("BANKRUPT|").contains("high score has been saved"))
                    assertTrue(bankruptcy.await(5, TimeUnit.SECONDS))
                    expect("DISCONNECTED")
                    assertNull(connectedClient.authenticatedUserId)
                    assertNull(connectedClient.playerBalance)
                    assertFalse(connectedClient.isConnected())
                    assertTrue(accounts.findUser(user.id).isEmpty)
                    assertTrue(wallets.findByUserId(user.id).isEmpty)
                } finally { connectedClient.disconnect(); worker.join(2000) }
            }
        } finally {
            DatabaseConnection.getConnection().use { c ->
                listOf("DELETE FROM game_history WHERE leaderboard_id IN (SELECT id FROM leaderboard WHERE username = ?)",
                    "DELETE FROM leaderboard WHERE username = ?").forEach { sql ->
                    c.prepareStatement(sql).use { it.setString(1, username); it.executeUpdate() }
                }
                listOf("DELETE FROM wallets WHERE user_id = ?", "DELETE FROM users WHERE id = ?").forEach { sql ->
                    c.prepareStatement(sql).use { it.setLong(1, user.id); it.executeUpdate() }
                }
            }
        }
    }

    @Test fun registerLoginLogoutAndSavedWalletOverProtocol() {
        val username = "wire|" + UUID.randomUUID().toString().take(12)
        val password = "test|password123"
        var userId: Long? = null
        val manager = TableSessionManager()
        ServerSocket(0).use { server ->
            val workers = mutableListOf<Thread>()
            val acceptor = thread {
                repeat(3) {
                    val socket = server.accept()
                    workers.add(thread { ClientHandler(socket, manager).run() })
                }
            }
            fun session(action: (CasinoClient, LinkedBlockingQueue<String>) -> Unit) {
                val client = CasinoClient("127.0.0.1", server.localPort)
                val messages = LinkedBlockingQueue<String>()
                client.setMessageListener { messages.add(it) }
                try { assertTrue(client.connect()); action(client, messages) }
                finally { client.disconnect() }
                assertNull(client.authenticatedUserId)
                assertNull(client.playerBalance)
            }
            fun reply(messages: LinkedBlockingQueue<String>): String =
                messages.poll(10, TimeUnit.SECONDS) ?: error("No server response")
            try {
                session { client, messages ->
                    client.sendCommand("GET_LEADERBOARD")
                    assertEquals("ERROR|Authentication required", reply(messages))
                    client.sendCommand("JOIN_TABLE|arbitrary")
                    assertEquals("ERROR|Authentication required", reply(messages))
                    client.sendCommand("LOGIN|invalid!|invalid!")
                    assertTrue(reply(messages).startsWith("AUTH_ERROR|"))
                    client.register(username, password)
                    assertTrue(reply(messages).startsWith("AUTH_OK|"))
                    userId = client.authenticatedUserId
                    assertNotNull(userId)
                    assertEquals(username, client.authenticatedUsername)
                    assertEquals(1000.0, client.playerBalance)
                    client.sendCommand("GET_LEADERBOARD")
                    val leaders = reply(messages)
                    assertTrue(leaders.startsWith("LEADERBOARD|"))
                    val records = leaders.substringAfter('|').split(',').filter { it.isNotEmpty() }
                    assertTrue(records.size <= 20)
                    records.forEach { assertEquals(5, it.split('~').size) }
                    assertFalse(leaders.contains("HOUSE"))
                    assertFalse(leaders.contains(password))
                    assertFalse(leaders.contains("password_hash"))
                    client.sendCommand("JOIN_TABLE|spoofed")
                    assertTrue(reply(messages).startsWith("JOINED|"))
                    val state = reply(messages)
                    val record = String(java.util.Base64.getUrlDecoder().decode(state.split('|')[6])).split('~')
                    assertEquals(username, String(java.util.Base64.getUrlDecoder().decode(record[2])))
                    client.sendCommand("LEAVE_TABLE")
                    assertEquals("LEFT", reply(messages))
                }
                WalletRepository().setBalance(userId!!, BigDecimal("1010.00"))
                session { client, messages ->
                    client.login(username, "incorrect")
                    assertEquals("AUTH_ERROR|Invalid username or password", reply(messages))
                    assertNull(client.authenticatedUserId)
                    client.login(username, password)
                    assertTrue(reply(messages).startsWith("AUTH_OK|"))
                    assertEquals(userId, client.authenticatedUserId)
                    assertEquals(1010.0, client.playerBalance)
                }
                session { client, messages ->
                    client.login(username, password)
                    assertTrue(reply(messages).startsWith("AUTH_OK|"))
                    client.sendCommand("JOIN_TABLE")
                    assertTrue(reply(messages).endsWith("|1010.0"))
                }
            } finally {
                server.close()
                acceptor.join(2000)
                workers.forEach { it.join(2000) }
                userId?.let { id ->
                    DatabaseConnection.getConnection().use { connection ->
                        listOf("leaderboard", "wallets", "users").forEach { table ->
                            val column = if (table != "users") "user_id" else "id"
                            connection.prepareStatement("DELETE FROM $table WHERE $column = ?").use {
                                it.setLong(1, id); it.executeUpdate()
                            }
                        }
                    }
                }
            }
        }
    }
}
