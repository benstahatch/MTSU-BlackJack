package com.mtsu.table21.server

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TableSessionManagerTest {

    @Test
    // test that a player can join a table
    fun playerCanJoinTable() {

        val manager = TableSessionManager()

        manager.joinTable(
            "table-1",
            "benjamin"
        )

        val players =
            manager.getPlayers("table-1")

        assertEquals(
            setOf("benjamin"),
            players
        )
    }

    @Test
    // test that leaving removes the player from the table
    fun playerCanLeaveTable() {

        val manager = TableSessionManager()

        manager.joinTable(
            "table-1",
            "benjamin"
        )

        manager.leaveTable(
            "table-1",
            "benjamin"
        )

        val players =
            manager.getPlayers("table-1")

        assertEquals(
            emptySet<String>(),
            players
        )
    }
}
