package com.mtsu.table21.ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font

class LoginView {
	//Creates a vertal propmt
    fun getView(): VBox {

        // Title
        val title = Label("TABLE 21")
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
            }
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
            message
        )

        layout.alignment = Pos.CENTER
        layout.padding = Insets(30.0)

        return layout
    }
}