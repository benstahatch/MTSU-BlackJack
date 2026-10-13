package com.mtsu.table21.ui

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

class LoginView {
	//Creates a vertal propmt
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

        //Logout buttom
        val quitButton = Button("QUIT")
        quitButton.prefWidth = 300.0

        //create user buttom
        val createUserButton = Button("Create Account")
        createUserButton.prefWidth = 300.0

        // Message shown after clicking login
        val message = Label()

        loginButton.setOnAction {

            val username = usernameField.text
            val password = passwordField.text

            if (username.isBlank() || password.isBlank()) {
                message.text = "Please enter your username and password."
                message.textFill = Color.RED
            } else {
                message.text = "Login request sent."
                message.textFill = Color.GREEN

                // Later, this is where we connect to the server.
                val lobby=LobbyView()
                val scene = Scene(lobby.getView(username, stage), 800.0, 600.0)
                stage.scene = scene
            }
        }

        createUserButton.setOnAction {
            val user=CreateUser()
            val scene = Scene(user.getView(stage), 800.0, 600.0)
            stage.scene = scene
        }

        quitButton.setOnAction {
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