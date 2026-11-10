package com.mtsu.table21.ui

import com.mtsu.table21.game.BlackjackRules
import com.mtsu.table21.game.BlackjackTable
import com.mtsu.table21.game.GameRound
import com.mtsu.table21.model.Bet
import com.mtsu.table21.model.Card
import com.mtsu.table21.model.Hand
import com.mtsu.table21.model.Wallet
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

class BlackjackTableView(
    private val blackjackTable: BlackjackTable,
    private val wallet: Wallet,
    private val onQuit: () -> Unit
) : StackPane() {

    private val tablePane = Pane()

    private val dealerCards = HBox(6.0)
    private val playerCards = HBox(5.0)

    private val dealerTotal = Label("")
    private val playerName = Label(blackjackTable.player.name)
    private val playerTotal = Label("")

    private val walletLabel = Label()
    private val betLabel = Label("Bet: $0.00")
    private val statusLabel = Label("Enter a bet, then press Deal.")

    private val betInput = TextField("10")
    private val dealButton = Button("Deal")
    private val hitButton = Button("Hit")
    private val standButton = Button("Stand")
    private val resetButton = Button("Reset")
    private val quitButton = Button("Quit")

    private var currentBet: Bet? = null
    private var showRound = false
    private var payoutApplied = false

    init {
        prefWidth = 800.0
        prefHeight = 600.0
        minWidth = 800.0
        minHeight = 600.0
        maxWidth = 800.0
        maxHeight = 600.0

        createBackground()
        createTableOverlay()
        setupButtons()
        updateView()
    }

    private fun createBackground() {
        val resource = javaClass.getResource("/images/BackGround blackjack/blackjack_table.png")
            ?: throw IllegalStateException(
                "Missing /images/blackjack_table.png. " +
                        "Place the PNG in src/main/resources/images/"
            )

        val background = ImageView(Image(resource.toExternalForm())).apply {
            fitWidth = 800.0
            fitHeight = 600.0
            isPreserveRatio = false
            isMouseTransparent = true
        }

        children.add(background)
        children.add(tablePane)
    }

    private fun createTableOverlay() {
        // Dealer cards fit inside the two card outlines in the background PNG.
        dealerCards.alignment = Pos.CENTER
        dealerCards.layoutX = 330.0
        dealerCards.layoutY = 48.0
        dealerCards.prefWidth = 140.0
        dealerCards.prefHeight = 90.0
        tablePane.children.add(dealerCards)

        dealerTotal.apply {
            font = Font.font("Georgia", FontWeight.BOLD, 15.0)
            textFill = GOLD
        }
        place(dealerTotal, 340.0, 140.0)
        tablePane.children.add(dealerTotal)

        // This version of the backend has one Player per BlackjackTable.
        // The local player is displayed at seat 1 (left side of the image).
        playerName.apply {
            font = Font.font("Georgia", FontWeight.BOLD, 14.0)
            textFill = GOLD
        }
        place(playerName, 48.0, 222.0, 135.0)
        tablePane.children.add(playerName)

        playerCards.alignment = Pos.CENTER
        playerCards.layoutX = 42.0
        playerCards.layoutY = 244.0
        playerCards.prefWidth = 145.0
        playerCards.prefHeight = 68.0
        tablePane.children.add(playerCards)

        betLabel.apply {
            font = Font.font("Georgia", FontWeight.BOLD, 13.0)
            textFill = GOLD
        }
        place(betLabel, 72.0, 365.0, 100.0)
        tablePane.children.add(betLabel)

        playerTotal.apply {
            font = Font.font("Arial", FontWeight.BOLD, 12.0)
            textFill = Color.WHITE
        }
        place(playerTotal, 65.0, 385.0, 115.0)
        tablePane.children.add(playerTotal)

        walletLabel.apply {
            font = Font.font("Arial", FontWeight.BOLD, 14.0)
            textFill = GOLD
        }
        place(walletLabel, 30.0, 488.0, 190.0)
        tablePane.children.add(walletLabel)

        statusLabel.apply {
            font = Font.font("Arial", FontWeight.BOLD, 13.0)
            textFill = Color.WHITE
            alignment = Pos.CENTER
            prefWidth = 340.0
            maxWidth = 340.0
        }
        place(statusLabel, 230.0, 485.0, 340.0)
        tablePane.children.add(statusLabel)

        val betPrompt = Label("Bet amount:")
        betPrompt.apply {
            font = Font.font("Arial", FontWeight.BOLD, 12.0)
            textFill = GOLD
        }
        place(betPrompt, 640.0, 468.0, 100.0)
        tablePane.children.add(betPrompt)

        betInput.apply {
            prefWidth = 100.0
            maxWidth = 100.0
            prefHeight = 27.0
            style = """
                -fx-background-color: #F5F0DF;
                -fx-text-fill: #153D2E;
                -fx-font-weight: bold;
                -fx-background-radius: 5;
            """.trimIndent()
        }
        place(betInput, 640.0, 490.0, 100.0)
        tablePane.children.add(betInput)

        styleButton(dealButton, "#D9B84F", "#153D2E")
        styleButton(hitButton, "#174D35", "#FFFFFF")
        styleButton(standButton, "#174D35", "#FFFFFF")
        styleButton(resetButton, "#174D35", "#FFFFFF")
        styleButton(quitButton, "#8B2525", "#FFFFFF")

        place(dealButton, 180.0, 535.0, 95.0, 38.0)
        place(hitButton, 285.0, 535.0, 85.0, 38.0)
        place(standButton, 380.0, 535.0, 95.0, 38.0)
        place(resetButton, 485.0, 535.0, 95.0, 38.0)
        place(quitButton, 680.0, 535.0, 80.0, 38.0)

        tablePane.children.addAll(
            dealButton,
            hitButton,
            standButton,
            resetButton,
            quitButton
        )
    }

    private fun place(
        node: javafx.scene.Node,
        x: Double,
        y: Double,
        width: Double? = null,
        height: Double? = null
    ) {
        node.layoutX = x
        node.layoutY = y

        if (width != null && node is javafx.scene.layout.Region) {
            node.prefWidth = width
            node.minWidth = width
        }
        if (height != null && node is javafx.scene.layout.Region) {
            node.prefHeight = height
            node.minHeight = height
        }
    }

    private fun styleButton(button: Button, background: String, foreground: String) {
        button.apply {
            prefHeight = 38.0
            minHeight = 38.0
            font = Font.font("Arial", FontWeight.BOLD, 13.0)
            style = """
                -fx-background-color: $background;
                -fx-text-fill: $foreground;
                -fx-border-color: #E7C65A;
                -fx-border-width: 1.5;
                -fx-background-radius: 7;
                -fx-border-radius: 7;
                -fx-cursor: hand;
            """.trimIndent()
        }
    }

    private fun setupButtons() {
        dealButton.setOnAction {
            startRoundWithBet()
        }

        hitButton.setOnAction {
            try {
                blackjackTable.currentRound.hit()
                updateView()
            } catch (e: IllegalStateException) {
                statusLabel.text = e.message ?: "You cannot hit right now."
            }
        }

        standButton.setOnAction {
            try {
                blackjackTable.currentRound.stand()
                updateView()
            } catch (e: IllegalStateException) {
                statusLabel.text = e.message ?: "You cannot stand right now."
            }
        }

        resetButton.setOnAction {
            if (blackjackTable.hasActiveRound()) {
                statusLabel.text = "Finish the current round before resetting."
            } else {
                showRound = false
                currentBet = null
                payoutApplied = false
                dealerCards.children.clear()
                playerCards.children.clear()
                dealerTotal.text = ""
                playerTotal.text = ""
                betLabel.text = "Bet: $0.00"
                statusLabel.text = "Ready for a new round. Enter a bet and press Deal."
                updateView()
            }
        }
        quitButton.setOnAction {
            onQuit()
        }
    }

    private fun startRoundWithBet() {
        if (blackjackTable.hasActiveRound()) {
            statusLabel.text = "The current round is still in progress."
            return
        }

        val amount = betInput.text.trim().toDoubleOrNull()
        if (amount == null || !amount.isFinite() || amount <= 0.0) {
            statusLabel.text = "Enter a valid bet greater than zero."
            return
        }

        if (!wallet.canAfford(amount)) {
            statusLabel.text = "You do not have enough chips for that bet."
            return
        }

        val newBet = try {
            Bet(amount)
        } catch (e: IllegalArgumentException) {
            statusLabel.text = e.message ?: "Invalid bet."
            return
        }

        // Take the wager before starting the round.
        wallet.removeChips(amount)

        try {
            blackjackTable.startNewRound()
            currentBet = newBet
            payoutApplied = false
            showRound = true
            statusLabel.text = "Your turn."
            updateView()
        } catch (e: RuntimeException) {
            // Return the wager if the game could not start.
            wallet.addChips(amount)
            currentBet = null
            statusLabel.text = e.message ?: "Could not start the round."
            updateView()
        }
    }

    fun updateView() {
        walletLabel.text = "Chips: ${formatChips(wallet.balance)}"

        val round = blackjackTable.currentRound

        if (!showRound || round == null) {
            hitButton.isDisable = true
            standButton.isDisable = true
            dealButton.isDisable = blackjackTable.hasActiveRound()
            return
        }

        val player = blackjackTable.player
        playerName.text = player.name
        betLabel.text = "Bet: ${formatChips(currentBet?.amount ?: 0.0)}"

        updatePlayerCards(player.hand)
        updateDealerCards(round)

        val isPlayerTurn = round.state == GameRound.State.PLAYER_TURN
        hitButton.isDisable = !isPlayerTurn
        standButton.isDisable = !isPlayerTurn
        dealButton.isDisable = !round.isFinished()
        resetButton.isDisable = round.isFinished().not()

        if (round.isFinished()) {
            settleBet(round)

            val result = round.getResult()
            val resultText = when (result) {
                BlackjackRules.Result.PLAYER_WIN -> "You win!"
                BlackjackRules.Result.DEALER_WIN -> "Dealer wins."
                BlackjackRules.Result.PUSH -> "Push — your bet was returned."
                else -> "Round finished."
            }

            statusLabel.text = resultText
            dealerTotal.text = "Dealer total: ${round.dealer.hand.value}"
            walletLabel.text = "Chips: ${formatChips(wallet.balance)}"
        } else {
            statusLabel.text = "Your turn."
            dealerTotal.text = ""
        }
    }

    private fun settleBet(round: GameRound) {
        if (payoutApplied) return

        val amount = currentBet?.amount ?: return

        when (round.getResult()) {
            BlackjackRules.Result.PLAYER_WIN -> {
                // Return the original stake plus an equal amount in winnings.
                wallet.addChips(amount * 2.0)
            }
            BlackjackRules.Result.PUSH -> {
                // Return the original stake on a tie.
                wallet.addChips(amount)
            }
            BlackjackRules.Result.DEALER_WIN -> {
                // The wager was already removed, so no further change is needed.
            }
            else -> return
        }

        payoutApplied = true
    }

    private fun updatePlayerCards(hand: Hand) {
        playerCards.children.clear()

        for (index in 0 until hand.cardCount) {
            playerCards.children.add(createCard(hand.getCard(index)))
        }

        playerTotal.text = "Total: ${hand.value}"
    }

    private fun updateDealerCards(round: GameRound) {
        dealerCards.children.clear()

        val hand = round.dealer.hand
        val revealAll = round.isFinished()

        for (index in 0 until hand.cardCount) {
            val cardNode = if (index == 1 && !revealAll) {
                createHiddenCard()
            } else {
                createCard(hand.getCard(index))
            }
            dealerCards.children.add(cardNode)
        }
    }

    private fun createCard(card: Card): StackPane {

        // Convert the card rank into a number.
        val rankNumber = when (card.rank.name) {
            "ACE" -> 1
            "TWO" -> 2
            "THREE" -> 3
            "FOUR" -> 4
            "FIVE" -> 5
            "SIX" -> 6
            "SEVEN" -> 7
            "EIGHT" -> 8
            "NINE" -> 9
            "TEN" -> 10
            "JACK" -> 11
            "QUEEN" -> 12
            "KING" -> 13
            else -> throw IllegalArgumentException(
                "Unknown card rank: ${card.rank.name}"
            )
        }

        // Determine the starting number for each suit.
        val suitOffset = when (card.suit.name) {
            "SPADES" -> 0
            "HEARTS" -> 13
            "DIAMONDS" -> 26
            "CLUBS" -> 39
            else -> throw IllegalArgumentException(
                "Unknown card suit: ${card.suit.name}"
            )
        }

        // Calculate the PNG filename.
        val cardNumber = suitOffset + rankNumber
        val fileName = "$cardNumber.png"

        // Load the card image.
        val resource = javaClass.getResource(
            "/images/cards/$fileName"
        ) ?: throw IllegalStateException(
            "Missing card image: $fileName"
        )

        val imageView = ImageView(
            Image(resource.toExternalForm())
        ).apply {
            fitWidth = 58.0
            fitHeight = 84.0
            isPreserveRatio = false
            isSmooth = true
        }

        return StackPane(imageView).apply {
            prefWidth = 58.0
            minWidth = 58.0
            prefHeight = 84.0
            minHeight = 84.0
        }
    }

    private fun createHiddenCard(): StackPane {
        val resource = javaClass.getResource("/images/cards/b1fv.png")
            ?: throw IllegalStateException("Missing b1fv.png")

        val imageView = ImageView(Image(resource.toExternalForm())).apply {
            fitWidth = 58.0
            fitHeight = 84.0
            isPreserveRatio = false
        }

        return StackPane(imageView).apply {
            prefWidth = 58.0
            minWidth = 58.0
            prefHeight = 84.0
            minHeight = 84.0
        }
    }

    fun setStatus(message: String) {
        statusLabel.text = message
    }

    private fun formatChips(amount: Double): String =
        String.format("$%.2f", amount)

    companion object {
        private val GOLD = Color.web("#E7C65A")
    }
}
