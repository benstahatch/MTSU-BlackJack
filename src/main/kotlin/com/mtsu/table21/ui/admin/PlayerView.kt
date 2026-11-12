package com.mtsu.table21.ui.admin

import com.mtsu.table21.client.CasinoClient
import javafx.application.Platform
import javafx.scene.control.ComboBox
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import java.util.Base64

class PlayerView(private val client: CasinoClient) {
    fun getView(onBack: () -> Unit): VBox {
        val playersText = AdminScreenStyle.label("Loading connected players...")
        val selection = ComboBox<String>().apply { promptText = "Select a player"; prefWidth = 240.0 }
        val message = AdminScreenStyle.label("")
        val refresh = AdminScreenStyle.button("Refresh Players")
        val kick = AdminScreenStyle.button("Remove from Table")

        fun request() { client.sendCommand("GET_ADMIN_PLAYERS") }
        client.setMessageListener { line -> Platform.runLater {
            when {
                line.startsWith("ADMIN_PLAYERS|") -> {
                    selection.items.clear()
                    val payload = line.substringAfter('|')
                    val rows = if (payload.isBlank()) emptyList() else payload.split(',').mapNotNull {
                        try { String(Base64.getUrlDecoder().decode(it), Charsets.UTF_8).split('~') }
                        catch (_: Exception) { null }
                    }
                    val decodedRows = rows.mapNotNull { f ->
                        val encodedName = f.getOrNull(0) ?: return@mapNotNull null
                        val name = try { String(Base64.getUrlDecoder().decode(encodedName), Charsets.UTF_8) }
                            catch (_: Exception) { return@mapNotNull null }
                        listOf(name) + f.drop(1)
                    }
                    playersText.text = if (decodedRows.isEmpty()) "No players are currently seated." else decodedRows.joinToString("\n\n") { f ->
                        "${f.getOrNull(0)} — Seat ${(f.getOrNull(1)?.toIntOrNull() ?: -1) + 1}\nBalance: $${f.getOrNull(2)}   Bet: $${f.getOrNull(3)}\nConnected: ${f.getOrNull(4)}   Status: ${f.getOrNull(5)}"
                    }
                    decodedRows.forEach { f -> f.getOrNull(0)?.let { selection.items.add(it) } }
                }
                line.startsWith("ADMIN_KICKED|") -> { message.text = "Removed ${line.substringAfter('|')} from the table."; request() }
                line.startsWith("ERROR|") -> message.text = line.substringAfter('|')
            }
        } }
        refresh.setOnAction { request() }
        kick.setOnAction {
            val name = selection.value
            if (name.isNullOrBlank()) message.text = "Select a player first."
            else {
                val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(name.toByteArray(Charsets.UTF_8))
                client.sendCommand("ADMIN_KICK|$encoded")
            }
        }
        request()
        return AdminScreenStyle.root("Players at Table21", onBack, listOf(
            playersText, HBox(10.0, selection, kick, refresh), message,
            AdminScreenStyle.label("Players can only be removed between rounds so an active hand is not interrupted.")
        ))
    }
}
