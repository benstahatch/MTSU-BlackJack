package com.mtsu.table21.ui

import com.mtsu.table21.client.CasinoClient
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.Stage

class LoginView(
    private val casinoClient: CasinoClient
) {

    // Creates a vertical prompt
    fun getView(stage: Stage): VBox {

        // Title
        val title = Label("Casino Login")
        title.font = Font.font(32.0)

        // Username
        val usernameLabel = Label("Username")
        val usernameField = TextField()
        usernameField.promptText = "Enter username"
        usernameField.maxWidth = 300.0

        // Password
        val passwordLabel = Label("Password")
        val passwordField = PasswordField()
        passwordField.promptText = "Enter password"
        passwordField.maxWidth = 300.0

        // Login button
        val loginButton = Button("LOGIN")
        loginButton.prefWidth = 300.0

        // Quit button
        val quitButton = Button("QUIT")
        quitButton.prefWidth = 300.0

        // Create user button
        val createUserButton = Button("Create Account")
        createUserButton.prefWidth = 300.0

        // Message shown after clicking login
        val message = Label()

        loginButton.setOnAction {

            val username = usernameField.text
            val password = passwordField.text

            // Make sure both fields are filled in
            if (username.isBlank() || password.isBlank()) {

                message.text = "Please enter your username and password."
                message.textFill = Color.RED

            } else {

                // Try connecting to the Table21 server
                val connected = casinoClient.connect()

                if (!connected) {

                    message.text = "Could not connect to Table21 server."
                    message.textFill = Color.RED

                } else {

                    // Send a test message to make sure the server responds
                    val response = casinoClient.sendCommand("PING")

                    if (response == "PONG") {

                        message.text = "Connected to server."
                        message.textFill = Color.GREEN

                        // Open lobby after server connection succeeds
                        val lobby = LobbyView(casinoClient)

                        val scene = Scene(
                            lobby.getView(username, stage),
                            800.0,
                            600.0
                        )

                        stage.scene = scene

                    } else {

                        message.text = "Server did not respond."
                        message.textFill = Color.RED
                    }
                }
            }
        }

        createUserButton.setOnAction {

            // Open the create account screen
            val user = CreateUser(casinoClient)

            val scene = Scene(
                user.getView(stage),
                800.0,
                600.0
            )

            stage.scene = scene
        }

        quitButton.setOnAction {

            // Close server connection before quitting
            casinoClient.disconnect()

            javafx.application.Platform.exit()
        }

        // Put everything into the layout
        val layout = VBox(
            10.0,
            title,
            usernameLabel,
            usernameField,
            passwordLabel,
            passwordField,
            loginButton,
            createUserButton,
            quitButton,
            message
        )

        layout.alignment = Pos.CENTER
        layout.padding = Insets(30.0)

        return layout
    }
}
