package com.mtsu.table21.ui

import com.mtsu.table21.client.CasinoClient
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage

class MainApp : Application() {

    override fun start(stage: Stage) {

        // create one client connection for the application
        val casinoClient = CasinoClient()

        // open the login screen and pass the client to it
        val login = LoginView(casinoClient)

        // create the login scene
        val scene = Scene(login.getView(stage), 800.0, 600.0)

        // set up the main application window
        stage.title = "Welcome to the casino"
        stage.scene = scene

        // show the application
        stage.show()
    }
}

fun main() {

    // start the JavaFX application
    Application.launch(MainApp::class.java)
}
