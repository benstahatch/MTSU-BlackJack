package com.mtsu.table21.server

import java.net.Socket

// keep the player's connection and table together
data class ClientSession(
    val playerId: String,                // who the player is
    val socket: Socket,                 // their network connection
    var currentTableId: String? = null // value is allowed to be null
)
