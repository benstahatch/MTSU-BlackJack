package com.mtsu.table21.ui

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage

class MainApp : Application() {

    override fun start(stage: Stage) {
    	val login=LoginView()
        val scene = Scene(login.getView(stage), 800.0, 600.0)
 
        stage.title = "Welcome to the casino"
        stage.scene = scene 
        stage.show()
    }
}

fun main() {
    Application.launch(MainApp::class.java)
}
