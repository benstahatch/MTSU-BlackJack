package com.mtsu.table21.client

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CasinoClientTest {
    @Test
    fun addressDefaultsAndOverrides() {
        assertEquals("127.0.0.1", CasinoClient.serverHost(emptyMap()))
        assertEquals(5555, CasinoClient.serverPort(emptyMap()))
        assertEquals("192.168.1.25", CasinoClient.serverHost(mapOf("TABLE21_SERVER_HOST" to "192.168.1.25")))
        assertEquals(6000, CasinoClient.serverPort(mapOf("TABLE21_SERVER_PORT" to "6000")))
        assertEquals("127.0.0.1", CasinoClient.serverHost(mapOf("TABLE21_SERVER_HOST" to " ")))
        listOf("bad", "0", "65536").forEach {
            assertEquals(5555, CasinoClient.serverPort(mapOf("TABLE21_SERVER_PORT" to it)))
        }
    }

    @Test
    fun retainsLatestServerBalance() {
        val client = CasinoClient()
        assertNull(client.playerBalance)
        client.updatePlayerBalance(1000.0)
        client.updatePlayerBalance(990.0)
        assertEquals(990.0, client.playerBalance)
    }
}
