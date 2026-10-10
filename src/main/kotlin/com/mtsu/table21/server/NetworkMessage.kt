package com.mtsu.table21.server

// commands the server can recognize
enum class NetworkMessage {
    PING,
    JOIN_TABLE,
    LEAVE_TABLE,
    PLACE_BET,
    HIT,
    STAND,
    DOUBLE_DOWN,
    SPLIT,
    QUIT
}
