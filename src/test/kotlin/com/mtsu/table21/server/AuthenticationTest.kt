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
                        listOf("wallets", "users").forEach { table ->
                            val column = if (table == "wallets") "user_id" else "id"
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
