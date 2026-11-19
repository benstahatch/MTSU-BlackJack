package com.mtsu.table21.ui

import com.mtsu.table21.client.CasinoClient
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Base64
import java.util.Locale

class LeaderboardView(private val client: CasinoClient) {
    fun getView(onBack: () -> Unit): VBox {
        val title = Label("TABLE21\nALL-TIME LEADERS").apply {
            font = Font.font("Monospaced", 30.0); textFill = Color.GOLD; alignment = Pos.CENTER
        }
        val status = Label("Loading high scores…").apply { textFill = Color.GOLD }
        val grid = GridPane().apply { hgap = 24.0; vgap = 12.0; padding = Insets(16.0) }
        val money = NumberFormat.getCurrencyInstance(Locale.US)
        fun cell(value: String, column: Int, row: Int) {
            grid.add(Label(value).apply { textFill = if (row == 0) Color.GOLD else Color.WHITE }, column, row)
        }
        fun headings() {
            grid.children.clear()
            listOf("Rank", "Player", "High Score", "Peak Profit", "Wins").forEachIndexed { col, name -> cell(name, col, 0) }
        }
        headings()
        var active = true
        client.setMessageListener { line ->
            Platform.runLater {
                if (active) {
                    when {
                        line.startsWith("LEADERBOARD|") -> {
                            headings()
                            val rows = line.substringAfter('|').split(',').filter { it.isNotEmpty() }.take(20)
                            rows.forEachIndexed { index, row ->
                                val fields = row.split('~')
                                val name = String(Base64.getUrlDecoder().decode(fields[1]), Charsets.UTF_8)
                                val profit = BigDecimal(fields[3])
                                listOf(fields[0], name, money.format(BigDecimal(fields[2])),
                                    (if (profit.signum() >= 0) "+" else "") + money.format(profit), fields[4])
                                    .forEachIndexed { col, text -> cell(text, col, index + 1) }
                            }
                            status.text = if (rows.isEmpty()) "No scores yet. Be the first!" else "Top 20 • Highest bankroll per run"
                        }
                        line.startsWith("ERROR|") -> status.text = line.substringAfter('|')
                        line == "DISCONNECTED" -> status.text = "Disconnected. Return to login to reconnect."
                    }
                }
            }
        }
        val back = Button("Back").apply {
            setOnAction { active = false; client.setMessageListener(null); onBack() }
        }
        val scroll = ScrollPane(grid).apply {
            isFitToWidth = true; prefHeight = 390.0
            style = "-fx-background: #123D2B; -fx-background-color: #123D2B;"
        }
        if (client.sendCommand("GET_LEADERBOARD") != "SENT") status.text = "Disconnected. Return to login to reconnect."
        return VBox(16.0, title, status, scroll, back).apply {
            alignment = Pos.CENTER; padding = Insets(24.0); style = "-fx-background-color: #123D2B;"
        }
    }
}
