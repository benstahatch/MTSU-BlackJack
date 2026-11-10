package com.mtsu.table21.ui

import com.mtsu.table21.client.CasinoClient
import com.mtsu.table21.model.Card
import javafx.application.Platform
import javafx.geometry.NodeOrientation
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import java.util.Base64

class BlackjackTableView(
    private val casinoClient: CasinoClient,
    private val userName: String,
    private val onQuit: () -> Unit
) : StackPane() {
    private data class PlayerSnapshot(
        val id: String, val seat: Int, val name: String, val balance: Double,
        val optedIn: Boolean, val bet: Double, val result: String,
        val handIndex: Int, val hands: List<List<Card>>
    )

    private var myPlayerId: String? = null
    private var mySeat: Int = -1
    private var phase = "WAITING"
    private var currentPlayerId = "-"
    private var dealerHidden = true
    private var dealerCardsState: List<Card> = emptyList()
    private var playerState: List<PlayerSnapshot> = emptyList()

    private val tablePane = Pane()
    private val dealerCards = HBox(5.0)
    private val dealerTotal = Label("")
    private val playerNames = List(4) { Label("Seat ${it + 1} — Empty") }
    private val playerCards = List(4) { HBox(3.0) }
    private val playerInfo = List(4) { Label("") }
    private val statusLabel = Label("Joining table...")
    private val betInput = TextField("10")

    private val optInButton = Button("Play This Round")
    private val sitOutButton = Button("Sit Out")
    private val betButton = Button("Set Bet")
    private val dealButton = Button("Deal")
    private val hitButton = Button("Hit")
    private val standButton = Button("Stand")
    private val splitButton = Button("Split")
    private val doubleButton = Button("Double")
    private val quitButton = Button("Quit")

    init {
        prefWidth = 800.0; prefHeight = 600.0
        minWidth = 800.0; minHeight = 600.0
        maxWidth = 800.0; maxHeight = 600.0
        createBackground()
        createTableOverlay()
        setupButtons()
        casinoClient.setMessageListener { line -> Platform.runLater { handleServerMessage(line) } }
        updateView()
    }

    private fun createBackground() {
        val resource = javaClass.getResource("/images/BackGround blackjack/blackjack_table.png")
            ?: throw IllegalStateException("Missing blackjack_table.png in resources.")
        val background = ImageView(Image(resource.toExternalForm())).apply {
            fitWidth = 800.0; fitHeight = 600.0; isPreserveRatio = false; isMouseTransparent = true
        }
        children.addAll(background, tablePane)
    }

    private fun createTableOverlay() {
        dealerCards.apply { alignment = Pos.CENTER; layoutX = 320.0; layoutY = 45.0; prefWidth = 160.0; prefHeight = 75.0 }
        tablePane.children.add(dealerCards)
        dealerTotal.apply { font = Font.font("Georgia", FontWeight.BOLD, 14.0); textFill = GOLD }
        place(dealerTotal, 325.0, 125.0, 160.0); tablePane.children.add(dealerTotal)

        val positions = listOf(35.0 to 180.0, 425.0 to 180.0, 35.0 to 350.0, 425.0 to 350.0)
        for (i in 0..3) {
            val (x, y) = positions[i]
            playerNames[i].apply { font = Font.font("Georgia", FontWeight.BOLD, 14.0); textFill = GOLD }
            place(playerNames[i], x, y, 160.0); tablePane.children.add(playerNames[i])
            playerCards[i].apply { alignment = Pos.CENTER_LEFT; layoutX = x; layoutY = y + 25.0; prefWidth = 340.0; prefHeight = 65.0 }
            tablePane.children.add(playerCards[i])
            playerInfo[i].apply { font = Font.font("Arial", FontWeight.BOLD, 11.0); textFill = Color.WHITE }
            place(playerInfo[i], x, y + 95.0, 340.0); tablePane.children.add(playerInfo[i])
        }
        statusLabel.apply {
            font = Font.font("Arial", FontWeight.BOLD, 12.0); textFill = Color.WHITE
            alignment = Pos.CENTER; prefWidth = 430.0; maxWidth = 430.0; isWrapText = true
        }
        place(statusLabel, 185.0, 465.0, 430.0, 35.0); tablePane.children.add(statusLabel)
        val betPrompt = Label("Your bet:").apply { font = Font.font("Arial", FontWeight.BOLD, 12.0); textFill = GOLD }
        place(betPrompt, 650.0, 450.0, 100.0); tablePane.children.add(betPrompt)
        betInput.apply {
            prefWidth = 95.0; prefHeight = 27.0
            style = "-fx-background-color: #F5F0DF; -fx-text-fill: #153D2E; -fx-font-weight: bold; -fx-background-radius: 5;"
        }
        place(betInput, 650.0, 475.0, 95.0); tablePane.children.add(betInput)

        listOf(optInButton, sitOutButton, betButton, dealButton, hitButton, standButton, splitButton, doubleButton, quitButton)
            .forEach { styleButton(it) }
        val buttonPositions = listOf(
            optInButton to (15.0 to 535.0), sitOutButton to (125.0 to 535.0),
            betButton to (215.0 to 535.0), dealButton to (300.0 to 535.0),
            hitButton to (385.0 to 535.0), standButton to (445.0 to 535.0),
            splitButton to (510.0 to 535.0), doubleButton to (575.0 to 535.0),
            quitButton to (690.0 to 535.0)
        )
        buttonPositions.forEach { (button, pos) ->
            val width = when (button) {
                optInButton -> 105.0; sitOutButton -> 82.0; betButton -> 78.0
                dealButton -> 75.0; hitButton -> 55.0; standButton -> 60.0
                splitButton -> 60.0; doubleButton -> 90.0; else -> 70.0
            }
            place(button, pos.first, pos.second, width, 38.0)
            tablePane.children.add(button)
        }
    }

    private fun place(node: javafx.scene.Node, x: Double, y: Double, width: Double? = null, height: Double? = null) {
        node.layoutX = x; node.layoutY = y
        if (width != null && node is javafx.scene.layout.Region) { node.prefWidth = width; node.minWidth = width }
        if (height != null && node is javafx.scene.layout.Region) { node.prefHeight = height; node.minHeight = height }
    }

    private fun styleButton(button: Button) {
        button.apply {
            prefHeight = 38.0; minHeight = 38.0
            font = Font.font("Arial", FontWeight.BOLD, 10.0)
            style = "-fx-background-color: #174D35; -fx-text-fill: white; -fx-border-color: #E7C65A; -fx-border-width: 1.5; -fx-background-radius: 7; -fx-border-radius: 7; -fx-cursor: hand;"
        }
    }

    private fun setupButtons() {
        optInButton.setOnAction { send("OPT_IN") }
        sitOutButton.setOnAction { send("SIT_OUT") }
        betButton.setOnAction {
            val amount = betInput.text.trim().toDoubleOrNull()
            if (amount == null || !amount.isFinite() || amount <= 0.0) statusLabel.text = "Enter a valid bet greater than zero."
            else send("PLACE_BET|$amount")
        }
        dealButton.setOnAction { send("START_ROUND") }
        hitButton.setOnAction { send("HIT") }
        standButton.setOnAction { send("STAND") }
        splitButton.setOnAction { send("SPLIT") }
        doubleButton.setOnAction { send("DOUBLE_DOWN") }
        quitButton.setOnAction {
            send("LEAVE_TABLE")
            casinoClient.setMessageListener(null)
            onQuit()
        }
    }

    private fun send(command: String) {
        val result = casinoClient.sendCommand(command)
        if (result != "SENT") statusLabel.text = result
    }

    private fun handleServerMessage(line: String) {
        when {
            line == "DISCONNECTED" -> statusLabel.text = "Disconnected from server."
            line.startsWith("JOINED|") -> {
                val parts = line.split('|')
                myPlayerId = parts.getOrNull(1)
                mySeat = parts.getOrNull(2)?.toIntOrNull() ?: -1
                statusLabel.text = "Joined Table #1. You are Player ${mySeat + 1}. Choose Play This Round and set your bet."
            }
            line.startsWith("ERROR|") -> statusLabel.text = line.substringAfter('|')
            line.startsWith("OK|") -> statusLabel.text = line.substringAfter('|')
            line.startsWith("STATE|") -> parseState(line)
            line == "LEFT" -> statusLabel.text = "You left the table."
        }
    }

    private fun parseState(line: String) {
        val parts = line.split('|', limit = 7)
        if (parts.size < 7) return
        phase = parts[2]
        dealerCardsState = parseCards(parts[3])
        dealerHidden = parts[4].toBoolean()
        currentPlayerId = parts[5]
        playerState = if (parts[6].isBlank()) emptyList() else parts[6].split(',').mapNotNull { encoded ->
            try {
                val fields = String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8).split('~', limit = 10)
                if (fields.size < 10) return@mapNotNull null
                val hands = if (fields[9].isBlank()) emptyList() else fields[9].split(';').map { parseCards(it) }
                PlayerSnapshot(fields[0], fields[1].toInt(), decode(fields[2]), fields[3].toDouble(),
                    fields[4].toBoolean(), fields[5].toDouble(), fields[6], fields[7].toInt(), hands)
            } catch (_: Exception) { null }
        }
        updateView()
    }

    private fun decode(value: String) = String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)

    private fun parseCards(value: String): List<Card> {
        if (value.isBlank()) return emptyList()
        return value.split(',').mapNotNull { token ->
            val pair = token.split(':')
            if (pair.size != 2) null else try {
                Card(Card.Suit.valueOf(pair[1]), Card.Rank.valueOf(pair[0]))
            } catch (_: Exception) { null }
        }
    }

    private fun updateView() {
        dealerCards.children.clear()
        dealerCardsState.forEachIndexed { index, card ->
            val hidden = dealerHidden && phase == "PLAYERS" && index == 1
            dealerCards.children.add(if (hidden) createSmallHiddenCard() else createSmallCard(card))
        }
        dealerTotal.text = if (!dealerHidden || phase == "FINISHED") {
            "Dealer: ${dealerCardsState.sumOf { it.value }}"
        } else "Dealer"

        for (seat in 0..3) {
            val p = playerState.firstOrNull { it.seat == seat }
            playerNames[seat].text = p?.name ?: "Seat ${seat + 1} — Empty"
            playerCards[seat].children.clear()
            playerCards[seat].nodeOrientation = if (seat == 1 || seat == 3) NodeOrientation.RIGHT_TO_LEFT else NodeOrientation.LEFT_TO_RIGHT
            playerCards[seat].alignment = if (seat == 1 || seat == 3) Pos.CENTER_RIGHT else Pos.CENTER_LEFT
            playerNames[seat].alignment = if (seat == 1 || seat == 3) Pos.CENTER_RIGHT else Pos.CENTER_LEFT
            if (p != null) {
                p.hands.forEachIndexed { handIndex, cards ->
                    if (handIndex > 0) playerCards[seat].children.add(Label(" | ").apply { textFill = GOLD })
                    cards.forEach { playerCards[seat].children.add(createSmallCard(it)) }
                }
                val totals = p.hands.map { cards -> handTotal(cards) }
                val totalText = if (totals.size > 1) "Totals: ${totals.joinToString(" / ")}" else "Total: ${totals.firstOrNull() ?: 0}"
                playerInfo[seat].text = "$totalText   Bet: ${money(p.bet)}   Chips: ${money(p.balance)}   ${p.result}"
            } else playerInfo[seat].text = ""
        }

        val mine = playerState.firstOrNull { it.id == myPlayerId }
        val myTurn = mine != null && currentPlayerId == myPlayerId && phase == "PLAYERS"
        val betweenRounds = phase == "WAITING" || phase == "FINISHED"
        optInButton.isDisable = !betweenRounds || mine?.optedIn == true
        sitOutButton.isDisable = !betweenRounds || mine == null || !mine.optedIn
        betButton.isDisable = !betweenRounds || mine == null || mine.optedIn.not()
        betInput.isDisable = !betweenRounds || mine == null || mine.optedIn.not()
        dealButton.isDisable = !betweenRounds || mine == null
        hitButton.isDisable = !myTurn
        standButton.isDisable = !myTurn
        val currentHand = mine?.hands?.getOrNull(mine.handIndex)
        splitButton.isDisable = !myTurn || (currentHand?.let { it.size != 2 || it[0].rank != it[1].rank } ?: true)
        doubleButton.isDisable = !myTurn || currentHand?.size != 2

        if (phase == "PLAYERS") {
            val active = playerState.firstOrNull { it.id == currentPlayerId }
            statusLabel.text = if (myTurn) "Your turn — Player ${mySeat + 1}" else "${active?.name ?: "Waiting"}'s turn"
        } else if (phase == "DEALER") statusLabel.text = "Dealer's turn"
        else if (phase == "FINISHED") statusLabel.text = "Round finished. Choose Play This Round to join the next round."
        else if (mine != null) statusLabel.text = "Choose Play This Round, then set your bet."
    }

    private fun createSmallCard(card: Card): StackPane = createCard(card).apply {
        children.filterIsInstance<ImageView>().forEach { it.fitWidth = 40.0; it.fitHeight = 58.0 }
        prefWidth = 40.0; minWidth = 40.0; prefHeight = 58.0; minHeight = 58.0
    }

    private fun createCard(card: Card): StackPane {
        val rankNumber = when (card.rank) {
            Card.Rank.ACE -> 1; Card.Rank.TWO -> 2; Card.Rank.THREE -> 3; Card.Rank.FOUR -> 4
            Card.Rank.FIVE -> 5; Card.Rank.SIX -> 6; Card.Rank.SEVEN -> 7; Card.Rank.EIGHT -> 8
            Card.Rank.NINE -> 9; Card.Rank.TEN -> 10; Card.Rank.JACK -> 11; Card.Rank.QUEEN -> 12; Card.Rank.KING -> 13
        }
        val suitOffset = when (card.suit) {
            Card.Suit.SPADES -> 0; Card.Suit.HEARTS -> 13; Card.Suit.DIAMONDS -> 26; Card.Suit.CLUBS -> 39
        }
        val fileName = "${suitOffset + rankNumber}.png"
        val resource = javaClass.getResource("/images/cards/$fileName") ?: throw IllegalStateException("Missing card image: $fileName")
        val imageView = ImageView(Image(resource.toExternalForm())).apply {
            fitWidth = 58.0; fitHeight = 84.0; isPreserveRatio = false; isSmooth = true
        }
        return StackPane(imageView).apply { prefWidth = 58.0; minWidth = 58.0; prefHeight = 84.0; minHeight = 84.0 }
    }

    private fun createSmallHiddenCard(): StackPane {
        val resource = javaClass.getResource("/images/cards/b1fv.png")
            ?: throw IllegalStateException("Missing b1fv.png")
        val imageView = ImageView(Image(resource.toExternalForm())).apply {
            fitWidth = 40.0
            fitHeight = 58.0
            isPreserveRatio = false
            isSmooth = true
        }
        return StackPane(imageView).apply {
            prefWidth = 40.0
            minWidth = 40.0
            prefHeight = 58.0
            minHeight = 58.0
        }
    }

    // Calculates a blackjack hand total, counting Aces as 11 when possible
    // and reducing them to 1 when the hand would otherwise bust.
    private fun handTotal(cards: List<Card>): Int {
        var total = 0
        var aces = 0
        for (card in cards) {
            if (card.rank == Card.Rank.ACE) {
                total += 11
                aces++
            } else {
                total += card.value
            }
        }
        while (total > 21 && aces > 0) {
            total -= 10
            aces--
        }
        return total
    }

    private fun money(amount: Double) = "$" + String.format("%.2f", amount)

    companion object { private val GOLD = Color.web("#E7C65A") }
}
