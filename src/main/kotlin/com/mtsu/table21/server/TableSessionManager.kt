package com.mtsu.table21.server

class TableSessionManager {

    // keep track of which players are at each table
    private val tables =
        mutableMapOf<String, MutableSet<String>>()

    fun joinTable(
        tableId: String,
        playerId: String
    ) {
        // create the table if it does not exist yet
        val players =
            tables.getOrPut(tableId) {
                mutableSetOf()
            }

        // a set keeps the same player from being added twice
        players.add(playerId)
    }

    fun leaveTable(
        tableId: String,
        playerId: String
    ) {
        // remove the player if the table exists
        tables[tableId]?.remove(playerId)

        // remove tables that have no players left
        if (tables[tableId]?.isEmpty() == true) {
            tables.remove(tableId)
        }
    }

    fun getPlayers(
        tableId: String
    ): Set<String> {
        // return a copy of the players or an empty set
        return tables[tableId]?.toSet()
            ?: emptySet()
    }
}
